package ai.jhw.deviceinfo.model

/**
 * Used against total for RAM (Random Access Memory) or ROM (Read-Only Memory - the internal
 * storage). Bytes throughout; the app formats them.
 */
data class MemoryDetails(
    val totalBytes: Long,
    val availableBytes: Long,
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0)

    /** 0..100, e.g. 56.31 for the Home card's "56.31% used". */
    val usedPercent: Float
        get() = if (totalBytes <= 0) 0f else usedBytes * 100f / totalBytes
}

/** The "RAM Information" screen. */
data class RamDetails(
    val memory: MemoryDetails,
    /** Below this many free bytes the system starts killing background apps. */
    val lowMemoryThresholdBytes: Long,
    val isLowMemory: Boolean,
)

/** The "ROM Information" screen and the Home "Storage Usage" card. */
data class StorageDetails(
    /** The marketed size where the platform knows it (128 GB, not 119.2 GB), from API 26. */
    val memory: MemoryDetails,
    /** What the system itself and the partitions outside /data take, from API 26. */
    val systemBytes: Long?,
    val hasRemovableStorage: Boolean,
)
