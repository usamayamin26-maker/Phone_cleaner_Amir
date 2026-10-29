package com.phone.cleaner.presentation.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.FragmentCleanerSectionsBinding
import com.phone.cleaner.domain.view_models.CleanerUiState
import com.phone.cleaner.presentation.base.BaseFragment
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyNavigationBarBottomMarginAboveSlot
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.toReadableSize

/**
 * A section's page made of folder groups (Figma 1:48, the AI cleaner's Images): one card per group,
 * each with its own check and "View All", and "Delete" for the section's selection.
 */
class CleanerSectionsFragment : BaseFragment<FragmentCleanerSectionsBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCleanerSectionsBinding =
        FragmentCleanerSectionsBinding::inflate

    private val host: CleanerHost get() = requireActivity() as CleanerHost
    private val key: String get() = requireArguments().getString(ARG_KEY).orEmpty()

    private var cards: SectionCards? = null
    private var isLeaving = false

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        // The slot is the bottom-most view of the page, so it clears the gesture bar; Delete pays
        // only while the slot is gone, through its own gone margin.
        adSlotBottom.applyNavigationBarBottomMargin()
        deleteContainer.applyNavigationBarBottomMarginAboveSlot()
        header.backButton.root.onClick { leavePage() }
        header.backButton.root.expandTouchTarget()
        AdsController.routeSystemBack(this@CleanerSectionsFragment, header.backButton.root)
        cards = SectionCards(
            container = groups,
            onCheck = { host.cleaner.onSectionCheckClicked(it) },
            // A group's own page is another inner screen, so it takes the same page entry ad.
            onOpen = { sectionKey -> withEntryAd(host.screen.adsPageEntryFlag) { host.openSection(sectionKey) } },
            onAllowAccess = { host.requestAllFilesAccess() },
        )
        // No ad on Delete: it opens the design's "Are You Sure?".
        deleteButton.root.onClick { host.cleaner.onDeleteClicked(key) }
    }

    override fun observeState() {
        host.cleaner.state.collectState(::render)
    }

    override fun onResume() {
        super.onResume()
        val screen = host.screen
        AdsController.slot(requireActivity(), binding.adSlotTop, "${screen.adsKey}_groups_top", screen.adsUnits)
        AdsController.slot(requireActivity(), binding.adSlotBottom, "${screen.adsKey}_groups_bottom", screen.adsUnits)
    }

    override fun onDestroyView() {
        cards = null
        AdsController.forgetSlots("${host.screen.adsKey}_groups")
        super.onDestroyView()
    }

    /**
     * Back to the results, behind the page back interstitial. [isLeaving] guards the automatic pop
     * (everything in the section was deleted), which must not wait behind an ad.
     */
    private fun leavePage() {
        if (isLeaving) return
        leaveWithAd(host.screen.adsPageBackFlag) { parentFragmentManager.popBackStack() }
    }

    private fun render(state: CleanerUiState): Unit = with(binding) {
        val section = state.section(key)
        if (section == null) {
            // Everything in it was deleted: back to the results, once.
            if (!isLeaving) parentFragmentManager.popBackStack()
            isLeaving = true
            return@with
        }
        val context = requireContext()
        header.headerTitle.setText(section.type.pageTitle)
        cards?.bind(section.groups, state)

        val selected = state.selected(key)
        deleteButton.root.text = getString(
            R.string.cleaner_delete_button,
            context.formatNumber(selected.size),
            selected.sumOf { it.sizeBytes }.toReadableSize(context),
        )
        deleteButton.root.isEnabled = selected.isNotEmpty() && !state.isDeleting
        deleteButton.root.alpha = if (deleteButton.root.isEnabled) 1f else DISABLED_ALPHA
    }

    companion object {
        private const val ARG_KEY = "section_key"
        private const val DISABLED_ALPHA = 0.5f

        fun newInstance(key: String) = CleanerSectionsFragment().apply {
            arguments = Bundle().apply { putString(ARG_KEY, key) }
        }
    }
}
