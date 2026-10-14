package ai.jhw.compressor.utils

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Carries the photo's own metadata over to the compressed copy - when it was taken, the camera,
 * the location - so a compressed photo still sorts and maps like the original. Orientation is
 * reset because the pixels are already upright.
 */
internal object ExifCopier {

    private const val TAG = "ExifCopier"

    private val TAGS = listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
    )

    fun copy(resolver: ContentResolver, from: Uri, to: File) {
        try {
            val source = resolver.openInputStream(from)?.use { ExifInterface(it) } ?: return
            val target = ExifInterface(to.absolutePath)
            TAGS.forEach { tag -> source.getAttribute(tag)?.let { target.setAttribute(tag, it) } }
            target.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            target.saveAttributes()
        } catch (e: Exception) {
            // PNG and some WebP files cannot carry EXIF; the image itself is fine without it.
            Log.d(TAG, "EXIF not copied to ${to.name}: ${e.javaClass.simpleName}")
        }
    }
}
