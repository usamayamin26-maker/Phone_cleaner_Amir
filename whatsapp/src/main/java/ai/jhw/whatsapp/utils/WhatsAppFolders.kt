package ai.jhw.whatsapp.utils

import ai.jhw.whatsapp.model.WhatsAppMediaType
import ai.jhw.whatsapp.model.WhatsAppVariant
import java.io.File

internal object WhatsAppFolders {

    /**
     * Each variant's Media folder: under Android/media since WhatsApp moved there for Android 11,
     * and at the old top-level location for older installs. Both are returned when both exist.
     */
    fun mediaRoots(root: File): Map<WhatsAppVariant, List<File>> = mapOf(
        WhatsAppVariant.WHATSAPP to listOf(
            File(root, "Android/media/com.whatsapp/WhatsApp/Media"),
            File(root, "WhatsApp/Media"),
        ),
        WhatsAppVariant.WHATSAPP_BUSINESS to listOf(
            File(root, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media"),
            File(root, "WhatsApp Business/Media"),
        ),
    ).mapValues { (_, dirs) -> dirs.filter { it.isDirectory } }
        .filterValues { it.isNotEmpty() }

    /** "WhatsApp Business Voice Notes" -> VOICE_NOTES. */
    fun typeOf(folderName: String): WhatsAppMediaType {
        val name = folderName.removePrefix("WhatsApp Business ").removePrefix("WhatsApp ")
        return when (name) {
            "Images" -> WhatsAppMediaType.IMAGES
            "Video", "Video Notes" -> WhatsAppMediaType.VIDEOS
            "Audio" -> WhatsAppMediaType.AUDIO
            "Voice Notes" -> WhatsAppMediaType.VOICE_NOTES
            "Documents" -> WhatsAppMediaType.DOCUMENTS
            "Animated Gifs" -> WhatsAppMediaType.GIFS
            "Stickers" -> WhatsAppMediaType.STICKERS
            ".Statuses", "Statuses" -> WhatsAppMediaType.STATUSES
            "Profile Photos" -> WhatsAppMediaType.PROFILE_PHOTOS
            else -> WhatsAppMediaType.OTHER
        }
    }
}
