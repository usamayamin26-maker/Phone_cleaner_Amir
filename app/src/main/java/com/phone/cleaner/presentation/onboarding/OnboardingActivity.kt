package com.phone.cleaner.presentation.onboarding

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.ActivityOnboardingBinding
import com.phone.cleaner.domain.entities.OnboardingPage
import com.phone.cleaner.presentation.MainActivity
import com.phone.cleaner.presentation.adapters.OnboardingAdapter
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.utils.SharedPref
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.visibleIf
import org.koin.android.ext.android.inject

/**
 * The four onboarding pages, swipeable. Skip leaves from any page; Next turns the page and
 * becomes Continue on the last one. Leaving either way marks onboarding as done and opens Home.
 *
 * ViewPager2 follows the layout direction, so in a right-to-left language the pages run right to
 * left and the indicator is mirrored with them.
 */
class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityOnboardingBinding =
        ActivityOnboardingBinding::inflate

    /** The background runs behind both bars; Skip and the controls take the insets themselves. */
    override val applyDefaultInsets: Boolean = false

    private val sharedPref: SharedPref by inject()
    private val onboardingAdapter: OnboardingAdapter by inject()

    private val pages = OnboardingPage.entries

    /** The design's 12 px between the phone image and the controls, read from the layout. */
    private var controlsGap = 0
    private var navigationBarHeight = 0

    /** The page whose ad keys the one slot pair currently serves; -1 before the first bind. */
    private var boundAdPage = -1

    override fun setupViews(): Unit = with(binding) {
        skipButton.applyStatusBarTopMargin()
        skipButton.expandTouchTarget()
        // The slot is the bottom-most content, so it clears the gesture bar; the pager's bottom is
        // constrained to its top and the controls reserve their room above whichever it is.
        adSlotBottom.applyNavigationBarBottomMargin()
        controlsGap = (controls.layoutParams as ViewGroup.MarginLayoutParams).topMargin

        pager.adapter = onboardingAdapter
        // All four pages stay laid out, so the controls can be placed under the lowest image and do
        // not jump when a page with a longer title comes in.
        pager.offscreenPageLimit = pages.size - 1
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                showControlsFor(position)
                bindPageAds(position)
            }
        })
        showControlsFor(0)
        bindPageAds(0)

        ViewCompat.setOnApplyWindowInsetsListener(controls) { _, insets ->
            navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            reserveRoomForControls()
            insets
        }
        controls.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> reserveRoomForControls() }
        (pager.getChildAt(0) as RecyclerView).addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            placeControlsUnderImages()
        }

        skipButton.onClick { finishOnboarding(skipped = true) }
        actionButton.onClick { goToNextPage() }

        onBackPressedDispatcher.addCallback(this@OnboardingActivity) {
            if (pager.currentItem > 0) {
                pager.setCurrentItem(pager.currentItem - 1, true)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    /**
     * The pill marks the current page. The last page has no indicator and its Continue fills the
     * row, as the design draws it.
     */
    private fun showControlsFor(position: Int) = with(binding) {
        val isLast = position == pages.lastIndex
        indicator.visibleIf(!isLast)
        indicatorGap.visibleIf(!isLast)
        actionButton.setText(if (isLast) R.string.action_continue else R.string.onboarding_next)
        actionButton.updateLayoutParams<LinearLayout.LayoutParams> {
            width = if (isLast) 0 else LinearLayout.LayoutParams.WRAP_CONTENT
            weight = if (isLast) 1f else 0f
        }

        val pillWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._22sdp)
        val dotWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
        indicator.children.forEachIndexed { index, dot ->
            val isCurrent = index == position
            dot.updateLayoutParams<ViewGroup.LayoutParams> { width = if (isCurrent) pillWidth else dotWidth }
            dot.setBackgroundResource(
                if (isCurrent) R.drawable.shape_indicator_active else R.drawable.shape_indicator_inactive,
            )
        }
    }

    /**
     * Keeps the bottom of every page free for the controls and the navigation bar, so on a short
     * screen or at a large font the image shrinks instead of running under them.
     */
    private fun reserveRoomForControls() {
        // The gesture bar is paid once: by the bottom ad slot while it is showing (the pager's
        // bottom is constrained to the slot's top), and by this reserve while it is gone.
        val systemBar = if (binding.adSlotBottom.isVisible) 0 else navigationBarHeight
        onboardingAdapter.bottomReserve = binding.controls.height + 2 * controlsGap + systemBar
    }

    /** The design puts the controls 12 px under the phone image; where that is depends on the page. */
    private fun placeControlsUnderImages() {
        val pageViews = (binding.pager.getChildAt(0) as RecyclerView).children
        val imageBottom = pageViews.maxOfOrNull { it.findViewById<View>(R.id.page_image).bottom } ?: return
        val top = imageBottom + controlsGap
        if ((binding.controls.layoutParams as ConstraintLayout.LayoutParams).topMargin == top) return
        // Called from a layout pass; the new position is applied in the next one.
        binding.controls.post {
            binding.controls.updateLayoutParams<ConstraintLayout.LayoutParams> { topMargin = top }
        }
    }

    /**
     * The one slot pair, rebound to the page that is now showing.
     *
     * Each page keeps its own screen key, because the key is the ad caches' identity: a page's ad
     * is requested once and re-attached from the cache when the user swipes back to it.
     *
     * One slot pair serving four keys is the hazard here. A request for the page being left is
     * still in flight, and its callback writes into these very views - which by then belong to
     * another page. [AdsController.forgetSlots] on the outgoing key drops that request's claim on
     * them, so a late ad is only cached. It has to run before the incoming key is bound, or it
     * would take the new request's claim with it.
     */
    private fun bindPageAds(position: Int) {
        if (position == boundAdPage) return
        pages.getOrNull(boundAdPage)?.let { AdsController.forgetSlots(it.screenKey) }
        boundAdPage = position

        val page = pages[position]
        AdsController.slot(this, binding.adSlotTop, "${page.screenKey}_top", units = UNITS)
        AdsController.slot(this, binding.adSlotBottom, "${page.screenKey}_bottom", units = UNITS)
    }

    /**
     * Next page, with the full-screen native on the one transition the console can ask for it on.
     *
     * `is_show_admob_onboarding_full_screen_native` is a single flag for a single move - page 2 to
     * page 3. The ad comes first and the page turns after it, so the user does not watch the page
     * animate and then have it covered. Off, not ready or failed: the page simply turns.
     */
    private fun goToNextPage() = with(binding.pager) {
        if (currentItem >= pages.lastIndex) {
            finishOnboarding(skipped = false)
            return@with
        }
        val next = currentItem + 1
        if (next != PAGE_WITH_FULL_SCREEN_NATIVE_BEFORE_IT) {
            setCurrentItem(next, true)
            return@with
        }
        AdsController.fullScreenNative(
            this@OnboardingActivity,
            flag = "is_show_admob_onboarding_full_screen_native",
            unitKey = "admob_onboarding_full_screen_native_id",
        ) {
            if (!isFinishing) setCurrentItem(next, true)
        }
    }

    /**
     * Leaving onboarding, by Continue on the last page or by Skip from any page, behind the launch
     * interstitial. Each has its own flag; both share the one unit key.
     *
     * `isOnboardingDone` is set before the ad, not after: whatever happens to the ad, the user has
     * finished onboarding and must never be sent back through it.
     */
    private fun finishOnboarding(skipped: Boolean) {
        Log.d(TAG, "Onboarding finished (skipped=$skipped)")
        sharedPref.isOnboardingDone = true
        AdsController.launchInterstitial(
            this,
            flag = if (skipped) {
                "is_show_admob_onboarding_skip_btn_inter"
            } else {
                "is_show_admob_onboarding_get_started_inter"
            },
            unitKey = "admob_onboarding_get_started_inter_id",
        ) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        pages.forEach { AdsController.forgetSlots(it.screenKey) }
        super.onDestroy()
    }

    private companion object {
        const val TAG = "OnboardingActivity"

        /** All four pages share one unit group: `admob_onboarding_banner_id` / `_native_id`. */
        const val UNITS = "onboarding"

        /** The full-screen native goes in front of page 3, i.e. the 2 -> 3 move. Zero-based. */
        const val PAGE_WITH_FULL_SCREEN_NATIVE_BEFORE_IT = 2
    }
}
