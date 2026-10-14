package ai.jhw.deviceinfo.utils

import ai.jhw.deviceinfo.model.MemoryDetails
import ai.jhw.deviceinfo.model.RamDetails
import ai.jhw.deviceinfo.model.StorageDetails
import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.util.Log

internal object MemoryReader {

    private const val TAG = "MemoryReader"

    fun ram(context: Context): RamDetails {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
        return RamDetails(
            memory = MemoryDetails(totalBytes = info.totalMem, availableBytes = info.availMem),
            lowMemoryThresholdBytes = info.threshold,
            isLowMemory = info.lowMemory,
        )
    }

    fun storage(context: Context): StorageDetails {
        val dataFs = StatFs(Environment.getDataDirectory().path)
        val dataTotal = dataFs.totalBytes
        val dataFree = dataFs.availableBytes

        // From API 26 the platform knows the marketed size (128 GB) and what the system takes;
        // /data alone is smaller, which is why another app's "119 GB" never matches the box.
        var total = dataTotal
        var free = dataFree
        var system: Long? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val stats = context.getSystemService(StorageStatsManager::class.java)
                if (stats != null) {
                    total = stats.getTotalBytes(StorageManager.UUID_DEFAULT)
                    free = stats.getFreeBytes(StorageManager.UUID_DEFAULT)
                    system = (total - dataTotal).coerceAtLeast(0)
                }
            } catch (e: Exception) {
                Log.w(TAG, "StorageStatsManager unavailable, using /data", e)
            }
        }

        val removable = context.getSystemService(StorageManager::class.java)
            ?.storageVolumes
            ?.any { it.isRemovable } == true

        return StorageDetails(
            memory = MemoryDetails(totalBytes = total, availableBytes = free),
            systemBytes = system,
            hasRemovableStorage = removable,
        )
    }
}
