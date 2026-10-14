package ai.jhw.appmanager.utils

import ai.jhw.appmanager.model.SizeSource
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import java.io.File

internal data class AppSize(
    val appBytes: Long,
    val dataBytes: Long?,
    val cacheBytes: Long?,
    val source: SizeSource,
)

internal object AppSizeReader {

    /**
     * StorageStatsManager for another app's sizes needs Usage access and throws a
     * SecurityException without it; the APK size on disk is the fallback.
     */
    fun read(context: Context, info: ApplicationInfo): AppSize {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val stats = context.getSystemService(StorageStatsManager::class.java)
                    ?.queryStatsForPackage(info.storageUuid, info.packageName, Process.myUserHandle())
                if (stats != null) {
                    return AppSize(
                        appBytes = stats.appBytes,
                        // dataBytes includes the cache; split them so the two never add up twice.
                        dataBytes = (stats.dataBytes - stats.cacheBytes).coerceAtLeast(0),
                        cacheBytes = stats.cacheBytes,
                        source = SizeSource.STORAGE_STATS,
                    )
                }
            } catch (_: SecurityException) {
                // No Usage access - fall through to the APK size.
            } catch (_: Exception) {
                // Package vanished mid-read or the volume is unmounted.
            }
        }
        return AppSize(apkBytes(info), null, null, SizeSource.APK_ONLY)
    }

    private fun apkBytes(info: ApplicationInfo): Long {
        val paths = listOfNotNull(info.publicSourceDir ?: info.sourceDir) +
            info.splitPublicSourceDirs.orEmpty()
        return paths.sumOf { File(it).length() }
    }
}
