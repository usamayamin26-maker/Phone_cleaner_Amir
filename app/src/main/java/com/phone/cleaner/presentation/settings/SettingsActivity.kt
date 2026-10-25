package com.phone.cleaner.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.phone.cleaner.BuildConfig
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ActivitySettingsBinding
import com.phone.cleaner.databinding.ItemSettingsRowBinding
import com.phone.cleaner.domain.view_models.SettingsUiEvent
import com.phone.cleaner.domain.view_models.SettingsViewModel
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.presentation.onboarding.LanguageActivity
import com.phone.cleaner.utils.Constants
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
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

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        rows.applyNavigationBarBottomMargin()

        header.headerTitle.setText(R.string.settings_title)
        header.backButton.root.onClick { finish() }
        header.backButton.root.expandTouchTarget()

        rowLanguage.bind(R.drawable.ic_language, R.string.language_title) { viewModel.onLanguageClicked() }
        rowShare.bind(R.drawable.ic_share, R.string.settings_share_app) { viewModel.onShareClicked() }
        rowRate.bind(R.drawable.ic_star, R.string.settings_rate_us) { viewModel.onRateClicked() }
        rowPrivacy.bind(R.drawable.ic_privacy, R.string.settings_privacy_policy) { viewModel.onPrivacyPolicyClicked() }
    }

    override fun observeState() {
        viewModel.events.collectEvents(::handle)
    }

    private fun ItemSettingsRowBinding.bind(@DrawableRes icon: Int, @StringRes title: Int, onClick: () -> Unit) {
        rowIcon.setImageResource(icon)
        rowTitle.setText(title)
        root.onClick { onClick() }
    }

    private fun handle(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.OpenLanguage -> startActivity(LanguageActivity.fromSettings(this))
            SettingsUiEvent.ShareApp -> shareApp()
            SettingsUiEvent.RateApp -> rateApp()
            SettingsUiEvent.OpenPrivacyPolicy -> openLink(getString(R.string.privacy_policy_url))
        }
    }

    /** The system share sheet with a line about the app and its Play Store page. */
    private fun shareApp() {
        val link = Constants.PLAY_STORE_WEB_URL + BuildConfig.APPLICATION_ID
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_message, getString(R.string.app_name), link))
        startActivity(Intent.createChooser(send, getString(R.string.share_app_chooser)))
    }

    /** The Play Store app's page for this app; the Play Store website where there is no store app. */
    private fun rateApp() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, (Constants.PLAY_STORE_APP_URL + BuildConfig.APPLICATION_ID).toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No Play Store app, opening the store website", e)
            openLink(Constants.PLAY_STORE_WEB_URL + BuildConfig.APPLICATION_ID)
        }
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Nothing can open $url", e)
            showToast(getString(R.string.link_open_failed))
        }
    }

    private companion object {
        const val TAG = "SettingsActivity"
    }
}
