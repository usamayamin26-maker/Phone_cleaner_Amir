package com.phone.cleaner.presentation.components

import androidx.annotation.StringRes
import com.phone.cleaner.R
import com.phone.cleaner.domain.entities.CleanerSectionType

/** "Images (%1$s)": the section card's title with its count. */
@get:StringRes
val CleanerSectionType.countTitle: Int
    get() = when (this) {
        CleanerSectionType.IMAGES -> R.string.cleaner_images_count
        CleanerSectionType.VIDEOS -> R.string.cleaner_videos_count
        CleanerSectionType.LARGE_FILES -> R.string.cleaner_large_files_count
        CleanerSectionType.DOCUMENTS -> R.string.cleaner_files_count
        CleanerSectionType.VOICE_NOTES -> R.string.cleaner_voice_notes_count
        CleanerSectionType.AUDIO -> R.string.cleaner_audio_count
        CleanerSectionType.GIFS -> R.string.cleaner_gifs_count
        CleanerSectionType.STICKERS -> R.string.cleaner_stickers_count
        CleanerSectionType.STATUSES -> R.string.cleaner_statuses_count
        CleanerSectionType.DOWNLOADS -> R.string.cleaner_downloads_count
        CleanerSectionType.GALLERY -> R.string.cleaner_gallery_count
        CleanerSectionType.WA_PHOTOS -> R.string.cleaner_wa_photos_count
        CleanerSectionType.WA_VIDEOS -> R.string.cleaner_wa_videos_count
    }

/** "Images": the title of the section's own page. */
@get:StringRes
val CleanerSectionType.pageTitle: Int
    get() = when (this) {
        CleanerSectionType.IMAGES -> R.string.cleaner_title_images
        CleanerSectionType.VIDEOS -> R.string.cleaner_title_videos
        CleanerSectionType.LARGE_FILES -> R.string.cleaner_title_large_files
        CleanerSectionType.DOCUMENTS -> R.string.cleaner_title_files
        CleanerSectionType.VOICE_NOTES -> R.string.cleaner_title_voice_notes
        CleanerSectionType.AUDIO -> R.string.cleaner_title_audio
        CleanerSectionType.GIFS -> R.string.cleaner_title_gifs
        CleanerSectionType.STICKERS -> R.string.cleaner_title_stickers
        CleanerSectionType.STATUSES -> R.string.cleaner_title_statuses
        CleanerSectionType.DOWNLOADS -> R.string.cleaner_title_downloads
        CleanerSectionType.GALLERY -> R.string.cleaner_title_gallery
        CleanerSectionType.WA_PHOTOS -> R.string.cleaner_title_wa_photos
        CleanerSectionType.WA_VIDEOS -> R.string.cleaner_title_wa_videos
    }
