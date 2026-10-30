package com.phone.cleaner.presentation.components

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
    /** The results card's artwork: the card's robot, exported from Figma at the card's size. */
    @param:DrawableRes val robotCard: Int,
    val chips: Chips,
    /** Shown on the results when nothing was found. */
    @param:StringRes val emptyMessage: Int,
    /**
     * This cleaner's prefix in the ads Remote Config keys and in the ad caches: `ai_cleaner`,
     * `wa_cleaner` or `duplicates`. Each of the four pages adds its own suffix - `_scan`,
     * `_results`, `_groups`, `_detail` - so a page's ad is requested once and re-attached from the
     * cache when the user comes back to it, and a late ad for a page already left is only cached.
     */
    val adsKey: String,
) {

    /** `admob_<adsKey>_banner_id` / `_native_id`: all four pages share this cleaner's unit group. */
    val adsUnits: String get() = adsKey

    /** Back out of the cleaner itself, from the results page. */
    val adsBackFlag: String get() = "is_show_${adsKey}_back_inter"

    /** Opening one of the cleaner's inner pages (a section, a folder group). */
    val adsPageEntryFlag: String get() = "is_show_${adsKey}_detail_btn_inter"

    /** Back out of one of those inner pages, to the page it was opened from. */
    val adsPageBackFlag: String get() = "is_show_${adsKey}_detail_back_inter"

    enum class Chips {
        /** Images, videos and files, each with its icon and size (Figma 1:555). */
        SIZES,

        /** The total found and "Scan completed", text only (Figma 1:1065). */
        TOTAL_AND_STATUS,
    }
}
