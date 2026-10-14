package ai.jhw.storage.model

import android.net.Uri

/**
 * One file the cleaners show and may delete. Sizes in bytes, times in epoch milliseconds.
 * [mediaId] is the MediaStore row id when the file came from MediaStore; files found by walking
 * the file system have none.
 */
data class FileItem(
    val path: String,
    val name: String,
    val uri: Uri,
    val sizeBytes: Long,
    val dateModified: Long,
    val kind: FileKind,
    val mimeType: String? = null,
    val mediaId: Long? = null,
    /** Pixels, for images and videos when MediaStore knows them. */
    val width: Int? = null,
    val height: Int? = null,
    /** Audio and video length in milliseconds when MediaStore knows it. */
    val durationMillis: Long? = null,
)

enum class FileKind { IMAGE, VIDEO, AUDIO, DOCUMENT, APK, ARCHIVE, OTHER }
