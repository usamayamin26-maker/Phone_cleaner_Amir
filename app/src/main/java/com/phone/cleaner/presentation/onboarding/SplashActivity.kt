package com.phone.cleaner.presentation.onboarding

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.ads.SplashAds
import com.phone.cleaner.databinding.ActivitySplashBinding
import com.phone.cleaner.domain.view_models.SplashViewModel
import com.phone.cleaner.presentation.MainActivity
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.utils.SharedPref
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyNavigationBarBottomMarginAboveSlot
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.gone
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.visible
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * The launcher. The system splash from core-splashscreen covers the cold start with the same logo,
 * then this screen shows the design's splash while the loading bar fills, and moves on to the
 * Language screen, onboarding or Home.
 *
 * It is also where the ads bootstrap starts: consent needs an Activity, so `AdsController.start`
 * cannot live in `BaseApp`. The branded splash runs alongside the consent form, and the screen is
 * not finished while a form is up - it would go down with it.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivitySplashBinding =
        ActivitySplashBinding::inflate

    /** The background runs behind both bars; the bottom row and the slot draw their own insets. */
    override val applyDefaultInsets: Boolean = false

    private val viewModel: SplashViewModel by viewModel()
    private val sharedPref: SharedPref by inject()

    /** The ad that loaded and is waiting behind the Continue button, if one did. */
    private var loadedSplashAd: SplashAds.Loaded? = null

    /** Drops an in-flight splash ad request so a destroyed activity is not held by it. */
    private var cancelSplashAdLoad: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }

    override fun setupViews() = with(binding) {
        statusAnchor.applyStatusBarTopMargin()
        applyBottomInsets()
        bindAdSlots()
        fillProgressBar()
        continueButton.onClick { onContinueClicked() }

        lifecycleScope.launch {
            // Started here, not awaited yet: the consent form (where one is required) has to come
            // up while this activity is still alive, and the branded splash runs alongside it.
            val consent = async { awaitConsentSettled() }
            viewModel.state.first { it.isHoldDone }
            // Routing on would finish this activity and take the consent form down with it, so the
            // splash outstays the form on the rare launch where one is shown.
            consent.await()
            loadSplashAdIfAny()
            if (loadedSplashAd != null) showContinueButton() else routeOnwards()
        }
        Unit
    }

    /**
     * The bottom of this screen, where the navigation bar is.
     *
     * The slot is the bottom-most content and pays for the gesture bar itself. The loading row and
     * Continue sit directly above it and pay only when the slot is not there to - which is the
     * slot's whole life until an ad arrives - so whichever of the three is actually bottom-most
     * pays the inset, and it is paid exactly once.
     */
    private fun applyBottomInsets() = with(binding) {
        adSlotBottom.applyNavigationBarBottomMargin()
        loadingText.applyNavigationBarBottomMarginAboveSlot()
        continueContainer.applyNavigationBarBottomMarginAboveSlot()
    }

    private fun bindAdSlots() {
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom", units = SCREEN_KEY)
    }

    /** The bar fills from the start edge over the hold, so it is full as the splash leaves. */
    private fun fillProgressBar() = binding.progressFill.doOnLayout { fill ->
        fill.pivotX = if (fill.layoutDirection == View.LAYOUT_DIRECTION_RTL) fill.width.toFloat() else 0f
        fill.scaleX = 0f
        fill.animate()
            .scaleX(1f)
            .setDuration(SplashViewModel.HOLD_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /**
     * Swaps the loading row for Continue. Both end at the same edge of the same view, so nothing
     * moves, and nothing above is anchored to either.
     */
    private fun showContinueButton() = with(binding) {
        progressTrack.gone()
        loadingText.gone()
        continueContainer.visible()
    }

    private fun onContinueClicked() {
        val ad = loadedSplashAd
        loadedSplashAd = null
        // Disabled rather than hidden: a second tap while the ad is opening would otherwise route
        // twice, and the button is about to be covered by the ad anyway.
        binding.continueButton.isEnabled = false
        if (ad == null) {
            routeOnwards()
            return
        }
        SplashAds.show(this, ad) { routeOnwards() }
    }

    override fun onDestroy() {
        cancelSplashAdLoad?.invoke()
        cancelSplashAdLoad = null
        loadedSplashAd = null
        AdsController.forgetSlots(SCREEN_KEY)
        super.onDestroy()
    }

    /**
     * The splash full-screen ad, and the only part of this screen that can make the user wait.
     *
     * **The ad is loaded here and shown by the Continue button, never on its own.** While this runs
     * the bottom of the screen carries the loading bar and its line, and an ad that arrives turns
     * that row into Continue. An ad that does not arrive - every flag off, a blank unit id, a
     * failure, a timeout - leaves no button and no wait at all, and the splash routes exactly as it
     * did before any of this existed.
     *
     * It cannot go where the rest of the launch goes. `AdsBootstrap` runs consent, then the SDK,
     * then the config fetch; this screen awaits only consent, because that is the step needing a
     * live Activity. A full-screen ad needs the other two as well - there is no SDK to ask before
     * step 2, and the flag and the unit id only exist after step 3 - so the earliest it can be
     * requested is after the point the splash is otherwise ready to route.
     *
     * So the wait is bounded twice and the user is never held behind an ad that is not coming:
     *
     * 1. [SplashAds.isWorthWaitingFor] reads the config already in hand - persisted from the
     *    previous session, or `AppAds.DEFAULT_JSON` on a first run. With no splash format switched
     *    on, which is what ships, this returns false and the splash does not wait at all.
     * 2. If it is worth waiting for, the wait on the bootstrap is capped at [READY_TIMEOUT_MS] and
     *    the ad's own load inside the managers. Either deadline expiring routes on with no ad. Both
     *    bound the *load* only - once Continue is on screen the user takes as long as they like.
     */
    private suspend fun loadSplashAdIfAny() {
        if (!SplashAds.isWorthWaitingFor()) {
            Log.d(TAG, "No splash full-screen ad asked for; routing without waiting on the bootstrap")
            return
        }
        val canRequestAds = withTimeoutOrNull(READY_TIMEOUT_MS.milliseconds) {
            suspendCancellableCoroutine { continuation ->
                AdsController.whenReady(this@SplashActivity) { canRequestAds ->
                    if (continuation.isActive) continuation.resume(canRequestAds)
                }
            }
        }
        if (canRequestAds == null) {
            Log.w(TAG, "Ads bootstrap not ready within ${READY_TIMEOUT_MS}ms; routing without an ad")
            return
        }
        // Consent said no: no splash full-screen request at all. The slots check this themselves.
        if (!canRequestAds) {
            Log.d(TAG, "Ads not allowed by consent; routing without a splash ad")
            return
        }
        if (isFinishing) return
        loadedSplashAd = suspendCancellableCoroutine { continuation ->
            cancelSplashAdLoad = SplashAds.load(this@SplashActivity) { loaded ->
                cancelSplashAdLoad = null
                if (continuation.isActive) continuation.resume(loaded)
            }
        }
    }

    /**
     * Kicks off the ads bootstrap and waits for its consent step, the only step that needs this
     * activity. Initialising the Mobile Ads SDK and fetching Remote Config carry on afterwards;
     * neither blocks the splash, and no ad can be requested before all three are done.
     *
     * The timeout is a safety net against a callback that never arrives, not a limit on how long
     * the user may take to read the form - cutting a consent form short is worse than a slow
     * splash. A network error resolves in well under a second, so this should never trip.
     */
    private suspend fun awaitConsentSettled() {
        val settled = withTimeoutOrNull(CONSENT_TIMEOUT_MS.milliseconds) {
            suspendCancellableCoroutine { continuation ->
                AdsController.start(this@SplashActivity) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
        if (settled == null) {
            Log.w(TAG, "Consent did not settle within ${CONSENT_TIMEOUT_MS}ms, routing on without it")
        }
    }

    /**
     * Where the launch goes, now that the console has a say.
     *
     * The four flags come in pairs. `*_first_session` applies until Home has opened once
     * ([SharedPref.hasOpenedMainScreen]); after that the plain pair applies, which is what lets a
     * console bring the Language screen back for an existing install. Both default to the
     * behaviour this screen already had: the first-session pair ships at 1 and the other two at 0,
     * so a first run goes Language -> onboarding -> Home and every later run goes straight to Home.
     *
     * [SharedPref.isOnboardingDone] is deliberately **not** consulted. It used to be the router,
     * and leaving it in as an extra condition would quietly defeat `is_show_onboarding_screen` - a
     * console setting it to 1 would have no effect on anyone who had finished onboarding, which is
     * everyone it is aimed at. `hasOpenedMainScreen` already covers what it was there for: a user
     * who quits half way through onboarding has never reached Home, so their next launch is still a
     * first session and still shows it.
     */
    private fun routeOnwards() {
        if (isFinishing) return
        val next = when {
            showLanguageScreen(sharedPref) -> LanguageActivity::class.java
            showOnboardingScreen(sharedPref) -> OnboardingActivity::class.java
            else -> MainActivity::class.java
        }
        Log.d(TAG, "Routing to ${next.simpleName}")
        startActivity(Intent(this, next))
        finish()
    }

    companion object {
        private const val TAG = "SplashActivity"

        /** Safety net only - see [awaitConsentSettled]. */
        private const val CONSENT_TIMEOUT_MS = 30_000L

        /**
         * How long the splash waits for the ads bootstrap *after* the hold, and only when the
         * console has actually asked for a splash full-screen ad. Nothing in the shipped defaults
         * reaches this.
         */
        private const val READY_TIMEOUT_MS = 3_000L

        /** This screen's ad slots and its entry in the ad caches. */
        const val SCREEN_KEY = "splash"

        /** Whether the Language screen runs on this launch; see [routeOnwards]. */
        fun showLanguageScreen(sharedPref: SharedPref): Boolean = launchFlag(
            sharedPref,
            firstSessionKey = "is_show_language_screen_first_session",
            key = "is_show_language_screen",
        )

        /** Whether onboarding runs on this launch; read by the Language screen as well. */
        fun showOnboardingScreen(sharedPref: SharedPref): Boolean = launchFlag(
            sharedPref,
            firstSessionKey = "is_show_onboarding_screen_first_session",
            key = "is_show_onboarding_screen",
        )

        private fun launchFlag(sharedPref: SharedPref, firstSessionKey: String, key: String): Boolean {
            val config = AdsController.config()
            return if (sharedPref.hasOpenedMainScreen) {
                config.isOn(key)
            } else {
                config.int(firstSessionKey, default = 1) == 1
            }
        }
    }
}
