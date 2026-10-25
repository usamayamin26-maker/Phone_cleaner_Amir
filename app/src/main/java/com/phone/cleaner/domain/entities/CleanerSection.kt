package com.phone.cleaner.domain.entities

import ai.jhw.storage.model.FileItem

/**
 * What a section of a cleaner's results holds. It names the section and decides the page its
 * "View All" opens: photos and videos as a grid, documents and other files as a list.
 */
enum class CleanerSectionType(val isMedia: Boolean) {
    IMAGES(true),
    VIDEOS(true),
    LARGE_FILES(false),

    /** WhatsApp documents - the design's "Files" (Figma 1:1160). */
    DOCUMENTS(false),
    VOICE_NOTES(false),
    AUDIO(false),
    GIFS(true),
    STICKERS(true),
    STATUSES(true),

    /** The AI cleaner's folder groups inside Images and Videos (Figma 1:48). */
    DOWNLOADS(true),
    GALLERY(true),
    WA_PHOTOS(true),
    WA_VIDEOS(true),

    /** The Smart Scan's junk on the AI cleaner's results. */
    JUNK_FILES(false),
}

/**
 * One section of a cleaner's results (Figma 1:576), or one folder group inside a section's page.
 *
 * [key] is unique within the screen: the type for a section, "PARENT/TYPE" for a group.
 */
data class CleanerSection(
    val type: CleanerSectionType,
    val files: List<FileItem>,
    /** Folder groups the section's own page shows as cards (AI cleaner Images and Videos). */
    val groups: List<CleanerSection> = emptyList(),
    /** The section could not be scanned without All files access; its card asks for it. */
    val needsAccess: Boolean = false,
    /** Only for duplicates: what deleting every extra copy frees, less than the files' total. */
    private val wastedBytes: Long? = null,
    val key: String = type.name,
) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }

    /** What the section's size chip shows. */
    val reclaimableBytes: Long get() = wastedBytes ?: totalBytes

    /** This section or one of its groups. */
    fun find(key: String): CleanerSection? =
        if (this.key == key) this else groups.firstNotNullOfOrNull { it.find(key) }

    companion object {
        fun groupKey(parent: CleanerSectionType, type: CleanerSectionType) = "${parent.name}/${type.name}"
    }
}
