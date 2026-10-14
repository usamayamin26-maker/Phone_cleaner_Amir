package ai.jhw.compressor.utils

import ai.jhw.compressor.model.ImageFormat
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

internal object BitmapCodec {

    /** Decodes [uri] at no more than about [maxWidth] x [maxHeight], upright per its EXIF. */
    fun decode(resolver: ContentResolver, uri: Uri, maxWidth: Int?, maxHeight: Int?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val targetW = maxWidth ?: bounds.outWidth
        val targetH = maxHeight ?: bounds.outHeight
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetW && bounds.outHeight / (sample * 2) >= targetH) {
            sample *= 2
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val rotated = rotateUpright(decoded, orientation(resolver, uri))
        return fit(rotated, maxWidth, maxHeight)
    }

    fun encode(bitmap: Bitmap, format: ImageFormat, quality: Int): ByteArray =
        ByteArrayOutputStream().use { out ->
            bitmap.compress(compressFormat(format), quality, out)
            out.toByteArray()
        }

    private fun compressFormat(format: ImageFormat): Bitmap.CompressFormat = when (format) {
        ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
        ImageFormat.PNG -> Bitmap.CompressFormat.PNG
        ImageFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
    }

    private fun orientation(resolver: ContentResolver, uri: Uri): Int = try {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun rotateUpright(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Shrinks to fit inside the box, keeping the aspect ratio; never enlarges. */
    private fun fit(bitmap: Bitmap, maxWidth: Int?, maxHeight: Int?): Bitmap {
        val scaleW = maxWidth?.let { it.toFloat() / bitmap.width } ?: 1f
        val scaleH = maxHeight?.let { it.toFloat() / bitmap.height } ?: 1f
        val scale = minOf(scaleW, scaleH, 1f)
        if (scale >= 1f) return bitmap
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
