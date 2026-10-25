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

    /**
     * Folders under the standard ones that apps write to and expect to find: the camera's own
     * folder is often empty on a new phone, and deleting it only makes the camera create it again.
     */
    private val STANDARD_SUBFOLDERS = setOf(
        "DCIM/Camera", "DCIM/Screenshots", "Pictures/Screenshots", "Movies/Screen recordings",
    )

    /**
     * Files MediaStore keeps in every .thumbnails folder: they are markers, not thumbnails, and
     * deleting the .nomedia one would make the folder's images appear in the gallery.
     */
    private val THUMBNAIL_MARKERS = setOf(".nomedia", ".database_uuid")

    const val THUMBNAILS_DIR = ".thumbnails"

    fun isTempFile(file: File): Boolean =
        file.extension.lowercase(Locale.ROOT) in TEMP_EXTENSIONS

    fun isApk(file: File): Boolean = file.extension.equals("apk", ignoreCase = true)

    fun isThumbnailCache(dir: File): Boolean = dir.name == THUMBNAILS_DIR

    /** A thumbnail inside a .thumbnails folder, as opposed to one of MediaStore's markers. */
    fun isThumbnail(file: File): Boolean = file.isFile && file.name !in THUMBNAIL_MARKERS

    fun isProtectedFolder(dir: File, root: File): Boolean =
        dir == root ||
            (dir.parentFile == root && dir.name in STANDARD_FOLDERS) ||
            dir.relativeTo(root).invariantSeparatorsPath in STANDARD_SUBFOLDERS

    /** The Android/ folder, where apps keep their own files. */
    fun isAppsFolder(dir: File, root: File): Boolean = dir == File(root, "Android")
}
