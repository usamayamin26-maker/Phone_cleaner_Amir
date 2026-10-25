package ai.jhw.appusage.utils

import android.app.AppOpsManager
import android.content.Context
import android.os.Process

internal object UsageAccess {

    /**
     * checkOpNoThrow(String, int, String) is the current call on every API level: from API 29 it
     * runs the same check as unsafeCheckOpNoThrow, which API 36.1 deprecated in its favour.
     */
    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
