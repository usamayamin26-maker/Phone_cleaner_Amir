package com.phone.cleaner.presentation.appmanager

import ai.jhw.appmanager.model.InstalledApp
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.addCallback
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.ActivityAppCleanerBinding
import com.phone.cleaner.domain.view_models.AppCleanerUiEvent
import com.phone.cleaner.domain.view_models.AppCleanerUiState
import com.phone.cleaner.domain.view_models.AppCleanerViewModel
import com.phone.cleaner.presentation.adapters.AppAdapter
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.presentation.components.AppDialog
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyNavigationBarBottomMarginAboveSlot
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.visibleIf
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * App Cleaner (Figma 1:1778): the user's apps, largest first. A tap opens the design's dialog
 * (1:1881) with the app's icon: Uninstall opens the system uninstall dialog, Clear cache the app's
 * App info page. Nothing is removed by this app itself.
 */
class AppCleanerActivity : BaseActivity<ActivityAppCleanerBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityAppCleanerBinding =
        ActivityAppCleanerBinding::inflate

    override val applyDefaultInsets: Boolean = false

    private val viewModel: AppCleanerViewModel by viewModel()
    private val appAdapter: AppAdapter by inject()

    private var dialog: Dialog? = null

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        // The slot is the bottom-most view, so it clears the gesture bar; the list pays only while
        // the slot is gone, through its own gone margin.
        adSlotBottom.applyNavigationBarBottomMargin()
        apps.applyNavigationBarBottomMarginAboveSlot()

        header.headerTitle.setText(R.string.action_app_cleaner)
        header.backButton.root.onClick { leaveWithAd(BACK_INTERSTITIAL_FLAG) }
        header.backButton.root.expandTouchTarget()
        // System back does exactly what the screen's own back button does.
        onBackPressedDispatcher.addCallback(this@AppCleanerActivity) { leaveWithAd(BACK_INTERSTITIAL_FLAG) }

        apps.adapter = appAdapter
        appAdapter.onAppClick = viewModel::onAppClicked
        usagePrompt.onClick { viewModel.onUsageAccessPromptClicked() }
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
        viewModel.events.collectEvents(::handle)
    }

    override fun onResume() {
        super.onResume()
        // Back from the uninstall dialog, App info or the Usage access settings.
        viewModel.onScreenResumed()
        // Bound from here, not once at creation: coming back from one of those is exactly when a
        // cached ad should be re-attached or an expired one replaced.
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom", units = SCREEN_KEY)
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        AdsController.forgetSlots(SCREEN_KEY)
        super.onDestroy()
    }

    private fun render(state: AppCleanerUiState) = with(binding) {
        loading.visibleIf(state.isLoading && state.apps.isEmpty())
        usagePrompt.visibleIf(state.showUsageAccessPrompt && !state.isLoading)
        appAdapter.submitList(state.apps)
        if (state.error != null) {
            showToast(getString(R.string.error_generic))
            viewModel.clearError()
        }
    }

    private fun handle(event: AppCleanerUiEvent) {
        when (event) {
            is AppCleanerUiEvent.ShowAppDialog -> showAppDialog(event.app)
            is AppCleanerUiEvent.OpenSystemScreen -> try {
                // The uninstall dialog, App info and the Usage access settings are all another
                // app, so coming back from them is not a return from background. **And no ad of
                // any kind is put in front of the system uninstall dialog**: it is the moment the
                // user decides, and nothing of the app's may sit between them and it.
                AdsController.skipNextResume()
                startActivity(event.intent)
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No system screen for ${event.intent}", e)
                showToast(getString(R.string.link_open_failed))
            }
        }
    }

    private fun showAppDialog(app: InstalledApp) {
        val icon = try {
            packageManager.getApplicationIcon(app.packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        dialog?.dismiss()
        dialog = AppDialog.show(
            context = this,
            spec = AppDialog.Spec(
                title = app.label,
                message = getString(R.string.app_cleaner_uninstall_message),
                primaryLabel = getString(R.string.app_cleaner_uninstall),
                secondaryLabel = getString(R.string.app_cleaner_clear_cache),
                icon = icon,
                variant = AppDialog.Variant.APP,
            ),
            onPrimary = { viewModel.onUninstallClicked(app) },
            onSecondary = { viewModel.onClearCacheClicked(app) },
        )
    }

    private companion object {
        const val TAG = "AppCleanerActivity"

        /** This screen's ad slots and its entry in the ad caches. */
        const val SCREEN_KEY = "app_cleaner"

        const val BACK_INTERSTITIAL_FLAG = "is_show_app_cleaner_back_inter"
    }
}
