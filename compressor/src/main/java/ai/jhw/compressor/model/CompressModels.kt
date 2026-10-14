package ai.jhw.compressor.model

import ai.jhw.storage.model.FileItem

enum class ImageFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    WEBP("webp", "image/webp"),

    /** Lossless: [CompressOptions.quality] and [CompressOptions.maxBytes] do not apply. */
    PNG("png", "image/png"),
}

/**
 * How to compress. [maxWidth] / [maxHeight] shrink the image to fit (never enlarge); [maxBytes]
 * lowers the quality step by step until the file fits, down to [MIN_QUALITY].
 */
data class CompressOptions(
    val quality: Int = 80,
    val maxWidth: Int? = null,
    val maxHeight: Int? = null,
    val format: ImageFormat = ImageFormat.JPEG,
    val maxBytes: Long? = null,
) {
    init {
        require(quality in 1..100) { "quality must be 1..100" }
    }

    companion object {
        const val MIN_QUALITY = 10
    }
}

/** A compressed image waiting in the app's cache until the user saves or discards it. */
data class CompressedImage(
    val source: FileItem,
    val outputPath: String,
    val format: ImageFormat,
    val originalBytes: Long,
    val compressedBytes: Long,
    val width: Int,
    val height: Int,
    val qualityUsed: Int,
) {
    val savedBytes: Long get() = (originalBytes - compressedBytes).coerceAtLeast(0)
}
