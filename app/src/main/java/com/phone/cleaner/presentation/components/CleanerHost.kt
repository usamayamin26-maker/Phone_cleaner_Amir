package com.phone.cleaner.presentation.components

import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.phone.cleaner.domain.view_models.CleanerViewModel

/**
 * What the shared cleaner pages (scanning, results, a section's page) need from the screen that
 * hosts them. Implemented by [com.phone.cleaner.presentation.base.BaseCleanerActivity].
 */
interface CleanerHost {
    val cleaner: CleanerViewModel
    val screen: CleanerScreen

    /** "View All" or a section card: that section's page, over the results. */
    fun openSection(key: String)

    /** A section that needs All files access asked for it (the AI cleaner's Junk Files). */
    fun requestAllFilesAccess()
}

/** How one cleaner screen differs from the others in the design. */
data class CleanerScreen(
    @param:StringRes val title: Int,
    /** The scanning page's heading: "Loading Data" or "Scanning WhatsApp". */
    @param:StringRes val scanTitle: Int,
    /**
     * Figma 1:483 and 1:997 pad the scanning text by 12 px above and below, so it sits 24 px from
     * the ring and the button; the AI cleaner's 1:3 does not (12 px).
     */
    val scanTextPadded: Boolean,
    @param:DrawableRes val robot: Int,
    /**
     * The robot on the results card, scaled to this width and cut by the card. In Figma the robot
     * is part of the card's image fill, which is not exported, so its size and offsets are
     * measured from the PNG.
     */
    @param:DimenRes val robotWidth: Int,
    val chips: Chips,
    /** Shown on the results when nothing was found. */
    @param:StringRes val emptyMessage: Int,
    /** The robot's offset from the card's start edge. */
    @param:DimenRes val robotMarginStart: Int? = null,
    /** The robot's offset from the card's top edge; negative runs it above the card, cut. */
    @param:DimenRes val robotOffsetTop: Int? = null,
) {
    enum class Chips {
        /** Images, videos and files, each with its icon and size (Figma 1:555). */
        SIZES,

        /** The total found and "Scan completed", text only (Figma 1:1065). */
        TOTAL_AND_STATUS,
    }
}
