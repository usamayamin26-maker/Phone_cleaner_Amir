package ai.jhw.compressor.repo

import ai.jhw.compressor.domain.repository.CompressorRepository
import ai.jhw.compressor.model.CompressOptions
import ai.jhw.compressor.model.CompressedImage
import ai.jhw.compressor.model.ImageFormat
import ai.jhw.compressor.utils.BitmapCodec
import ai.jhw.compressor.utils.ExifCopier
import ai.jhw.storage.domain.repository.StorageRepository
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import ai.jhw.storage.model.StorageAccess
import ai.jhw.storage.utils.FileKinds
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class CompressorRepositoryImpl(
    private val context: Context,
    private val storage: StorageRepository,
) : CompressorRepository {

    private val resolver get() = context.contentResolver
    private val cacheDir: File get() = File(context.cacheDir, "compressor").apply { mkdirs() }

    override suspend fun compress(item: FileItem, options: CompressOptions): CompressedImage =
        withContext(Dispatchers.Default) {
            val bitmap = BitmapCodec.decode(resolver, item.uri, options.maxWidth, options.maxHeight)
                ?: throw IOException("Cannot decode ${item.path}")

            var quality = options.quality
            var bytes = BitmapCodec.encode(bitmap, options.format, quality)
            val maxBytes = options.maxBytes
            if (maxBytes != null && options.format != ImageFormat.PNG) {
                while (bytes.size > maxBytes && quality > CompressOptions.MIN_QUALITY) {
                    quality = (quality - QUALITY_STEP).coerceAtLeast(CompressOptions.MIN_QUALITY)
                    bytes = BitmapCodec.encode(bitmap, options.format, quality)
                }
            }

            val output = File(cacheDir, "${item.name.substringBeforeLast('.')}.${options.format.extension}")
            withContext(Dispatchers.IO) {
                output.writeBytes(bytes)
                ExifCopier.copy(resolver, item.uri, output)
            }
            CompressedImage(
                source = item,
                outputPath = output.path,
                format = options.format,
                originalBytes = item.sizeBytes,
                compressedBytes = output.length(),
                width = bitmap.width,
                height = bitmap.height,
                qualityUsed = quality,
            ).also { bitmap.recycle() }
        }

    override suspend fun saveCopy(image: CompressedImage, albumName: String): FileItem? =
        withContext(Dispatchers.IO) {
            val output = File(image.outputPath)
            val name = "${image.source.name.substringBeforeLast('.')}_${System.currentTimeMillis()}." +
                image.format.extension
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveThroughMediaStore(output, name, image.format, albumName)
                } else {
                    saveAsFile(output, name, albumName)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Saving the compressed copy failed", e)
                null
            }
        }

    override suspend fun replaceOriginal(image: CompressedImage): Boolean = withContext(Dispatchers.IO) {
        val original = File(image.source.path)
        if (storage.access() != StorageAccess.FULL) {
            Log.w(TAG, "Replacing an original needs All files access")
            return@withContext false
        }
        if (!original.extension.equals(image.format.extension, ignoreCase = true) &&
            !(image.format == ImageFormat.JPEG && original.extension.equals("jpeg", ignoreCase = true))
        ) {
            Log.w(TAG, "Format changed (${original.extension} -> ${image.format.extension}); not replacing")
            return@withContext false
        }
        try {
            // Write next to the original first, then swap, so a failure never leaves half a photo.
            val temp = File(original.parentFile, ".${original.name}.compressing")
            File(image.outputPath).copyTo(temp, overwrite = true)
            if (!temp.renameTo(original)) {
                temp.copyTo(original, overwrite = true)
                temp.delete()
            }
            File(image.outputPath).delete()
            MediaScannerConnection.scanFile(context, arrayOf(original.path), null, null)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Replacing ${original.path} failed", e)
            false
        }
    }

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) { cacheDir.listFiles()?.forEach { it.delete() } }
    }

    @RequiresApi(Build.VERSION_CODES.Q) // RELATIVE_PATH, IS_PENDING, VOLUME_EXTERNAL_PRIMARY
    private fun saveThroughMediaStore(
        output: File,
        name: String,
        format: ImageFormat,
        albumName: String,
    ): FileItem? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$albumName")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return null
        val size = output.length()
        resolver.openOutputStream(uri)?.use { out -> output.inputStream().use { it.copyTo(out) } }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        output.delete()

        // Where MediaStore put it; used for display only, the uri is what the app opens.
        @Suppress("DEPRECATION")
        val path = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "$albumName/$name",
        ).path
        return FileItem(
            path = path,
            name = name,
            uri = uri,
            sizeBytes = size,
            dateModified = System.currentTimeMillis(),
            kind = FileKind.IMAGE,
            mimeType = format.mimeType,
        )
    }

    private fun saveAsFile(output: File, name: String, albumName: String): FileItem {
        @Suppress("DEPRECATION") // The only way to reach Pictures/ by path below Android 10.
        val album = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName)
        album.mkdirs()
        val target = File(album, name)
        output.copyTo(target, overwrite = false)
        output.delete()
        MediaScannerConnection.scanFile(context, arrayOf(target.path), null, null)
        return FileKinds.fromFile(target, FileKind.IMAGE)
    }

    private companion object {
        const val TAG = "CompressorRepositoryImpl"
        const val QUALITY_STEP = 10
    }
}
