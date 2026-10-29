package com.phone.cleaner.presentation.components

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.FragmentCleanerResultsBinding
import com.phone.cleaner.databinding.ViewSizeChipBinding
import com.phone.cleaner.domain.entities.CleanerSectionType
import com.phone.cleaner.domain.view_models.CleanerUiState
import com.phone.cleaner.presentation.base.BaseFragment
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyNavigationBarBottomMarginAboveSlot
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.gone
import com.phone.cleaner.utils.onClick
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

    /** Where the robot sits in the card's artwork; measured once. */
    private var robotArtBounds: Pair<Int, Int>? = null

    override fun setupViews() = with(binding) {
        val screen = host.screen
        header.root.applyStatusBarTopMargin()
        // The pinned slot is the bottom-most view of the page, so it clears the gesture bar. The
        // scrolling content keeps its own clearance for the launches where the slot is GONE.
        resultsContent.applyNavigationBarBottomMargin()
        adSlotBottom.applyNavigationBarBottomMargin()
        header.headerTitle.setText(screen.title)
        // Back out of the cleaner, behind its back interstitial. The scan is over on this page, so
        // nothing is in flight; it is also where system back lands, which does the same thing.
        header.backButton.root.onClick { leaveCleaner() }
        header.backButton.root.expandTouchTarget()
        AdsController.routeSystemBack(this@CleanerResultsFragment, header.backButton.root)

        robot.setImageResource(screen.robotCard)
        robotCard.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> placeRobotArt() }
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
            // A section's page is an inner screen, so it opens behind the cleaner's page entry ad.
            onOpen = { key -> withEntryAd(screen.adsPageEntryFlag) { host.openSection(key) } },
            onAllowAccess = { host.requestAllFilesAccess() },
        )
        // No ad on Delete: it opens the design's "Are You Sure?" and nothing of the app's goes
        // between the user and that dialog.
        deleteButton.onClick { host.cleaner.onDeleteClicked(null) }
    }

    override fun observeState() {
        host.cleaner.state.collectState(::render)
    }

    /** Bound from here, not once at creation, so a cached ad is re-attached when the page returns. */
    override fun onResume() {
        super.onResume()
        val screen = host.screen
        AdsController.slot(requireActivity(), binding.adSlotTop, "${screen.adsKey}_results_top", screen.adsUnits)
        AdsController.slot(requireActivity(), binding.adSlotMid, "${screen.adsKey}_results_mid", screen.adsUnits)
        AdsController.slot(requireActivity(), binding.adSlotBottom, "${screen.adsKey}_results_bottom", screen.adsUnits)
    }

    override fun onDestroyView() {
        cards = null
        AdsController.forgetSlots("${host.screen.adsKey}_results")
        super.onDestroyView()
    }

    /** Leaves the whole cleaner behind its back interstitial. */
    private fun leaveCleaner() = leaveWithAd(host.screen.adsBackFlag) { requireActivity().finish() }

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
     * Draws the card's artwork at the card's width, as the design does. Two cases move it:
     * - Right to left, the chips sit on the other side, so the artwork is shifted until its robot
     *   is at the mirrored place. It is moved, not flipped, so the robot and its logos read right.
     * - At a large font the chips grow towards the robot; the artwork then shrinks until the robot
     *   is clear of them again, standing on the card's bottom edge.
     */
    private fun placeRobotArt() = with(binding) {
        val drawable = robot.drawable as? BitmapDrawable ?: return@with
        val bitmap = drawable.bitmap
        if (robot.width == 0 || bitmap.width == 0 || drawable.intrinsicWidth <= 0) return@with
        // The matrix works on the drawable's own size, which differs from the bitmap's pixels when
        // Android scales the image to the screen's density; the robot's columns are converted.
        val toDrawable = drawable.intrinsicWidth.toFloat() / bitmap.width
        val (startColumn, endColumn) = robotArtBounds ?: bitmap.opaqueColumns().also { robotArtBounds = it }
        val artStart = startColumn * toDrawable
        val artEnd = endColumn * toDrawable
        val rtl = robotCard.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val chipsStart = if (rtl) robotCard.width - chips.right else chips.left
        val room = (chipsStart - resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)).toFloat()
        val scale = minOf(robot.width.toFloat() / drawable.intrinsicWidth, room / artEnd)
        val dx = if (rtl) robot.width - (artStart + artEnd) * scale else 0f
        val dy = robot.height - drawable.intrinsicHeight * scale
        robot.imageMatrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
    }

    /** The first and last columns of the artwork that hold the robot, in bitmap pixels. */
    private fun Bitmap.opaqueColumns(): Pair<Int, Int> {
        val column = IntArray(height)
        fun isOpaque(x: Int): Boolean {
            getPixels(column, 0, 1, x, 0, 1, height)
            return column.any { Color.alpha(it) > ALPHA_THRESHOLD }
        }
        val first = (0 until width).firstOrNull(::isOpaque) ?: 0
        val last = (width - 1 downTo 0).firstOrNull(::isOpaque) ?: (width - 1)
        return first to last + 1
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
        const val ALPHA_THRESHOLD = 8
    }
}
