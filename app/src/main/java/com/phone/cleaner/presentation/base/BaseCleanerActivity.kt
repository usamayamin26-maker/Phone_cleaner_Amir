package com.phone.cleaner.presentation.base

import ai.jhw.storage.model.FileKind
import android.app.Activity
import android.app.Dialog
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ActivityCleanerBinding
import com.phone.cleaner.domain.entities.CleanerSection
import com.phone.cleaner.domain.repository.StorageAccessRepository
import com.phone.cleaner.domain.view_models.CleanerUiEvent
import com.phone.cleaner.domain.view_models.CleanerUiState
import com.phone.cleaner.presentation.components.AppDialog
import com.phone.cleaner.presentation.components.CleanerHost
import com.phone.cleaner.presentation.components.CleanerPageFragment
import com.phone.cleaner.presentation.components.CleanerResultsFragment
import com.phone.cleaner.presentation.components.CleanerScanFragment
import com.phone.cleaner.presentation.components.CleanerSectionsFragment
import com.phone.cleaner.presentation.components.StorageAccessGate
import com.phone.cleaner.utils.SharedPref
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.toReadableSize
import org.koin.android.ext.android.inject

/**
 * A cleaner screen (Delete Duplicates, AI Junk Cleaner, WA Cleaner). Hosts the shared pages -
 * scanning, results and a section's page - and owns what they share: storage access before the
 * scan, the "Are You Sure?" dialog (Figma 1:984) that every delete goes through, and the system
 * consent Android 11+ asks for without All files access.
 */
abstract class BaseCleanerActivity : BaseActivity<ActivityCleanerBinding>(), CleanerHost {

    override val bindingInflater: (LayoutInflater) -> ActivityCleanerBinding = ActivityCleanerBinding::inflate

    /** Each page draws its own header below the status bar. */
    override val applyDefaultInsets: Boolean = false

    /** What the scan needs: media through MediaStore, or every file. */
    protected abstract val accessLevel: StorageAccessGate.Level

    /** The media the scan reads, so Android asks for nothing else. */
    protected open val mediaKinds: Set<FileKind> = setOf(FileKind.IMAGE, FileKind.VIDEO)

    protected abstract val logTag: String

    /** A section to open as soon as the results are in (Scan Now opens Junk Files this way). */
    protected open val sectionToOpen: String? = null

    /** [sectionToOpen], until it has been opened once or turned out not to be there. */
    private var pendingSection: String? = null

    private val storageAccess: StorageAccessRepository by inject()
    private val sharedPref: SharedPref by inject()

    private val accessGate by lazy { gate(accessLevel, onGranted = { if (!cleaner.hasStarted) cleaner.startScan() }, onDeclined = ::finish) }

    /**
     * For a section that asks for All files access from the results; declining keeps the results.
     * A screen that needs All files access to scan at all already has it, so it has no second gate.
     */
    private val allFilesGate by lazy {
        if (accessLevel == StorageAccessGate.Level.ALL_FILES) {
            accessGate
        } else {
            gate(StorageAccessGate.Level.ALL_FILES, onGranted = ::onAllFilesAccessGranted, onDeclined = {})
        }
    }

    private val consentLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            cleaner.onConsentResult(result.resultCode == Activity.RESULT_OK)
        }

    private var dialog: Dialog? = null

    override fun setupViews() {
        // The gates register their result launchers, which has to happen before the activity starts.
        accessGate
        allFilesGate
        // A fresh ViewModel has no results - first launch, or the process was killed and the
        // pages restored - so the screen starts again from scanning, whatever page was showing.
        if (!cleaner.hasStarted) {
            pendingSection = sectionToOpen
            showScanning()
            // Driven from here, not from onStart: a one-shot event emitted during start-up is lost.
            accessGate.request()
        }
    }

    override fun observeState() {
        cleaner.state.collectState(::render)
        cleaner.events.collectEvents(::handle)
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        super.onDestroy()
    }

    override fun openSection(key: String) {
        val section = cleaner.state.value.section(key) ?: return
        supportFragmentManager.commit {
            replace(R.id.cleaner_container, pageFor(section))
            addToBackStack(key)
        }
    }

    override fun requestAllFilesAccess() = allFilesGate.request()

    /** All files access was granted from a section's card. */
    protected open fun onAllFilesAccessGranted() = Unit

    /** The page a section opens: its folder groups, a photo grid or a file list. */
    protected open fun pageFor(section: CleanerSection): Fragment = when {
        section.groups.isNotEmpty() -> CleanerSectionsFragment.newInstance(section.key)
        else -> CleanerPageFragment.newInstance(section.key, grid = section.type.isMedia)
    }

    private fun gate(level: StorageAccessGate.Level, onGranted: () -> Unit, onDeclined: () -> Unit) =
        StorageAccessGate(
            activity = this,
            repository = storageAccess,
            sharedPref = sharedPref,
            level = level,
            mediaKinds = mediaKinds,
            featureName = screen.title,
            onGranted = onGranted,
            onDeclined = onDeclined,
        )

    private fun showScanning() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit { replace(R.id.cleaner_container, CleanerScanFragment()) }
    }

    private fun render(state: CleanerUiState) {
        val current = supportFragmentManager.findFragmentById(R.id.cleaner_container)
        when {
            // A new scan (access granted from the results): back to the scanning page.
            state.isScanning && current != null && current !is CleanerScanFragment -> showScanning()
            // Results once the scan is done, unless they (or a section's page) already show. A
            // quick scan can finish before the scanning page's own commit has run, so an empty
            // container counts as scanning too; the two commits then run in order.
            state.isScanComplete && (current == null || current is CleanerScanFragment) -> {
                Log.d(logTag, "Showing results")
                supportFragmentManager.commit { replace(R.id.cleaner_container, CleanerResultsFragment()) }
                pendingSection?.let { key ->
                    pendingSection = null
                    if (state.section(key) != null) openSection(key)
                }
            }
        }
        if (state.error != null) {
            showToast(getString(R.string.error_generic))
            cleaner.clearError()
        }
    }

    private fun handle(event: CleanerUiEvent) {
        when (event) {
            is CleanerUiEvent.ConfirmDelete -> confirmDelete(event.count, event.bytes)
            is CleanerUiEvent.RequestConsent ->
                consentLauncher.launch(IntentSenderRequest.Builder(event.intentSender).build())
            is CleanerUiEvent.Deleted -> showToast(
                if (event.failedCount > 0) {
                    getString(R.string.cleaner_delete_partial, event.freedBytes.toReadableSize(this))
                } else {
                    getString(R.string.cleaner_freed, event.freedBytes.toReadableSize(this))
                },
            )
        }
    }

    /** The design's "Are You Sure?", naming how many files and how much space are about to go. */
    private fun confirmDelete(count: Int, bytes: Long) {
        val message = getString(R.string.dialog_delete_message) + "\n" +
            getString(R.string.dialog_delete_summary, formatNumber(count), bytes.toReadableSize(this))
        dialog?.dismiss()
        dialog = AppDialog.show(
            context = this,
            spec = AppDialog.Spec(
                title = getString(R.string.dialog_delete_title),
                message = message,
                primaryLabel = getString(R.string.action_delete),
                secondaryLabel = getString(R.string.action_cancel),
            ),
            onPrimary = cleaner::onDeleteConfirmed,
            onSecondary = cleaner::onDeleteCancelled,
            onCancel = cleaner::onDeleteCancelled,
        )
    }
}
