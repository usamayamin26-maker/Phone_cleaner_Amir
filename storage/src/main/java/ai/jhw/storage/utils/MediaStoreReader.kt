package ai.jhw.storage.utils

import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import java.io.File

/** Reads MediaStore's Files table into [FileItem]s. */
internal class MediaStoreReader(private val resolver: ContentResolver) {

    private val filesUri: Uri = MediaStore.Files.getContentUri("external")

    private val projection: Array<String> = buildList {
        add(FileColumns._ID)
        // DATA is deprecated but still the file path, and the cleaners need the path.
        @Suppress("DEPRECATION")
        add(FileColumns.DATA)
        add(FileColumns.DISPLAY_NAME)
        add(FileColumns.SIZE)
        add(FileColumns.DATE_MODIFIED)
        add(FileColumns.MIME_TYPE)
        add(FileColumns.MEDIA_TYPE)
        add(FileColumns.WIDTH)
        add(FileColumns.HEIGHT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(FileColumns.DURATION)
    }.toTypedArray()

    fun query(kind: FileKind): List<FileItem> {
        val (selection, args) = selectionFor(kind)
        return read(selection, args, "${FileColumns.DATE_MODIFIED} DESC", Int.MAX_VALUE) { it.kind == kind }
    }

    fun largeFiles(minBytes: Long, limit: Int): List<FileItem> =
        read("${FileColumns.SIZE} >= ?", arrayOf(minBytes.toString()), "${FileColumns.SIZE} DESC", limit) {
            true
        }

    private fun selectionFor(kind: FileKind): Pair<String, Array<String>> = when (kind) {
        FileKind.IMAGE -> mediaType(FileColumns.MEDIA_TYPE_IMAGE)
        FileKind.VIDEO -> mediaType(FileColumns.MEDIA_TYPE_VIDEO)
        FileKind.AUDIO -> mediaType(FileColumns.MEDIA_TYPE_AUDIO)
        FileKind.APK -> "${FileColumns.DISPLAY_NAME} LIKE ?" to arrayOf("%.apk")
        // Documents, archives and the rest are not media: filtered by extension after the query.
        FileKind.DOCUMENT, FileKind.ARCHIVE, FileKind.OTHER ->
            "${FileColumns.MEDIA_TYPE} = ? AND ${FileColumns.SIZE} > 0" to
                arrayOf(FileColumns.MEDIA_TYPE_NONE.toString())
    }

    private fun mediaType(type: Int) = "${FileColumns.MEDIA_TYPE} = ?" to arrayOf(type.toString())

    private fun read(
        selection: String,
        args: Array<String>,
        sortOrder: String,
        limit: Int,
        accept: (FileItem) -> Boolean,
    ): List<FileItem> {
        val items = ArrayList<FileItem>()
        try {
            resolver.query(filesUri, projection, selection, args, sortOrder)?.use { cursor ->
                val c = Columns(cursor)
                while (cursor.moveToNext() && items.size < limit) {
                    val item = c.toItem(cursor) ?: continue
                    if (accept(item)) items += item
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore query failed", e)
        }
        return items
    }

    private inner class Columns(cursor: Cursor) {
        val id = cursor.getColumnIndexOrThrow(FileColumns._ID)
        @Suppress("DEPRECATION")
        val data = cursor.getColumnIndexOrThrow(FileColumns.DATA)
        val name = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
        val size = cursor.getColumnIndexOrThrow(FileColumns.SIZE)
        val modified = cursor.getColumnIndexOrThrow(FileColumns.DATE_MODIFIED)
        val mime = cursor.getColumnIndexOrThrow(FileColumns.MIME_TYPE)
        val width = cursor.getColumnIndexOrThrow(FileColumns.WIDTH)
        val height = cursor.getColumnIndexOrThrow(FileColumns.HEIGHT)
        val duration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cursor.getColumnIndex(FileColumns.DURATION)
        } else {
            -1
        }

        fun toItem(cursor: Cursor): FileItem? {
            val path = cursor.getString(data) ?: return null
            val mediaId = cursor.getLong(id)
            val displayName = cursor.getString(name) ?: File(path).name
            val mimeType = cursor.getString(mime)
            return FileItem(
                path = path,
                name = displayName,
                uri = ContentUris.withAppendedId(filesUri, mediaId),
                sizeBytes = cursor.getLong(size),
                // MediaStore keeps seconds.
                dateModified = cursor.getLong(modified) * 1000,
                kind = FileKinds.of(displayName, mimeType),
                mimeType = mimeType,
                mediaId = mediaId,
                width = cursor.getInt(width).takeIf { it > 0 },
                height = cursor.getInt(height).takeIf { it > 0 },
                durationMillis = if (duration >= 0) cursor.getLong(duration).takeIf { it > 0 } else null,
            )
        }
    }

    private companion object {
        const val TAG = "MediaStoreReader"
    }
}
