package ai.jhw.storage.repo

import ai.jhw.storage.domain.repository.StorageRepository
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import ai.jhw.storage.model.StorageAccess
import ai.jhw.storage.utils.AccessChecker
import ai.jhw.storage.utils.MediaStoreReader
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StorageRepositoryImpl(private val context: Context) : StorageRepository {

    private val reader by lazy { MediaStoreReader(context.contentResolver) }

    override fun access(): StorageAccess = AccessChecker.access(context)

    override fun mediaPermissions(kinds: Set<FileKind>): Array<String> = AccessChecker.mediaPermissions(kinds)

    override fun allFilesAccessIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.fromParts("package", context.packageName, null),
            )
        } else {
            null
        }

    // Deprecated for apps without file access; this app reads shared storage by path on purpose.
    @Suppress("DEPRECATION")
    override fun sharedStorageRoot(): File = Environment.getExternalStorageDirectory()

    override suspend fun queryMedia(kind: FileKind): List<FileItem> =
        withContext(Dispatchers.IO) { reader.query(kind) }

    override suspend fun getLargeFiles(minBytes: Long, limit: Int): List<FileItem> =
        withContext(Dispatchers.IO) { reader.largeFiles(minBytes, limit) }

    override suspend fun delete(items: List<FileItem>): DeleteOutcome = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext DeleteOutcome.Completed(DeleteResult(emptyList(), emptyList(), 0))

        if (access() == StorageAccess.FULL) {
            return@withContext DeleteOutcome.Completed(deleteDirectly(items))
        }

        // Without full access only MediaStore can delete another app's media, and from Android 11
        // it asks the user first. The request takes Images / Video / Audio / Downloads item URIs
        // only - a Files-table URI is refused ("All requested items must be Media items") - so
        // each item is addressed through its own collection, and one no collection covers is
        // left out of the request (it stays, and confirmDeleted reports nothing for it).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val requests = items.mapNotNull { item -> consentUri(item)?.let { item to it } }
            if (requests.isNotEmpty()) {
                val skipped = items.size - requests.size
                if (skipped > 0) Log.w(TAG, "$skipped items are not media and cannot be deleted without full access")
                val request = MediaStore.createDeleteRequest(context.contentResolver, requests.map { it.second })
                return@withContext DeleteOutcome.NeedsUserConsent(request.intentSender, requests.map { it.first })
            }
        }

        Log.w(TAG, "No access to delete ${items.size} items")
        DeleteOutcome.Completed(DeleteResult(emptyList(), items.map { it.path }, 0))
    }

    override suspend fun confirmDeleted(items: List<FileItem>): DeleteResult =
        withContext(Dispatchers.IO) {
            val (gone, left) = items.partition { !File(it.path).exists() }
            DeleteResult(
                deletedPaths = gone.map { it.path },
                failedPaths = left.map { it.path },
                freedBytes = gone.sumOf { it.sizeBytes },
            )
        }

    override suspend fun deletePaths(paths: List<String>): DeleteResult = withContext(Dispatchers.IO) {
        if (access() != StorageAccess.FULL) {
            Log.w(TAG, "deletePaths without full access: ${paths.size} paths refused")
            return@withContext DeleteResult(emptyList(), paths, 0)
        }
        val deleted = ArrayList<String>()
        val failed = ArrayList<String>()
        var freed = 0L
        for (path in paths) {
            val file = File(path)
            val size = if (file.isFile) file.length() else 0L
            if (deleteSafely(file)) {
                deleted += path
                freed += size
            } else {
                failed += path
            }
        }
        rescan(deleted)
        DeleteResult(deleted, failed, freed)
    }

    private fun deleteDirectly(items: List<FileItem>): DeleteResult {
        val deleted = ArrayList<String>()
        val failed = ArrayList<String>()
        var freed = 0L
        for (item in items) {
            val file = File(item.path)
            if (deleteSafely(file)) {
                deleted += item.path
                freed += item.sizeBytes
                // Drop the MediaStore row too, so the gallery stops showing a broken thumbnail.
                if (item.mediaId != null) {
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                    } catch (e: Exception) {
                        Log.d(TAG, "MediaStore row already gone for ${item.path}")
                    }
                }
            } else {
                failed += item.path
            }
        }
        rescan(deleted)
        return DeleteResult(deleted, failed, freed)
    }

    /** The item's URI in the media collection the consent request accepts, if it has one. */
    private fun consentUri(item: FileItem): Uri? {
        val id = item.mediaId ?: return null
        val collection = when (item.kind) {
            FileKind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            FileKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            FileKind.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && "/Download/" in item.path) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                return null
            }
        }
        return ContentUris.withAppendedId(collection, id)
    }

    /**
     * A file is deleted; a directory only when it is empty right now. Never recursive: a folder
     * that gained a file since the scan keeps it.
     */
    private fun deleteSafely(file: File): Boolean {
        if (!file.exists()) return true
        if (file.isDirectory && !file.list().isNullOrEmpty()) {
            Log.w(TAG, "Refusing to delete non-empty directory ${file.path}")
            return false
        }
        return try {
            file.delete()
        } catch (e: SecurityException) {
            Log.w(TAG, "Delete refused for ${file.path}", e)
            false
        }
    }

    /** Tells MediaStore the files are gone, so no other app keeps listing them. */
    private fun rescan(paths: List<String>) {
        if (paths.isEmpty()) return
        MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
    }

    private companion object {
        const val TAG = "StorageRepositoryImpl"
    }
}
