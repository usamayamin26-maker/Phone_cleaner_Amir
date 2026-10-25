package com.phone.cleaner.domain.repository

import ai.jhw.appmanager.model.InstalledApp
import android.content.Intent
import com.phone.cleaner.domain.entities.ApiResult

/**
 * App Cleaner: the user's apps with their sizes, the system uninstall dialog and App info page,
 * and the Usage access that makes the sizes exact. An app can never uninstall another app or clear
 * its cache itself - these return the system screens that do.
 */
interface AppManagerRepository {

    /** User-installed apps with a launcher icon, largest first. */
    suspend fun getInstalledApps(): ApiResult<List<InstalledApp>>

    fun isInstalled(packageName: String): Boolean

    fun uninstallIntent(packageName: String): Intent

    /** The system's App info page, where the user clears the app's cache. */
    fun appDetailsIntent(packageName: String): Intent

    /** Without Usage access only the APK (Android Package) size is known. */
    fun hasUsageAccess(): Boolean

    fun usageAccessSettingsIntent(): Intent
}
