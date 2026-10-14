package ai.jhw.appmanager.model

/** One app on the App Cleaner list. Sizes in bytes; the app formats them. */
data class InstalledApp(
    val packageName: String,
    /** The app's own name as the device shows it - device data, not app text. */
    val label: String,
    val versionName: String?,
    val installedAt: Long,
    val updatedAt: Long,
    val isSystemApp: Boolean,
    /** The installed code (APK (Android Package) plus splits). */
    val appBytes: Long,
    /** User data; null when [sizeSource] is [SizeSource.APK_ONLY]. */
    val dataBytes: Long?,
    /** Cache; null when [sizeSource] is [SizeSource.APK_ONLY]. */
    val cacheBytes: Long?,
    val sizeSource: SizeSource,
) {
    val totalBytes: Long get() = appBytes + (dataBytes ?: 0L) + (cacheBytes ?: 0L)
}

enum class SizeSource {
    /** From StorageStatsManager (API 26+ with Usage access): code, data and cache. */
    STORAGE_STATS,

    /** Only the APK files on disk - what is known without Usage access. */
    APK_ONLY,
}

data class InstalledAppsResult(
    /** Largest first. */
    val apps: List<InstalledApp>,
) {
    val totalBytes: Long get() = apps.sumOf { it.totalBytes }
}
