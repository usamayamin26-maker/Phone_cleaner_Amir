package com.phone.cleaner.presentation.onboarding

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.ActivityLanguageBinding
import com.phone.cleaner.domain.view_models.LanguageUiEvent
import com.phone.cleaner.domain.view_models.LanguageUiState
import com.phone.cleaner.domain.view_models.LanguageViewModel
import com.phone.cleaner.presentation.MainActivity
import com.phone.cleaner.presentation.adapters.LanguageAdapter
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.utils.SharedPref
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyNavigationBarBottomMarginAboveSlot
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.visibleIf
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Language picker, as in UniversalRemoteControl. A tap selects a language; Continue stores it in
 * SharedPref and applies it straight away, so the rest of the app is already in that language.
 *
 * Applying a locale recreates this activity (LocaleManager from API (Application Programming
 * Interface) level 33, AppCompatDelegate below it), and the new instance cannot tell why it was
 * created. [SharedPref.isLocaleChangePending] carries that across: it is set just before the
 * locale is applied, and the first [onCreate] that sees it clears it and moves on, exactly once.
 * Choosing the language already in effect recreates nothing, so that case moves on directly.
 *
 * On the first run the screen moves on to onboarding (or Home once onboarding is done) and has no
 * back button. Settings opens it with [EXTRA_FROM_SETTINGS]; then it closes back to Settings. The
 * extra survives the recreation, because an activity is recreated with its own intent.
 */
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityLanguageBinding =
        ActivityLanguageBinding::inflate

    override val applyDefaultInsets: Boolean = false

    private val viewModel: LanguageViewModel by viewModel()
    private val languageAdapter: LanguageAdapter by inject()
    private val sharedPref: SharedPref by inject()

    private val isFromSettings: Boolean
        get() = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (sharedPref.isLocaleChangePending) {
            sharedPref.isLocaleChangePending = false
            Log.d(TAG, "Recreated with the chosen locale in effect")
            leave()
        }
    }

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        // The slot is the bottom-most content, so it is what clears the gesture bar; Continue pays
        // only while the slot is gone.
        adSlotBottom.applyNavigationBarBottomMargin()
        continueContainer.applyNavigationBarBottomMarginAboveSlot()

        header.headerTitle.setText(R.string.language_title)
        header.backButton.root.visibleIf(isFromSettings)
        header.backButton.root.onClick { leaveWithAd(BACK_INTERSTITIAL_FLAG) }
        header.backButton.root.expandTouchTarget()
        // System back does exactly what the screen's own back button does.
        if (isFromSettings) {
            onBackPressedDispatcher.addCallback(this@LanguageActivity) {
                leaveWithAd(BACK_INTERSTITIAL_FLAG)
            }
        }

        languages.adapter = languageAdapter
        languageAdapter.onLanguageClick = { viewModel.onLanguageSelected(it.tag) }
        continueButton.onClick { viewModel.onContinueClicked() }
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
        viewModel.events.collectEvents(::handle)
    }

    /**
     * Bound from [onResume] rather than once at creation: the screen is visible from here on, and
     * coming back to it - from Settings, or after the locale recreated it - is exactly when a
     * cached ad should be re-attached or an expired one replaced.
     */
    override fun onResume() {
        super.onResume()
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom", units = SCREEN_KEY)
    }

    override fun onDestroy() {
        AdsController.forgetSlots(SCREEN_KEY)
        super.onDestroy()
    }

    private fun render(state: LanguageUiState) {
        languageAdapter.selectedTag = state.selectedTag
        languageAdapter.submitList(state.languages)
    }

    private fun handle(event: LanguageUiEvent) {
        when (event) {
            is LanguageUiEvent.LanguageStored -> applyLocaleAndContinue(event.tag)
        }
    }

    private fun applyLocaleAndContinue(tag: String) {
        if (currentLocaleTags() == tag) {
            Log.d(TAG, "Locale $tag is already in effect")
            leave()
            return
        }
        Log.d(TAG, "Applying locale $tag; this activity is about to be recreated")
        sharedPref.isLocaleChangePending = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    private fun currentLocaleTags(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java)?.applicationLocales?.toLanguageTags()
        } else {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }

    /**
     * Back to Settings, or on along the first run, behind the launch interstitial.
     *
     * Whether onboarding runs is the console's call now
     * ([SplashActivity.showOnboardingScreen]), not `isOnboardingDone`: leaving that in as an extra
     * condition would quietly defeat `is_show_onboarding_screen` for everyone it is aimed at. It is
     * still written by onboarding, and read by nothing that routes.
     *
     * Coming from Settings this simply closes: Continue there is "done", not a step of the launch
     * flow, and the screen the user lands on is the one they came from.
     */
    private fun leave() {
        if (isFromSettings) {
            finish()
            return
        }
        val next = if (SplashActivity.showOnboardingScreen(sharedPref)) {
            OnboardingActivity::class.java
        } else {
            MainActivity::class.java
        }
        val show = {
            AdsController.launchInterstitial(this, DONE_INTERSTITIAL_FLAG, DONE_INTERSTITIAL_UNIT) {
                startActivity(Intent(this, next))
                finish()
            }
        }
        // launchInterstitial runs either way, so its "flag off" log line is written either way.
        // Only the path that really shows an ad is posted: on the instance the locale recreated
        // this runs from onCreate, where a dialog has no window token yet. With no ad there is
        // nothing to post and the screen moves on this frame, exactly as it always did.
        if (AdsController.willShowLaunchInterstitial(DONE_INTERSTITIAL_FLAG, DONE_INTERSTITIAL_UNIT)) {
            binding.root.post { if (!isFinishing) show() }
        } else {
            show()
        }
    }

    companion object {
        private const val TAG = "LanguageActivity"

        /** Set by Settings: close on Continue instead of going on to onboarding. */
        private const val EXTRA_FROM_SETTINGS = "extra_from_settings"

        /** This screen's ad slots and its entry in the ad caches. */
        const val SCREEN_KEY = "language"

        private const val DONE_INTERSTITIAL_FLAG = "is_show_admob_language_done_btn_inter"
        private const val DONE_INTERSTITIAL_UNIT = "admob_language_done_btn_inter_id"

        /** Only reachable from Settings, where this screen has a back button. */
        private const val BACK_INTERSTITIAL_FLAG = "is_show_admob_language_back_inter"

        fun fromSettings(context: Context): Intent =
            Intent(context, LanguageActivity::class.java).putExtra(EXTRA_FROM_SETTINGS, true)
    }
}
