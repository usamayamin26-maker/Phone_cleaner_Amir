package com.phone.cleaner.presentation.components

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import com.phone.cleaner.R
import com.phone.cleaner.databinding.FragmentCleanerResultsBinding
import com.phone.cleaner.databinding.ViewSizeChipBinding
import com.phone.cleaner.domain.entities.CleanerSectionType
import com.phone.cleaner.domain.view_models.CleanerUiState
import com.phone.cleaner.presentation.base.BaseFragment
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.gone
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.scaleToWidthFromTop
import com.phone.cleaner.utils.toReadableSize
import com.phone.cleaner.utils.visibleIf

/**
 * Results (Figma 1:528 / 1:349 / 1:1042), a page every cleaner screen shares: the robot card with
 * its chips, a card per section and "Delete" for everything selected across them. A section's
 * check takes it in or out of Delete; "View All" opens its page.
 */
class CleanerResultsFragment : BaseFragment<FragmentCleanerResultsBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCleanerResultsBinding =
        FragmentCleanerResultsBinding::inflate

    private val host: CleanerHost get() = requireActivity() as CleanerHost

    private var cards: SectionCards? = null

    override fun setupViews() = with(binding) {
        val screen = host.screen
        header.root.applyStatusBarTopMargin()
        resultsContent.applyNavigationBarBottomMargin()
        header.headerTitle.setText(screen.title)
        header.backButton.root.onClick { requireActivity().finish() }
        header.backButton.root.expandTouchTarget()

        robot.setImageResource(screen.robot)
        robot.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = resources.getDimensionPixelSize(screen.robotWidth)
            marginStart = screen.robotMarginStart?.let(resources::getDimensionPixelSize) ?: 0
        }
        robot.scaleToWidthFromTop(screen.robotOffsetTop?.let(resources::getDimensionPixelSize) ?: 0)
        keepRobotClearOfChips()
        when (screen.chips) {
            CleanerScreen.Chips.SIZES -> {
                chipImages.chipIcon.setImageResource(R.drawable.ic_image)
                chipVideos.chipIcon.setImageResource(R.drawable.ic_video)
                chipFiles.chipIcon.setImageResource(R.drawable.ic_file)
            }
            CleanerScreen.Chips.TOTAL_AND_STATUS -> {
                listOf(chipImages, chipVideos).forEach { it.asTextChip() }
                chipVideos.chipSize.isAllCaps = false
                chipVideos.chipSize.setText(R.string.wa_scan_completed)
                chipFiles.root.gone()
            }
        }
        resultsEmpty.setText(screen.emptyMessage)

        cards = SectionCards(
            container = resultsSections,
            onCheck = { host.cleaner.onSectionCheckClicked(it) },
            onOpen = { host.openSection(it) },
        )
        deleteButton.onClick { host.cleaner.onDeleteClicked(null) }
    }

    override fun observeState() {
        host.cleaner.state.collectState(::render)
    }

    override fun onDestroyView() {
        cards = null
        super.onDestroyView()
    }

    private fun render(state: CleanerUiState) = with(binding) {
        val context = requireContext()
        when (host.screen.chips) {
            CleanerScreen.Chips.SIZES -> {
                chipImages.setSize(state.section(CleanerSectionType.IMAGES.name)?.reclaimableBytes ?: 0)
                chipVideos.setSize(state.section(CleanerSectionType.VIDEOS.name)?.reclaimableBytes ?: 0)
                chipFiles.setSize(state.section(CleanerSectionType.LARGE_FILES.name)?.reclaimableBytes ?: 0)
            }
            // A file in two sections (a large video) counts once.
            CleanerScreen.Chips.TOTAL_AND_STATUS ->
                chipImages.setSize(state.sections.flatMap { it.files }.distinctBy { it.path }.sumOf { it.sizeBytes })
        }

        cards?.bind(state.sections, state)

        resultsEmpty.visibleIf(state.isEmpty)
        val selected = state.selected()
        deleteButton.visibleIf(!state.isEmpty)
        deleteButton.text = getString(
            R.string.cleaner_delete_button,
            context.formatNumber(selected.size),
            selected.sumOf { it.sizeBytes }.toReadableSize(context),
        )
        deleteButton.isEnabled = selected.isNotEmpty() && !state.isDeleting
        deleteButton.alpha = if (deleteButton.isEnabled) 1f else DISABLED_ALPHA
    }

    /**
     * The chips grow with the font size; at a large one they would run over the robot. The robot
     * then gives way: it is narrowed to the room left beside the chips, keeping the design's gap.
     */
    private fun keepRobotClearOfChips() = with(binding) {
        val designWidth = resources.getDimensionPixelSize(host.screen.robotWidth)
        val gap = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
        chips.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val card = view.parent as? ViewGroup ?: return@addOnLayoutChangeListener
            val robotStart = robot.marginStart
            val chipsStart = if (card.layoutDirection == View.LAYOUT_DIRECTION_RTL) card.width - view.right else view.left
            val width = minOf(designWidth, chipsStart - robotStart - gap).coerceAtLeast(0)
            if (robot.layoutParams.width != width) {
                robot.updateLayoutParams<ConstraintLayout.LayoutParams> { this.width = width }
            }
        }
    }

    private fun ViewSizeChipBinding.setSize(bytes: Long) {
        chipSize.text = bytes.toReadableSize(requireContext())
    }

    /** WA Cleaner's chips (Figma 1:1065): the same pill with its text centred and no icon. */
    private fun ViewSizeChipBinding.asTextChip() {
        chipIconFrame.gone()
        root.gravity = Gravity.CENTER
        chipSize.updateLayoutParams<ViewGroup.MarginLayoutParams> { marginStart = 0 }
    }

    private companion object {
        const val DISABLED_ALPHA = 0.5f
    }
}
