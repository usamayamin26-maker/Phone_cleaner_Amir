package ai.jhw.deviceinfo.model

/** The "Display Information" screen, and the resolution and size on "Device Information". */
data class DisplayDetails(
    /** Physical panel resolution in pixels, independent of orientation and the app window. */
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    /** "xxhdpi" and friends - Android's density bucket for this screen. */
    val densityBucket: String,
    /** The diagonal in inches, computed from the panel's own dots-per-inch, e.g. 6.12. */
    val diagonalInches: Double?,
    val refreshRateHz: Float,
    val supportedRefreshRatesHz: List<Float>,
    val isHdrSupported: Boolean,
    /** 0.0 to 1.0, or null when automatic brightness is on or the value is unreadable. */
    val brightnessFraction: Float?,
    val screenTimeoutMillis: Long?,
)
