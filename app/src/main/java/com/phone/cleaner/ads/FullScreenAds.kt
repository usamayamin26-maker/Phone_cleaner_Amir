package com.phone.cleaner.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Full-screen ads on a user action: interstitial, rewarded and full-screen native. Reached through
 * [AdsController]; see there for what each placement means.
 *
 * One interstitial path serves every placement — a tab, a button that opens a screen, back, a launch
 * button — and they differ only in their arguments: the `is_show_*` flag, which `counter_*` it draws
 * on (none for the launch buttons, which fire at most once a session), which unit, and how long the
 * user may wait behind the loading dialog.
 *
 * `onDone` always runs exactly once — immediately when the flag is off, the counter says no, the unit
 * is blank or the bootstrap is not ready, otherwise once the ad is dismissed, fails or times out — on
 * every path but an activity that is gone. While one ad is in flight further taps are ignored, so a
 * double tap cannot stack two.
 */
internal object FullScreenAds {

    private var interstitialInProgress = false
    private var rewardedInProgress = false

    /** Longest wait behind the loading dialog for a rewarded ad, as for an interstitial. */
    private const val REWARDED_TIMEOUT_MS = 8_000L

    /** Whether [interstitial] would put an ad on screen right now, without starting one. */
    fun willShowInterstitial(flag: String, unitKey: String?): Boolean {
        if (interstitialInProgress) return false
        if (!AdsBootstrap.mayRequestNow()) return false
        val config = RemoteConfigManager.current()
        return config.isOn(flag) && AdUnitIds.interstitial(config.unit(unitKey, config.appInterId)).isNotBlank()
    }

    fun interstitial(
        activity: Activity,
        flag: String,
        counter: String?,
        unitKey: String?,
        timeoutMs: Long?,
        onDone: () -> Unit,
    ) {
        if (interstitialInProgress) return
        // Not [AdsBootstrap.whenReady]: this runs on a tap the user is waiting behind, so a bootstrap
        // that has not finished means no ad rather than a wait. It also keeps the click counter from
        // being spent on a tap that could never have shown one.
        if (!AdsBootstrap.mayRequestNow(activity)) {
            onDone()
            return
        }

        val config = RemoteConfigManager.current()
        val adUnitId = AdUnitIds.interstitial(config.unit(unitKey, config.appInterId))
        if (!config.isOn(flag) || adUnitId.isBlank() ||
            (counter != null && !InterstitialClickGates.shouldShow(config, counter))
        ) {
            onDone()
            return
        }

        interstitialInProgress = true
        val dialog = AdLoadingDialog.show(activity)
        // A dialog belongs to its activity's window: if the activity goes while the ad loads, the
        // dialog goes with it and the next tap is not blocked by a flag nobody clears.
        val whileAlive = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                interstitialInProgress = false
                dialog.dismissSafely()
            }
        }
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle
        lifecycle?.addObserver(whileAlive)

        val finish: (Boolean) -> Unit = {
            interstitialInProgress = false
            lifecycle?.removeObserver(whileAlive)
            dialog.dismissSafely()
            if (!activity.isFinishing && !activity.isDestroyed) onDone()
        }
        if (timeoutMs != null) {
            InterstitialAdManager.loadAndShow(activity, adUnitId, isEnabled = true, timeoutMs = timeoutMs, onComplete = finish)
        } else {
            InterstitialAdManager.loadAndShow(activity, adUnitId, isEnabled = true, onComplete = finish)
        }
    }

    /**
     * Rewarded ad, then [onDone] — watched, skipped, failed, timed out or switched off, the action
     * continues. The user waits behind the loading dialog for at most [REWARDED_TIMEOUT_MS] from the
     * tap (bootstrap + load); an ad that arrives after that is dropped, never shown.
     */
    fun rewarded(activity: Activity, flag: String, unitKey: String?, onDone: () -> Unit) {
        if (rewardedInProgress) return
        val config = RemoteConfigManager.current()
        val unitId = AdUnitIds.rewarded(config.unit(unitKey, config.appRewardedId))
        if (!config.isOn(flag) || unitId.isBlank()) {
            onDone()
            return
        }
        val placement = activity.javaClass.simpleName
        rewardedInProgress = true
        val dialog = AdLoadingDialog.show(activity)
        val mainHandler = Handler(Looper.getMainLooper())
        var finished = false
        // Same as the interstitial: if the activity goes while the ad loads, the dialog goes with it
        // and the next tap is not blocked by a flag nobody clears.
        val whileAlive = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                finished = true
                rewardedInProgress = false
                mainHandler.removeCallbacksAndMessages(null)
                dialog.dismissSafely()
                FullScreenAdState.end()
            }
        }
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle
        lifecycle?.addObserver(whileAlive)

        fun finish() {
            if (finished) return
            finished = true
            rewardedInProgress = false
            mainHandler.removeCallbacksAndMessages(null)
            lifecycle?.removeObserver(whileAlive)
            dialog.dismissSafely()
            FullScreenAdState.end()
            if (!activity.isFinishing && !activity.isDestroyed) onDone()
        }
        mainHandler.postDelayed({
            if (!finished) AdLogger.timeout(AdLogger.REWARDED, placement, REWARDED_TIMEOUT_MS)
            finish()
        }, REWARDED_TIMEOUT_MS)

        AdsBootstrap.whenReady(activity) { canRequestAds ->
            if (finished) return@whenReady
            if (!canRequestAds || activity.isFinishing) {
                finish()
                return@whenReady
            }
            FullScreenAdState.begin(activity)
            AdLogger.request(AdLogger.REWARDED, placement, unitId)
            RewardedAd.load(activity, unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    // Timed out (or the activity went) while it loaded: drop it, the user has moved on.
                    if (finished || activity.isFinishing || activity.isDestroyed) {
                        AdLogger.skipped(AdLogger.REWARDED, placement, "arrived after the timeout or the activity went")
                        finish()
                        return
                    }
                    AdLogger.loaded(AdLogger.REWARDED, placement)
                    mainHandler.removeCallbacksAndMessages(null)
                    dialog.dismissSafely()
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() = AdLogger.shown(AdLogger.REWARDED, placement)
                        override fun onAdDismissedFullScreenContent() = finish()
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            AdLogger.showFailed(AdLogger.REWARDED, placement, error)
                            finish()
                        }
                    }
                    ad.show(activity) { /* reward earned — the action itself is the reward */ }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!finished) AdLogger.failed(AdLogger.REWARDED, placement, error)
                    finish()
                }
            })
        }
    }

    /** Full-screen native (no click counter), then [onDone] — closed, failed, off or not ready. */
    fun fullScreenNative(activity: Activity, flag: String, unitKey: String?, onDone: () -> Unit) {
        if (!RemoteConfigManager.current().isOn(flag) || !AdsBootstrap.mayRequestNow(activity)) {
            onDone()
            return
        }
        val config = RemoteConfigManager.current()
        FullScreenNativeAdLoader.loadAndShow(
            activity = activity,
            adUnitId = AdUnitIds.native(config.unit(unitKey, config.appNativeId)),
            isEnabled = true,
        ) {
            if (!activity.isFinishing && !activity.isDestroyed) onDone()
        }
    }
}
