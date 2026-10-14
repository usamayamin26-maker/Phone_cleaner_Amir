package ai.jhw.whatsapp.model

import ai.jhw.storage.model.FileItem

enum class WhatsAppVariant { WHATSAPP, WHATSAPP_BUSINESS }

/** The folders WhatsApp keeps under Media/, "Sent" subfolders included. */
enum class WhatsAppMediaType {
    IMAGES, VIDEOS, AUDIO, VOICE_NOTES, DOCUMENTS, GIFS, STICKERS, STATUSES, PROFILE_PHOTOS, OTHER
}

data class WhatsAppCategory(
    val type: WhatsAppMediaType,
    /** Newest first; both WhatsApp and WhatsApp Business. */
    val files: List<FileItem>,
) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }
}

data class WhatsAppScanResult(
    /** Which of the two apps had a media folder on this phone. */
    val variantsFound: Set<WhatsAppVariant>,
    val categories: List<WhatsAppCategory>,
    /** True when the scan could not run because All files access is not granted. */
    val needsFullAccess: Boolean = false,
) {
    val totalBytes: Long get() = categories.sumOf { it.totalBytes }

    /** The design's "Large Files" row: every WhatsApp file of at least [minBytes], largest first. */
    fun largeFiles(minBytes: Long = DEFAULT_LARGE_FILE_BYTES): List<FileItem> =
        categories.flatMap { it.files }.filter { it.sizeBytes >= minBytes }.sortedByDescending { it.sizeBytes }

    companion object {
        const val DEFAULT_LARGE_FILE_BYTES = 10L * 1024 * 1024
    }
}

sealed interface WhatsAppScanEvent {
    data class Progress(val fraction: Float, val currentPath: String?) : WhatsAppScanEvent
    data class Completed(val result: WhatsAppScanResult) : WhatsAppScanEvent
}
