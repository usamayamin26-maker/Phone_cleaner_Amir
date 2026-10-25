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
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ActivityOnboardingBinding
import com.phone.cleaner.domain.entities.OnboardingPage
import com.phone.cleaner.presentation.MainActivity
import com.phone.cleaner.presentation.adapters.OnboardingAdapter
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.utils.SharedPref
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

    override fun setupViews(): Unit = with(binding) {
        skipButton.applyStatusBarTopMargin()
        skipButton.expandTouchTarget()
        controlsGap = (controls.layoutParams as ViewGroup.MarginLayoutParams).topMargin

        pager.adapter = onboardingAdapter
        // All four pages stay laid out, so the controls can be placed under the lowest image and do
        // not jump when a page with a longer title comes in.
        pager.offscreenPageLimit = pages.size - 1
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = showControlsFor(position)
        })
        showControlsFor(0)

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
        onboardingAdapter.bottomReserve =
            binding.controls.height + 2 * controlsGap + navigationBarHeight
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

    private fun goToNextPage() = with(binding.pager) {
        if (currentItem >= pages.lastIndex) {
            finishOnboarding(skipped = false)
        } else {
            setCurrentItem(currentItem + 1, true)
        }
    }

    private fun finishOnboarding(skipped: Boolean) {
        Log.d(TAG, "Onboarding finished (skipped=$skipped)")
        sharedPref.isOnboardingDone = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private companion object {
        const val TAG = "OnboardingActivity"
    }
}
