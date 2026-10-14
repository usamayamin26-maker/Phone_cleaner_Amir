package ai.jhw.appmanager.domain.repository

import ai.jhw.appmanager.model.InstalledAppsResult
import android.content.Intent

/**
 * App Cleaner: the user's installed apps with their sizes, and the system uninstall flow.
 *
 * Sizes are exact (code + data + cache) only with Usage access; without it they are the APK size.
 * An app can never uninstall another app itself: [uninstallIntent] opens the system dialog, and
 * the screen calls [isInstalled] when it comes back to see what the user chose.
 */
interface AppManagerRepository {

    /** User-installed apps with a launcher icon. System apps are included only when asked. */
    suspend fun getInstalledApps(includeSystemApps: Boolean = false): InstalledAppsResult

    fun isInstalled(packageName: String): Boolean

    fun uninstallIntent(packageName: String): Intent

    /** The system's App info page, where the user can clear an app's cache or storage. */
    fun appDetailsIntent(packageName: String): Intent
}
