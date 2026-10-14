package ai.jhw.aicleaner.utils

import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import androidx.core.graphics.scale

/** Loads a file as the model's 224x224 input, decoding no more pixels than needed. */
internal class ThumbnailLoader(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver

    fun load(item: FileItem): Bitmap? = try {
        val bitmap = when (item.kind) {
            FileKind.VIDEO -> videoFrame(item)
            else -> image(item)
        }
        bitmap?.let { if (it.width == SIZE && it.height == SIZE) it else it.scale(SIZE, SIZE) }
    } catch (e: Exception) {
        Log.w(TAG, "Cannot decode ${item.path}: ${e.javaClass.simpleName}")
        null
    }

    private fun image(item: FileItem): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(item.uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= SIZE && bounds.outHeight / (sample * 2) >= SIZE) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openInputStream(item.uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun videoFrame(item: FileItem): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, item.uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(FRAME_AT_MICROS, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, SIZE, SIZE)
            } else {
                retriever.getFrameAtTime(FRAME_AT_MICROS, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val TAG = "ThumbnailLoader"
        const val SIZE = JunkClassifier.SIZE
        const val FRAME_AT_MICROS = 1_000_000L
    }
}
