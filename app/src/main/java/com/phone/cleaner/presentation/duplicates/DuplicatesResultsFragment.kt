package com.phone.cleaner.presentation.duplicates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import com.phone.cleaner.R
import com.phone.cleaner.databinding.FragmentDuplicatesResultsBinding
import com.phone.cleaner.databinding.ItemDuplicatesSectionBinding
import com.phone.cleaner.databinding.ViewSizeChipBinding
import com.phone.cleaner.domain.entities.DuplicatesSection
import com.phone.cleaner.domain.view_models.DuplicatesUiState
import com.phone.cleaner.domain.view_models.DuplicatesViewModel
import com.phone.cleaner.presentation.base.BaseFragment
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.clipToRoundRect
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.loadFilePreview
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.scaleToWidthFromTop
import com.phone.cleaner.utils.toReadableSize
import com.phone.cleaner.utils.visibleIf
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Results (Figma 1:528): what each section can free, a preview of each section and "Delete" for
 * everything selected across them. A section's check selects or clears all of it; "View All" opens
 * its page.
 */
class DuplicatesResultsFragment : BaseFragment<FragmentDuplicatesResultsBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentDuplicatesResultsBinding =
        FragmentDuplicatesResultsBinding::inflate

    private val viewModel: DuplicatesViewModel by activityViewModel()

    /** Read from the current binding every time: the view is rebuilt when the page comes back. */
    private fun sections() = mapOf(
        DuplicatesSection.IMAGES to binding.sectionImages,
        DuplicatesSection.VIDEOS to binding.sectionVideos,
        DuplicatesSection.LARGE_FILES to binding.sectionLargeFiles,
    )

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        resultsContent.applyNavigationBarBottomMargin()
        header.headerTitle.setText(R.string.action_delete_duplicates)
        header.backButton.root.onClick { requireActivity().finish() }
        header.backButton.root.expandTouchTarget()

        robot.scaleToWidthFromTop()
        chipImages.chipIcon.setImageResource(R.drawable.ic_image)
        chipVideos.chipIcon.setImageResource(R.drawable.ic_video)
        chipFiles.chipIcon.setImageResource(R.drawable.ic_file)

        val radius = resources.getDimension(com.intuit.sdp.R.dimen._15sdp)
        sections().forEach { (section, card) ->
            card.previews().forEach { it.clipToRoundRect(radius) }
            card.sectionCheck.onClick(intervalMs = 0) { viewModel.onSectionCheckClicked(section) }
            card.sectionCheck.expandTouchTarget()
            card.sectionViewAll.onClick { (requireActivity() as DuplicatesActivity).openSection(section) }
            card.root.onClick { (requireActivity() as DuplicatesActivity).openSection(section) }
        }
        deleteButton.onClick { viewModel.onDeleteClicked(null) }
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
    }

    private fun render(state: DuplicatesUiState) = with(binding) {
        val context = requireContext()
        chipImages.setSize(state.reclaimableBytes(DuplicatesSection.IMAGES))
        chipVideos.setSize(state.reclaimableBytes(DuplicatesSection.VIDEOS))
        chipFiles.setSize(state.reclaimableBytes(DuplicatesSection.LARGE_FILES))

        sections().forEach { (section, card) -> card.bind(section, state) }

        resultsEmpty.visibleIf(state.isEmpty)
        val selected = state.selected()
        deleteButton.visibleIf(!state.isEmpty)
        deleteButton.text = getString(
            R.string.duplicates_delete_button,
            context.formatNumber(selected.size),
            selected.sumOf { it.sizeBytes }.toReadableSize(context),
        )
        deleteButton.isEnabled = selected.isNotEmpty() && !state.isDeleting
        deleteButton.alpha = if (deleteButton.isEnabled) 1f else DISABLED_ALPHA
    }

    private fun ItemDuplicatesSectionBinding.bind(section: DuplicatesSection, state: DuplicatesUiState) {
        val files = state.files(section)
        root.visibleIf(files.isNotEmpty())
        if (files.isEmpty()) return
        sectionTitle.text = getString(countTitle(section), requireContext().formatNumber(files.size))
        val included = state.isIncluded(section)
        sectionCheck.setBackgroundResource(
            if (included) R.drawable.shape_section_check_on else R.drawable.shape_check_off,
        )
        sectionCheckTick.visibleIf(included)
        sectionCheck.contentDescription = sectionTitle.text
        ViewCompat.setStateDescription(
            sectionCheck,
            getString(if (included) R.string.state_selected else R.string.state_not_selected),
        )
        previews().forEachIndexed { index, preview ->
            val file = files.getOrNull(index)
            preview.visibleIf(file != null)
            if (file != null && preview.tag != file.path) {
                preview.tag = file.path
                preview.loadFilePreview(file)
            }
        }
    }

    private fun ItemDuplicatesSectionBinding.previews() =
        listOf(preview1.root, preview2.root, preview3.root, preview4.root)

    private fun ViewSizeChipBinding.setSize(bytes: Long) {
        chipSize.text = bytes.toReadableSize(requireContext())
    }

    @StringRes
    private fun countTitle(section: DuplicatesSection): Int = when (section) {
        DuplicatesSection.IMAGES -> R.string.duplicates_images_count
        DuplicatesSection.VIDEOS -> R.string.duplicates_videos_count
        DuplicatesSection.LARGE_FILES -> R.string.duplicates_large_files_count
    }

    private companion object {
        const val DISABLED_ALPHA = 0.5f
    }
}
