package com.phone.cleaner.domain.entities

/**
 * The three sections of Delete Duplicates (Figma 1:528). Images and Videos hold groups of
 * identical copies; Large Files holds single files and has no "Keep best one".
 */
enum class DuplicatesSection {
    IMAGES,
    VIDEOS,
    LARGE_FILES,
    ;

    val hasGroups: Boolean get() = this != LARGE_FILES
}
