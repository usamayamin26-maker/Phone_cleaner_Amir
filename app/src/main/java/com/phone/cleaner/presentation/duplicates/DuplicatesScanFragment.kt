package com.phone.cleaner.presentation.duplicates

import android.view.LayoutInflater
import android.view.ViewGroup
import com.phone.cleaner.R
import com.phone.cleaner.databinding.FragmentDuplicatesScanBinding
import com.phone.cleaner.domain.view_models.DuplicatesUiState
import com.phone.cleaner.domain.view_models.DuplicatesViewModel
import com.phone.cleaner.presentation.base.BaseFragment
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatPercent
import com.phone.cleaner.utils.onClick
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Scanning (Figma 1:483): the ring and percentage follow the scan. "Stop Scanning" cancels it and
 * leaves - a stopped scan has deleted nothing.
 */
class DuplicatesScanFragment : BaseFragment<FragmentDuplicatesScanBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentDuplicatesScanBinding =
        FragmentDuplicatesScanBinding::inflate

    private val viewModel: DuplicatesViewModel by activityViewModel()

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        header.headerTitle.setText(R.string.action_delete_duplicates)
        header.backButton.root.onClick { stopAndLeave() }
        header.backButton.root.expandTouchTarget()
        stopButton.onClick { stopAndLeave() }
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
    }

    private fun render(state: DuplicatesUiState) = with(binding) {
        progressRing.progress = state.progress
        progressPercent.text = requireContext().formatPercent(state.progress * 100, 0)
    }

    private fun stopAndLeave() {
        viewModel.stopScan()
        requireActivity().finish()
    }
}
