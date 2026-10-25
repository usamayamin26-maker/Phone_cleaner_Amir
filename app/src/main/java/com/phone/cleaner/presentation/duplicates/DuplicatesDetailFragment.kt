package com.phone.cleaner.presentation.duplicates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Switch
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.phone.cleaner.R
import com.phone.cleaner.databinding.FragmentDuplicatesDetailBinding
import com.phone.cleaner.databinding.ViewSelectAllBinding
import com.phone.cleaner.domain.entities.DuplicatesSection
import com.phone.cleaner.domain.view_models.DuplicatesUiState
import com.phone.cleaner.domain.view_models.DuplicatesViewModel
import com.phone.cleaner.presentation.adapters.DuplicateGroupAdapter
import com.phone.cleaner.presentation.base.BaseFragment
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.setOn
import com.phone.cleaner.utils.toReadableSize
import com.phone.cleaner.utils.visibleIf
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * One section's page (Figma 1:661, 1:827, 1:738): its groups with a check on every copy,
 * "Select All", "Keep best one" for photos and videos, and "Delete" for this section's selection.
 */
class DuplicatesDetailFragment : BaseFragment<FragmentDuplicatesDetailBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentDuplicatesDetailBinding =
        FragmentDuplicatesDetailBinding::inflate

    private val viewModel: DuplicatesViewModel by activityViewModel()
    private val groupAdapter: DuplicateGroupAdapter by inject()

    private val section: DuplicatesSection
        get() = DuplicatesSection.valueOf(requireArguments().getString(ARG_SECTION) ?: DuplicatesSection.IMAGES.name)

    private var selectAll: ViewSelectAllBinding? = null

    /** Read by the accessibility delegate, so TalkBack announces the switch as on or off. */
    private var isKeepBestOn = false

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        deleteContainer.applyNavigationBarBottomMargin()
        header.headerTitle.setText(title(section))
        header.backButton.root.onClick { parentFragmentManager.popBackStack() }
        header.backButton.root.expandTouchTarget()

        selectAll = ViewSelectAllBinding.inflate(layoutInflater, header.headerTrailing, true).also {
            it.root.onClick(intervalMs = 0) { viewModel.onSelectAllClicked(section) }
        }

        keepBest.visibleIf(section.hasGroups)
        keepBest.contentDescription = getString(R.string.duplicates_keep_best)
        keepBest.onClick(intervalMs = 0) { viewModel.onKeepBestChanged(section, !isKeepBestOn) }
        ViewCompat.setAccessibilityDelegate(keepBest, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: android.view.View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = Switch::class.java.name
                info.isCheckable = true
                info.setChecked(
                    if (isKeepBestOn) AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE else AccessibilityNodeInfoCompat.CHECKED_STATE_FALSE,
                )
            }
        })

        groups.adapter = groupAdapter
        groupAdapter.onFileClick = { viewModel.onFileClicked(section, it) }
        deleteButton.onClick { viewModel.onDeleteClicked(section) }
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
    }

    override fun onDestroyView() {
        selectAll = null
        super.onDestroyView()
    }

    private fun render(state: DuplicatesUiState) = with(binding) {
        val context = requireContext()
        val section = section

        val allSelected = state.isAllSelected(section)
        selectAll?.apply {
            selectAllCircle.setBackgroundResource(
                if (allSelected) R.drawable.shape_check_on else R.drawable.shape_select_all_off,
            )
            selectAllTick.visibleIf(allSelected)
            ViewCompat.setStateDescription(
                root,
                getString(if (allSelected) R.string.state_selected else R.string.state_not_selected),
            )
        }

        isKeepBestOn = section in state.keepBest
        keepBestSwitch.setOn(isKeepBestOn)
        ViewCompat.setStateDescription(keepBest, getString(if (isKeepBestOn) R.string.state_on else R.string.state_off))

        groupAdapter.submitList(items(state, section))

        val selected = state.selected(section)
        deleteButton.text = getString(
            R.string.duplicates_delete_button,
            context.formatNumber(selected.size),
            selected.sumOf { it.sizeBytes }.toReadableSize(context),
        )
        deleteButton.isEnabled = selected.isNotEmpty() && !state.isDeleting
        deleteButton.alpha = if (deleteButton.isEnabled) 1f else DISABLED_ALPHA
    }

    /** Photos and videos as their groups, named after the best copy; a large file is a card alone. */
    private fun items(state: DuplicatesUiState, section: DuplicatesSection): List<DuplicateGroupAdapter.Item> =
        if (section.hasGroups) {
            state.groups(section).map { group ->
                DuplicateGroupAdapter.Item(
                    key = group.hash,
                    title = group.best.name,
                    files = group.files,
                    selected = group.files.map { it.path }.filter { it in state.selectedPaths }.toSet(),
                )
            }
        } else {
            state.largeFiles.map { file ->
                DuplicateGroupAdapter.Item(
                    key = file.path,
                    title = file.name,
                    files = listOf(file),
                    selected = if (file.path in state.selectedPaths) setOf(file.path) else emptySet(),
                )
            }
        }

    private fun title(section: DuplicatesSection): Int = when (section) {
        DuplicatesSection.IMAGES -> R.string.duplicates_title_images
        DuplicatesSection.VIDEOS -> R.string.duplicates_title_videos
        DuplicatesSection.LARGE_FILES -> R.string.duplicates_title_large_files
    }

    companion object {
        private const val ARG_SECTION = "section"
        private const val DISABLED_ALPHA = 0.5f

        fun newInstance(section: DuplicatesSection) = DuplicatesDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_SECTION, section.name) }
        }
    }
}
