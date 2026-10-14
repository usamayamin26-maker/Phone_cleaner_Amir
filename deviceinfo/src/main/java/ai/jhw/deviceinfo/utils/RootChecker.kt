package ai.jhw.deviceinfo.utils

import android.os.Build
import java.io.File

/**
 * A best-effort root check for the Device Information screen: a test-keys build or an `su` binary
 * in one of the usual places. It is informational only and is never used to block anything.
 */
internal object RootChecker {

    private val SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/su/bin/su",
    )

    fun isRooted(): Boolean =
        Build.TAGS?.contains("test-keys") == true || SU_PATHS.any { File(it).exists() }
}
