package ai.jhw.junkcleaner.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal class PackageChecker(private val context: Context) {

    /**
     * True when "not installed" can be trusted: below Android 11 every app is visible; from
     * Android 11 only with QUERY_ALL_PACKAGES, which this app does not declare by default.
     */
    val canSeeAllPackages: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.QUERY_ALL_PACKAGES) ==
            PackageManager.PERMISSION_GRANTED

    fun installedVersion(packageName: String): Long? = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        info.versionCodeCompat()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    /** Package name and version code of an APK file, or null when it cannot be parsed. */
    fun archiveInfo(path: String): Pair<String, Long>? = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(path, 0)
        }
        info?.let { it.packageName to it.versionCodeCompat() }
    } catch (_: Exception) {
        null
    }

    private fun PackageInfo.versionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
}
