package ai.jhw.storage.utils

import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.Locale

/** Helpers the cleaner modules use for files they find by walking the file system. */
object FileKinds {

    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp",
        "csv", "epub",
    )
    private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")

    fun mimeTypeOf(name: String): String? {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun of(name: String, mimeType: String?): FileKind {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = mimeType ?: mimeTypeOf(name)
        return when {
            extension == "apk" || mime == "application/vnd.android.package-archive" -> FileKind.APK
            mime?.startsWith("image/") == true -> FileKind.IMAGE
            mime?.startsWith("video/") == true -> FileKind.VIDEO
            mime?.startsWith("audio/") == true -> FileKind.AUDIO
            extension in DOCUMENT_EXTENSIONS -> FileKind.DOCUMENT
            extension in ARCHIVE_EXTENSIONS -> FileKind.ARCHIVE
            else -> FileKind.OTHER
        }
    }

    /** A [FileItem] for a file found on disk rather than through MediaStore. */
    fun fromFile(file: File, kind: FileKind? = null): FileItem {
        val mime = mimeTypeOf(file.name)
        return FileItem(
            path = file.absolutePath,
            name = file.name,
            uri = Uri.fromFile(file),
            sizeBytes = file.length(),
            dateModified = file.lastModified(),
            kind = kind ?: of(file.name, mime),
            mimeType = mime,
        )
    }
}
