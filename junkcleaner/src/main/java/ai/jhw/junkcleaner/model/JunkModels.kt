package ai.jhw.junkcleaner.model

/** The Smart Scan categories. */
enum class JunkType {
    /** Installer files (.apk) left on shared storage. */
    APK_FILES,

    /** Temporary and log files: .tmp, .temp, .log, .xlog, .chck, .dmp, .hprof, .trace. */
    TEMP_FILES,

    /** Gallery thumbnail caches (.thumbnails folders) - the system rebuilds them. */
    THUMBNAILS,

    /** Folders with nothing in them, outside the standard top-level folders. */
    EMPTY_FOLDERS,

    /** Folders left behind by apps that are no longer installed. */
    RESIDUAL_FILES,

    /** Other apps' cache folders - Android 10 and older only. */
    APP_CACHE,
}

/** Why a category could not be scanned. */
enum class UnavailableReason {
    /** All files access is not granted. */
    NEEDS_FULL_ACCESS,

    /** Android 11+ does not let any app into other apps' private folders. */
    BLOCKED_BY_ANDROID,

    /**
     * Android 11+ hides most installed apps from this one, so "not installed" cannot be told
     * apart from "not visible". Needs QUERY_ALL_PACKAGES, which Play reviews.
     */
    NEEDS_PACKAGE_VISIBILITY,
}

/** One thing the Smart Scan found. Sizes in bytes, time in epoch milliseconds. */
data class JunkItem(
    val path: String,
    val name: String,
    val type: JunkType,
    val sizeBytes: Long,
    val lastModified: Long,
    /** A folder whose contents are deleted as one item (residual, thumbnails, app cache). */
    val isDirectory: Boolean,
    /** The app a residual folder, cache folder or APK belongs to. */
    val packageName: String? = null,
    /**
     * Safe to pre-select: a temp file, a thumbnail, an empty folder, a residual folder, or an APK
     * whose app is already installed at the same or a newer version. The user still confirms.
     */
    val isRecommended: Boolean,
)

data class JunkCategory(
    val type: JunkType,
    val items: List<JunkItem>,
    val unavailableReason: UnavailableReason? = null,
) {
    val isAvailable: Boolean get() = unavailableReason == null
    val totalBytes: Long get() = items.sumOf { it.sizeBytes }
}

data class JunkScanResult(val categories: List<JunkCategory>) {
    val totalBytes: Long get() = categories.sumOf { it.totalBytes }
}

/** What the scan reports while it runs - the "68%" screen - and once at the end. */
sealed interface JunkScanEvent {
    /** [fraction] is 0..1; [currentPath] is the folder being looked at. */
    data class Progress(val fraction: Float, val currentPath: String?) : JunkScanEvent
    data class Completed(val result: JunkScanResult) : JunkScanEvent
}
