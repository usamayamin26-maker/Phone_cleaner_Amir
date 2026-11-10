package com.phone.cleaner.presentation.components

import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.FragmentCleanerScanBinding
import com.phone.cleaner.domain.view_models.CleanerUiState
import com.phone.cleaner.presentation.base.BaseFragment
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatPercent
import com.phone.cleaner.utils.onClick

/**
 * Scanning (Figma 1:483 / 1:3 / 1:997), a page every cleaner screen shares: the ring and percentage
 * follow the scan. "Stop Scanning" and back (the header's or the system's) ask first with "Wait! Are
 * you sure?"; its Stop Anyway cancels the scan and leaves - a stopped scan has deleted nothing.
 *
 * **This page shows inline ads only.** No full-screen ad goes in front of a running scan and none
 * goes in front of leaving one: the user pressing Stop or back is asking for the scan to end now,
 * and an interstitial on top of that reads as the app refusing to stop. The cleaner's back
 * interstitial lives on the results page, where there is nothing in flight.
 */
class CleanerScanFragment : BaseFragment<FragmentCleanerScanBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCleanerScanBinding =
        FragmentCleanerScanBinding::inflate

    private val host: CleanerHost get() = requireActivity() as CleanerHost

    private var leaveDialog: Dialog? = null

    override fun setupViews() = with(binding) {
        val screen = host.screen
        header.root.applyStatusBarTopMargin()
        // The slot is the bottom-most view of the page, so it clears the gesture bar.
        adSlotBottom.applyNavigationBarBottomMargin()
        header.headerTitle.setText(screen.title)
        header.backButton.root.onClick { confirmLeave() }
        header.backButton.root.expandTouchTarget()
        scanTitle.setText(screen.scanTitle)
        if (!screen.scanTextPadded) {
            val gap = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
            scanTitle.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = gap }
            stopButton.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = gap }
        }
        // Stop Scanning asks first too, like back.
        stopButton.onClick { confirmLeave() }
        // System back does exactly what the screen's own back button does (the "Wait! Are you
        // sure?" dialog) - and neither shows an ad.
        AdsController.routeSystemBack(this@CleanerScanFragment, header.backButton.root)
    }

    override fun observeState() {
        host.cleaner.state.collectState(::render)
    }

    /** Bound from here, not once at creation, so a cached ad is re-attached when the page returns. */
    override fun onResume() {
        super.onResume()
        val screen = host.screen
        AdsController.slot(requireActivity(), binding.adSlotTop, "${screen.adsKey}_scan_top", screen.adsUnits)
        AdsController.slot(requireActivity(), binding.adSlotBottom, "${screen.adsKey}_scan_bottom", screen.adsUnits)
    }

    /**
     * Gone with the page: when the scan finishes while the dialog is open, the results replace this
     * page and the dialog closes with it.
     */
    override fun onDestroyView() {
        leaveDialog?.dismiss()
        leaveDialog = null
        AdsController.forgetSlots("${host.screen.adsKey}_scan")
        super.onDestroyView()
    }

    /** Back while scanning asks first; the scan keeps running behind the dialog. */
    private fun confirmLeave() {
        if (leaveDialog?.isShowing == true) return
        leaveDialog = AppDialog.showLeaveScan(requireContext(), onStop = ::stopAndLeave)
    }

    private fun render(state: CleanerUiState) = with(binding) {
        progressRing.progress = state.progress
        progressPercent.text = requireContext().formatPercent(state.progress * 100, 0)
    }

    private fun stopAndLeave() {
        host.cleaner.stopScan()
        requireActivity().finish()
    }
}
