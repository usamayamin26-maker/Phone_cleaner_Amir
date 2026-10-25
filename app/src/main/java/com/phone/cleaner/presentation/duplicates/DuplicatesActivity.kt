package com.phone.cleaner.presentation.duplicates

import android.app.Activity
import android.app.Dialog
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ActivityDuplicatesBinding
import com.phone.cleaner.domain.entities.DuplicatesSection
import com.phone.cleaner.domain.repository.StorageAccessRepository
import com.phone.cleaner.domain.view_models.DuplicatesUiEvent
import com.phone.cleaner.domain.view_models.DuplicatesUiState
import com.phone.cleaner.domain.view_models.DuplicatesViewModel
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.presentation.components.AppDialog
import com.phone.cleaner.presentation.components.StorageAccessGate
import com.phone.cleaner.utils.SharedPref
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.toReadableSize
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Delete Duplicates. Hosts its three pages - scanning, results and a section's details - and owns
 * what they share: storage access before the scan, the "Are You Sure?" dialog (Figma 1:984) that
 * every delete goes through, and the system consent Android 11+ asks for without All files access.
 */
class DuplicatesActivity : BaseActivity<ActivityDuplicatesBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityDuplicatesBinding =
        ActivityDuplicatesBinding::inflate

    /** Each page draws its own header below the status bar. */
    override val applyDefaultInsets: Boolean = false

    private val viewModel: DuplicatesViewModel by viewModel()
    private val storageAccess: StorageAccessRepository by inject()
    private val sharedPref: SharedPref by inject()

    /** Photos and videos are enough: they go through MediaStore. */
    private val accessGate by lazy {
        StorageAccessGate(
            activity = this,
            repository = storageAccess,
            sharedPref = sharedPref,
            level = StorageAccessGate.Level.MEDIA,
            featureName = R.string.action_delete_duplicates,
            onGranted = { if (!viewModel.hasStarted) viewModel.startScan() },
            onDeclined = { finish() },
        )
    }

    private val consentLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onConsentResult(result.resultCode == Activity.RESULT_OK)
        }

    private var dialog: Dialog? = null

    override fun setupViews() {
        // A fresh ViewModel has no results - first launch, or the process was killed and the
        // pages restored - so the screen starts again from scanning, whatever page was showing.
        if (!viewModel.hasStarted) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.commit { replace(R.id.duplicates_container, DuplicatesScanFragment()) }
        }
        // Driven from here, not from onStart: a one-shot event emitted during start-up is lost.
        if (!viewModel.hasStarted) accessGate.request()
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
        viewModel.events.collectEvents(::handle)
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        super.onDestroy()
    }

    /** "View All" or a section card: its page, on the back stack over the results. */
    fun openSection(section: DuplicatesSection) {
        supportFragmentManager.commit {
            replace(R.id.duplicates_container, DuplicatesDetailFragment.newInstance(section))
            addToBackStack(section.name)
        }
    }

    private fun render(state: DuplicatesUiState) {
        // Results once the scan is done, unless they (or a section's page) already show. A quick
        // scan can finish before the scanning page's own commit has run, so an empty container
        // counts as scanning too; the two commits then run in order.
        val current = supportFragmentManager.findFragmentById(R.id.duplicates_container)
        if (state.isScanComplete && current !is DuplicatesResultsFragment && current !is DuplicatesDetailFragment) {
            Log.d(TAG, "Showing results")
            supportFragmentManager.commit { replace(R.id.duplicates_container, DuplicatesResultsFragment()) }
        }
        if (state.error != null) {
            showToast(getString(R.string.error_generic))
            viewModel.clearError()
        }
    }

    private fun handle(event: DuplicatesUiEvent) {
        when (event) {
            is DuplicatesUiEvent.ConfirmDelete -> confirmDelete(event.count, event.bytes)
            is DuplicatesUiEvent.RequestConsent ->
                consentLauncher.launch(IntentSenderRequest.Builder(event.intentSender).build())
            is DuplicatesUiEvent.Deleted -> showToast(
                if (event.failedCount > 0) {
                    getString(R.string.duplicates_delete_partial, event.freedBytes.toReadableSize(this))
                } else {
                    getString(R.string.duplicates_freed, event.freedBytes.toReadableSize(this))
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
            onPrimary = viewModel::onDeleteConfirmed,
            onSecondary = viewModel::onDeleteCancelled,
            onCancel = viewModel::onDeleteCancelled,
        )
    }

    private companion object {
        const val TAG = "DuplicatesActivity"
    }
}
