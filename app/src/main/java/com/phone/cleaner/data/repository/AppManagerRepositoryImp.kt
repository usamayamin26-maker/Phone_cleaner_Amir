package com.phone.cleaner.data.repository

import ai.jhw.appmanager.model.InstalledApp
import ai.jhw.appusage.domain.repository.AppUsageRepository
import android.content.Intent
import android.util.Log
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.repository.AppManagerRepository
import kotlinx.coroutines.CancellationException

class AppManagerRepositoryImp(
    private val appManager: ai.jhw.appmanager.domain.repository.AppManagerRepository,
    private val appUsage: AppUsageRepository,
) : AppManagerRepository {

    override suspend fun getInstalledApps(): ApiResult<List<InstalledApp>> = try {
        ApiResult.Success(appManager.getInstalledApps().apps)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Listing installed apps failed", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }

    override fun isInstalled(packageName: String): Boolean = appManager.isInstalled(packageName)

    override fun uninstallIntent(packageName: String): Intent = appManager.uninstallIntent(packageName)

    override fun appDetailsIntent(packageName: String): Intent = appManager.appDetailsIntent(packageName)

    override fun hasUsageAccess(): Boolean = appUsage.hasUsageAccess()

    override fun usageAccessSettingsIntent(): Intent = appUsage.usageAccessSettingsIntent()

    private companion object {
        const val TAG = "AppManagerRepositoryImp"
    }
}
