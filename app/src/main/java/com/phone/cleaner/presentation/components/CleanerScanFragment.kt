package com.phone.cleaner.presentation.components

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import com.phone.cleaner.databinding.FragmentCleanerScanBinding
import com.phone.cleaner.domain.view_models.CleanerUiState
import com.phone.cleaner.presentation.base.BaseFragment
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatPercent
import com.phone.cleaner.utils.onClick

/**
 * Scanning (Figma 1:483 / 1:3 / 1:997), a page every cleaner screen shares: the ring and percentage
 * follow the scan. "Stop Scanning" cancels it and leaves - a stopped scan has deleted nothing.
 */
class CleanerScanFragment : BaseFragment<FragmentCleanerScanBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCleanerScanBinding =
        FragmentCleanerScanBinding::inflate

    private val host: CleanerHost get() = requireActivity() as CleanerHost

    override fun setupViews() = with(binding) {
        val screen = host.screen
        header.root.applyStatusBarTopMargin()
        header.headerTitle.setText(screen.title)
        header.backButton.root.onClick { stopAndLeave() }
        header.backButton.root.expandTouchTarget()
        scanTitle.setText(screen.scanTitle)
        if (!screen.scanTextPadded) {
            val gap = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
            scanTitle.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = gap }
            stopButton.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = gap }
        }
        stopButton.onClick { stopAndLeave() }
    }

    override fun observeState() {
        host.cleaner.state.collectState(::render)
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
