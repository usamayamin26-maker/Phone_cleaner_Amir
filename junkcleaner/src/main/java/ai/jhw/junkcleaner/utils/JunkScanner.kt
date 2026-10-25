package ai.jhw.junkcleaner.utils

import ai.jhw.junkcleaner.model.JunkCategory
import ai.jhw.junkcleaner.model.JunkItem
import ai.jhw.junkcleaner.model.JunkScanResult
import ai.jhw.junkcleaner.model.JunkType
import ai.jhw.junkcleaner.model.UnavailableReason
import android.os.Build
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

/**
 * One walk over shared storage that sorts what it finds into the junk categories. The Android/
 * folder is not walked: other apps' files live there. Residual folders (Android/media of removed
 * apps) and, below Android 11, Android/data cache folders are checked separately.
 */
internal class JunkScanner(
    private val root: File,
    private val packages: PackageChecker,
    private val onProgress: suspend (fraction: Float, path: String) -> Unit,
) {
    private val apks = ArrayList<JunkItem>()
    private val temps = ArrayList<JunkItem>()
    private val thumbnails = ArrayList<JunkItem>()
    private val emptyFolders = ArrayList<JunkItem>()

    suspend fun scan(): JunkScanResult {
        val topLevel = root.listFiles()?.sortedBy { it.name.lowercase() }.orEmpty()
        topLevel.forEachIndexed { index, child ->
            currentCoroutineContext().ensureActive()
            onProgress(index.toFloat() / topLevel.size.coerceAtLeast(1), child.path)
            if (child.isDirectory) visitDirectory(child, depth = 1) else visitFile(child)
        }
        val residual = residualCategory()
        val cache = appCacheCategory()
        onProgress(1f, root.path)

        return JunkScanResult(
            listOf(
                JunkCategory(JunkType.APK_FILES, apks.sortedByDescending { it.sizeBytes }),
                JunkCategory(JunkType.TEMP_FILES, temps.sortedByDescending { it.sizeBytes }),
                JunkCategory(JunkType.THUMBNAILS, thumbnails),
                // Deepest first, so a parent is only reached after its empty children are gone.
                JunkCategory(JunkType.EMPTY_FOLDERS, emptyFolders.sortedByDescending { it.path.length }),
                residual,
                cache,
            )
        )
    }

    /** Returns true when the directory holds no file at any depth. */
    private suspend fun visitDirectory(dir: File, depth: Int): Boolean {
        currentCoroutineContext().ensureActive()
        // Android/ belongs to apps; only the residual and cache checks below look inside it.
        if (JunkRules.isAppsFolder(dir, root)) return false
        if (JunkRules.isThumbnailCache(dir)) {
            folderItem(dir, JunkType.THUMBNAILS, packageName = null)?.let { thumbnails += it }
            return false
        }
        if (depth > MAX_DEPTH) return false

        val children = dir.listFiles() ?: return false
        var isEmpty = true
        for (child in children) {
            if (child.isDirectory) {
                if (!visitDirectory(child, depth + 1)) isEmpty = false
            } else {
                visitFile(child)
                isEmpty = false
            }
        }
        if (isEmpty && !JunkRules.isProtectedFolder(dir, root)) {
            emptyFolders += JunkItem(
                path = dir.path,
                name = dir.name,
                type = JunkType.EMPTY_FOLDERS,
                sizeBytes = 0,
                lastModified = dir.lastModified(),
                isDirectory = false,
                isRecommended = true,
            )
        }
        return isEmpty
    }

    private fun visitFile(file: File) {
        when {
            JunkRules.isTempFile(file) -> temps += fileItem(file, JunkType.TEMP_FILES, null, true)
            JunkRules.isApk(file) -> {
                val archive = packages.archiveInfo(file.path)
                val installed = archive?.let { packages.installedVersion(it.first) }
                val superseded = archive != null && installed != null && installed >= archive.second
                apks += fileItem(file, JunkType.APK_FILES, archive?.first, superseded)
            }
        }
    }

    /** Android/media/<package> folders (and Android/data below Android 11) of removed apps. */
    private fun residualCategory(): JunkCategory {
        if (!packages.canSeeAllPackages) {
            return JunkCategory(JunkType.RESIDUAL_FILES, emptyList(), UnavailableReason.NEEDS_PACKAGE_VISIBILITY)
        }
        val parents = buildList {
            add(File(root, "Android/media"))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) add(File(root, "Android/data"))
        }
        val items = parents
            .flatMap { it.listFiles()?.toList().orEmpty() }
            .filter { it.isDirectory && looksLikePackage(it.name) }
            .filter { packages.installedVersion(it.name) == null }
            .mapNotNull { folderItem(it, JunkType.RESIDUAL_FILES, it.name) }
        return JunkCategory(JunkType.RESIDUAL_FILES, items.sortedByDescending { it.sizeBytes })
    }

    private fun appCacheCategory(): JunkCategory {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return JunkCategory(JunkType.APP_CACHE, emptyList(), UnavailableReason.BLOCKED_BY_ANDROID)
        }
        val items = File(root, "Android/data").listFiles().orEmpty()
            .filter { it.isDirectory && packages.installedVersion(it.name) != null }
            .mapNotNull { folderItem(File(it, "cache"), JunkType.APP_CACHE, it.name) }
        return JunkCategory(JunkType.APP_CACHE, items.sortedByDescending { it.sizeBytes })
    }

    private fun fileItem(file: File, type: JunkType, packageName: String?, recommended: Boolean) =
        JunkItem(
            path = file.path,
            name = file.name,
            type = type,
            sizeBytes = file.length(),
            lastModified = file.lastModified(),
            isDirectory = false,
            packageName = packageName,
            isRecommended = recommended,
        )

    /**
     * A folder as one item, sized by its files; null when it does not exist or holds nothing. A
     * thumbnails folder counts only its thumbnails, not MediaStore's marker files.
     */
    private fun folderItem(dir: File, type: JunkType, packageName: String?): JunkItem? {
        if (!dir.isDirectory) return null
        val counted: (File) -> Boolean = if (type == JunkType.THUMBNAILS) JunkRules::isThumbnail else File::isFile
        val size = dir.walkTopDown().filter(counted).sumOf { it.length() }
        if (size <= 0L) return null
        return JunkItem(
            path = dir.path,
            name = dir.name,
            type = type,
            sizeBytes = size,
            lastModified = dir.lastModified(),
            isDirectory = true,
            packageName = packageName,
            isRecommended = true,
        )
    }

    private fun looksLikePackage(name: String) = PACKAGE_NAME.matches(name)

    private companion object {
        const val MAX_DEPTH = 16
        val PACKAGE_NAME = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")
    }
}
