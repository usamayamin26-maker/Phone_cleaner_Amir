package com.phone.cleaner.presentation.components

import ai.jhw.storage.model.FileKind
import ai.jhw.storage.model.StorageAccess
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.phone.cleaner.R
import com.phone.cleaner.domain.repository.StorageAccessRepository
import com.phone.cleaner.utils.SharedPref

/**
 * Storage access for every cleaner: explains why it is needed with the design's "Permission is
 * required" dialog (Figma 1:2884), then asks through [StorageAccessRepository] - the runtime media
 * permissions, or on Android 11+ the All files access page when the feature needs [Level.ALL_FILES].
 *
 * - **Denied:** the dialog explains again; "Not Now" gives up and calls [onDeclined].
 * - **Denied forever:** Android shows no dialog any more, so the explanation offers the app's
 *   settings page instead. Coming back from any settings page re-checks access.
 *
 * Create it as an activity property, so its result launchers are registered before the activity
 * starts. The activity calls [request] itself, from onCreate behind its own latch - never from
 * onStart and never through a one-shot event, which would be dropped during start-up. A request in
 * flight survives a rotation through the saved state, so it is not asked twice.
 */
class StorageAccessGate(
    private val activity: ComponentActivity,
    private val repository: StorageAccessRepository,
    private val sharedPref: SharedPref,
    private val level: Level,
    /**
     * The media the feature reads, so Android asks only for those: Delete Duplicates and the AI
     * cleaner need photos and videos, never audio. Below Android 13 one permission covers all.
     */
    private val mediaKinds: Set<FileKind>,
    /** The feature asking, named in the dialog: "Delete Duplicates needs access to ...". */
    @param:StringRes private val featureName: Int,
    private val onGranted: () -> Unit,
    private val onDeclined: () -> Unit,
) {

    enum class Level {
        /** The media in [mediaKinds] through MediaStore; partial access on Android 14+ counts. */
        MEDIA,

        /** Every file in shared storage: documents, folders, junk. */
        ALL_FILES,
    }

    private var dialog: Dialog? = null
    private var isRequestInFlight = false
    private var isStateRestored = false

    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            isRequestInFlight = false
            onAnswer()
        }

    private val settingsLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isRequestInFlight = false
            onAnswer()
        }

    init {
        activity.savedStateRegistry.registerSavedStateProvider(STATE_KEY) {
            Bundle().apply { putBoolean(KEY_IN_FLIGHT, isRequestInFlight) }
        }
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dialog?.dismiss()
                dialog = null
            }
        })
    }

    fun hasAccess(): Boolean = when (level) {
        Level.MEDIA -> repository.access() != StorageAccess.NONE
        Level.ALL_FILES -> repository.access() == StorageAccess.FULL
    }

    /**
     * Calls [onGranted] at once when access is already there; otherwise explains and asks. Does
     * nothing while a request from before a rotation is still open - its answer arrives on its own.
     */
    fun request() {
        // The saved state can only be read once the activity's onCreate has restored it.
        if (!isStateRestored) {
            isStateRestored = true
            isRequestInFlight = activity.savedStateRegistry
                .consumeRestoredStateForKey(STATE_KEY)?.getBoolean(KEY_IN_FLIGHT) ?: false
        }
        if (isRequestInFlight) return
        if (hasAccess()) {
            onGranted()
            return
        }
        explain()
    }

    private fun onAnswer() {
        if (hasAccess()) {
            Log.d(TAG, "Storage access granted: ${repository.access()}")
            onGranted()
        } else {
            Log.d(TAG, "Storage access still missing (${repository.access()})")
            explain()
        }
    }

    private fun explain() {
        val deniedForever = isDeniedForever()
        val message = when {
            deniedForever -> activity.getString(R.string.permission_message_settings)
            level == Level.ALL_FILES && usesAllFilesPage() ->
                activity.getString(R.string.permission_message_all_files, activity.getString(featureName))
            else -> activity.getString(R.string.permission_message_media, activity.getString(featureName))
        }
        dialog?.dismiss()
        dialog = AppDialog.show(
            context = activity,
            spec = AppDialog.Spec(
                title = activity.getString(R.string.permission_title),
                message = message,
                primaryLabel = activity.getString(
                    if (deniedForever) R.string.permission_open_settings else R.string.permission_allow,
                ),
                secondaryLabel = activity.getString(R.string.permission_not_now),
                scrim = AppDialog.Scrim.STRONG,
            ),
            onPrimary = { if (deniedForever) openAppSettings() else ask() },
            onSecondary = onDeclined,
            onCancel = onDeclined,
        )
    }

    private fun ask() {
        isRequestInFlight = true
        val allFilesIntent = repository.allFilesAccessIntent()
        if (level == Level.ALL_FILES && allFilesIntent != null) {
            launchSettings(allFilesIntent)
        } else {
            sharedPref.hasAskedStorageAccess = true
            permissionLauncher.launch(repository.mediaPermissions(mediaKinds))
        }
    }

    private fun openAppSettings() {
        isRequestInFlight = true
        launchSettings(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)),
        )
    }

    private fun launchSettings(intent: Intent) {
        try {
            settingsLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No settings page for $intent", e)
            isRequestInFlight = false
            onDeclined()
        }
    }

    private fun usesAllFilesPage(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * After the user said "Don't ask again" (or denied twice), Android answers every request with
     * a silent denial. Asked before, no rationale owed and still not granted means exactly that.
     * The All files page is a settings screen and can always be opened again.
     */
    private fun isDeniedForever(): Boolean {
        if (level == Level.ALL_FILES && usesAllFilesPage()) return false
        if (!sharedPref.hasAskedStorageAccess) return false
        val permission = repository.mediaPermissions(mediaKinds).first()
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    private companion object {
        const val TAG = "StorageAccessGate"
        const val STATE_KEY = "storage_access_gate"
        const val KEY_IN_FLIGHT = "in_flight"
    }
}
