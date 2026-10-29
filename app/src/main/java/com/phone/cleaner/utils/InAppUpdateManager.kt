package com.phone.cleaner.utils

import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import java.io.File

/**
 * Google Play in-app updates, the same file in every project (only the package differs).
 *
 * Which kind is read from the app's Remote Config ads JSON, key `in_app_update_type`:
 * - 0 off
 * - 1 flexible (default): downloads in the background while the app is used, then a
 *   "Restart" snackbar installs it
 * - 2 immediate: Play's full-screen update flow, which the user cannot use the app behind
 *
 * One instance per MainActivity, created as a property (it registers an activity-result launcher,
 * which has to happen before the activity starts); call [checkForUpdate] once from `onCreate`. It
 * follows the activity's lifecycle itself: on resume it brings back an immediate update that was
 * interrupted and re-shows the Restart prompt for a flexible one already downloaded.
 *
 * Only works for an install from Google Play; a sideloaded or debug build finds no update.
 *
 * **Testing without Play (debug builds only).** If the file `files/fake_in_app_update` exists in
 * the app's private storage, Google's [FakeAppUpdateManager] is used instead and an update is
 * simulated: it is "available", the user "accepts" it, it "downloads", and the real Restart snackbar
 * shows. Create it with
 * `adb shell run-as <applicationId> touch files/fake_in_app_update` and relaunch; delete it to go back
 * to Play. A release build is not debuggable, so it never reads the file.
 */
class InAppUpdateManager(
    private val activity: AppCompatActivity,
    /** The view the Restart snackbar sits above (the bottom bar); ignored while it is not shown. */
    private val snackbarAnchor: () -> View? = { null },
) : DefaultLifecycleObserver {

    // Both lazy: this object is built while the activity is being constructed, before it has a
    // Context, so nothing here may touch the activity as a Context until onCreate has run.

    /** Set only in a debug build with the fake-update file present; see the class docs. */
    private val fake: FakeAppUpdateManager? by lazy {
        if (isFakeUpdateRequested()) FakeAppUpdateManager(activity) else null
    }

    private val appUpdateManager: AppUpdateManager by lazy { fake ?: AppUpdateManagerFactory.create(activity) }

    private val updateLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            // Cancelled or failed: nothing to do, the next launch asks again.
            Log.d(TAG, "Update flow finished with result ${result.resultCode}")
        }

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) showRestartPrompt()
    }

    /** `in_app_update_type`, read once the activity exists (also after a recreate, which skips [checkForUpdate]). */
    private val type: Int by lazy { AdsController.config().int(KEY_TYPE, default = TYPE_FLEXIBLE) }

    private var listenerRegistered = false

    init {
        activity.lifecycle.addObserver(this)
    }

    /** Asks Play whether an update is available and starts the configured flow if one is. */
    fun checkForUpdate() {
        val updateType = playUpdateType(type) ?: return
        registerListenerOnce()
        fake?.let {
            Log.d(TAG, "Fake in-app update: simulating an available update")
            it.setUpdateAvailable(FAKE_AVAILABLE_VERSION_CODE, updateType)
        }

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(updateType)
                ) {
                    startUpdateFlow(info, updateType)
                }
            }
            .addOnFailureListener { Log.w(TAG, "Update check failed", it) }
    }

    override fun onResume(owner: LifecycleOwner) {
        if (type == TYPE_OFF) return
        registerListenerOnce()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> showRestartPrompt()
                type == TYPE_IMMEDIATE &&
                    info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    startUpdateFlow(info, AppUpdateType.IMMEDIATE)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (listenerRegistered) appUpdateManager.unregisterListener(installListener)
    }

    /** A flexible download reports "downloaded" through the listener, so it is attached once per activity. */
    private fun registerListenerOnce() {
        if (listenerRegistered || type != TYPE_FLEXIBLE) return
        appUpdateManager.registerListener(installListener)
        listenerRegistered = true
    }

    private fun startUpdateFlow(info: AppUpdateInfo, updateType: Int) {
        if (activity.isFinishing || activity.isDestroyed) return
        // Play's update screen is another app: coming back from it is not a return from background,
        // so it must not show the resume app open ad.
        AdsController.skipNextResume()
        appUpdateManager.startUpdateFlowForResult(info, updateLauncher, AppUpdateOptions.newBuilder(updateType).build())
        fake?.let { simulateUserAndDownload(it, updateType) }
    }

    /** Plays the user's and Play's part for [fake]: accept, then (flexible) download in the background. */
    private fun simulateUserAndDownload(fake: FakeAppUpdateManager, updateType: Int) {
        Log.d(TAG, "Fake in-app update: flow started (type=$updateType), user accepts")
        fake.userAcceptsUpdate()
        if (updateType != AppUpdateType.FLEXIBLE) return
        fake.downloadStarts()
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Fake in-app update: download complete")
            fake.downloadCompletes()
        }, FAKE_DOWNLOAD_MS)
    }

    private fun isFakeUpdateRequested(): Boolean {
        val debuggable = activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return debuggable && File(activity.filesDir, FAKE_UPDATE_FILE).exists()
    }

    private fun showRestartPrompt() {
        if (activity.isFinishing || activity.isDestroyed) return
        Snackbar.make(activity.findViewById<View>(android.R.id.content), R.string.in_app_update_downloaded, Snackbar.LENGTH_INDEFINITE)
            .setAnchorView(snackbarAnchor()?.takeIf { it.isShown })
            .setAction(R.string.in_app_update_restart) {
                Log.d(TAG, "Restart tapped, completing the update")
                appUpdateManager.completeUpdate()
            }
            .show()
    }

    private fun playUpdateType(type: Int): Int? = when (type) {
        TYPE_FLEXIBLE -> AppUpdateType.FLEXIBLE
        TYPE_IMMEDIATE -> AppUpdateType.IMMEDIATE
        else -> null
    }

    private companion object {
        const val TAG = "InAppUpdateManager"
        const val KEY_TYPE = "in_app_update_type"
        const val TYPE_OFF = 0
        const val TYPE_FLEXIBLE = 1
        const val TYPE_IMMEDIATE = 2
        const val FAKE_UPDATE_FILE = "fake_in_app_update"
        const val FAKE_AVAILABLE_VERSION_CODE = 999_999
        const val FAKE_DOWNLOAD_MS = 3_000L
    }
}
