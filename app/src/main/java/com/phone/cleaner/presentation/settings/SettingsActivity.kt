package com.phone.cleaner.presentation.settings

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.activity.addCallback
import androidx.core.net.toUri
import com.phone.cleaner.BuildConfig
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.ActivitySettingsBinding
import com.phone.cleaner.databinding.ItemSettingsRowBinding
import com.phone.cleaner.domain.view_models.SettingsUiEvent
import com.phone.cleaner.domain.view_models.SettingsViewModel
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.presentation.components.RateUsDialog
import com.phone.cleaner.presentation.onboarding.LanguageActivity
import com.phone.cleaner.utils.Constants
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyNavigationBarBottomMarginAboveSlot
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.onClick
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Settings, opened from the Home gear: Language, Share App, Rate Us and Privacy & Policy. The
 * Play Store links are built from the application id, so they follow the app wherever it ships.
 */
class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivitySettingsBinding =
        ActivitySettingsBinding::inflate

    override val applyDefaultInsets: Boolean = false

    private val viewModel: SettingsViewModel by viewModel()

    private var dialog: Dialog? = null

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        // The slot is the bottom-most view, so it clears the gesture bar; the scroll pays only
        // while the slot is gone, through its own gone margin.
        adSlotBottom.applyNavigationBarBottomMargin()
        rowsScroll.applyNavigationBarBottomMarginAboveSlot()

        header.headerTitle.setText(R.string.settings_title)
        header.backButton.root.onClick { leaveWithAd(BACK_INTERSTITIAL_FLAG) }
        header.backButton.root.expandTouchTarget()
        // System back does exactly what the screen's own back button does.
        onBackPressedDispatcher.addCallback(this@SettingsActivity) { leaveWithAd(BACK_INTERSTITIAL_FLAG) }

        rowLanguage.bind(R.drawable.ic_language, R.string.language_title) { viewModel.onLanguageClicked() }
        rowShare.bind(R.drawable.ic_share, R.string.settings_share_app) { viewModel.onShareClicked() }
        rowRate.bind(R.drawable.ic_star, R.string.settings_rate_us) { viewModel.onRateClicked() }
        rowPrivacy.bind(R.drawable.ic_privacy, R.string.settings_privacy_policy) { viewModel.onPrivacyPolicyClicked() }
    }

    override fun observeState() {
        viewModel.events.collectEvents(::handle)
    }

    /** Bound from here, not once at creation, so coming back from Language re-attaches the ads. */
    override fun onResume() {
        super.onResume()
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotMid, "${SCREEN_KEY}_mid", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom", units = SCREEN_KEY)
    }

    private fun ItemSettingsRowBinding.bind(@DrawableRes icon: Int, @StringRes title: Int, onClick: () -> Unit) {
        rowIcon.setImageResource(icon)
        rowTitle.setText(title)
        root.onClick { onClick() }
    }

    private fun handle(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.OpenLanguage -> withEntryAd("is_show_language_btn_inter") {
                startActivity(LanguageActivity.fromSettings(this))
            }
            SettingsUiEvent.ShareApp -> shareApp()
            SettingsUiEvent.RateApp -> {
                dialog?.dismiss()
                dialog = RateUsDialog.show(this, viewModel::onRated)
            }
            SettingsUiEvent.OpenStore -> openStore()
            SettingsUiEvent.ThankForRating -> showToast(getString(R.string.rate_thanks))
            SettingsUiEvent.OpenPrivacyPolicy -> openLink(getString(R.string.privacy_policy_url))
        }
    }

    /** The system share sheet with a line about the app and its Play Store page. */
    private fun shareApp() {
        // The share sheet is another app: coming back from it is not a return from background.
        AdsController.skipNextResume()
        val link = Constants.PLAY_STORE_WEB_URL + BuildConfig.APPLICATION_ID
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_message, getString(R.string.app_name), link))
        startActivity(Intent.createChooser(send, getString(R.string.share_app_chooser)))
    }

    /**
     * The Play Store app's page for this app; the Play Store website where there is no store app.
     * Not the In-App Review card: Play's guidelines do not allow asking for a rating first and
     * showing the card only to happy users, which is what Rate Us does.
     */
    private fun openStore() {
        // The Play Store is another app; see shareApp().
        AdsController.skipNextResume()
        try {
            startActivity(Intent(Intent.ACTION_VIEW, (Constants.PLAY_STORE_APP_URL + BuildConfig.APPLICATION_ID).toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No Play Store app, opening the store website", e)
            openLink(Constants.PLAY_STORE_WEB_URL + BuildConfig.APPLICATION_ID)
        }
    }

    private fun openLink(url: String) {
        // A browser is another app; see shareApp().
        AdsController.skipNextResume()
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Nothing can open $url", e)
            showToast(getString(R.string.link_open_failed))
        }
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        AdsController.forgetSlots(SCREEN_KEY)
        super.onDestroy()
    }

    private companion object {
        const val TAG = "SettingsActivity"

        /** This screen's ad slots and its entry in the ad caches. */
        const val SCREEN_KEY = "settings"

        const val BACK_INTERSTITIAL_FLAG = "is_show_settings_back_inter"
    }
}
