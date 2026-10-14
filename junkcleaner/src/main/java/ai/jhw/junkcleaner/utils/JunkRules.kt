package ai.jhw.junkcleaner.utils

import java.io.File
import java.util.Locale

/**
 * What counts as junk. Deliberately narrower than the old source: .bak, .old and .nomedia files
 * are left alone (a .bak can be someone's only backup, and deleting a .nomedia file makes a hidden
 * folder's pictures appear in the gallery), and unknown MIME types are never junk by themselves.
 */
internal object JunkRules {

    private val TEMP_EXTENSIONS =
        setOf("tmp", "temp", "log", "xlog", "chck", "dmp", "hprof", "trace")

    /** Top-level folders every phone has; never reported as empty even when they are. */
    private val STANDARD_FOLDERS = setOf(
        "Alarms", "Android", "Audiobooks", "DCIM", "Documents", "Download", "Movies", "Music",
        "Notifications", "Pictures", "Podcasts", "Recordings", "Ringtones",
    )

    const val THUMBNAILS_DIR = ".thumbnails"

    fun isTempFile(file: File): Boolean =
        file.extension.lowercase(Locale.ROOT) in TEMP_EXTENSIONS

    fun isApk(file: File): Boolean = file.extension.equals("apk", ignoreCase = true)

    fun isThumbnailCache(dir: File): Boolean = dir.name == THUMBNAILS_DIR

    fun isProtectedFolder(dir: File, root: File): Boolean =
        dir == root || (dir.parentFile == root && dir.name in STANDARD_FOLDERS)

    /** The Android/ folder, where apps keep their own files. */
    fun isAppsFolder(dir: File, root: File): Boolean = dir == File(root, "Android")
}
