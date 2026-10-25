package com.phone.cleaner.presentation.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.phone.cleaner.R
import com.phone.cleaner.databinding.FragmentCleanerSectionsBinding
import com.phone.cleaner.domain.view_models.CleanerUiState
import com.phone.cleaner.presentation.base.BaseFragment
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
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
        deleteContainer.applyNavigationBarBottomMargin()
        header.backButton.root.onClick { parentFragmentManager.popBackStack() }
        header.backButton.root.expandTouchTarget()
        cards = SectionCards(
            container = groups,
            onCheck = { host.cleaner.onSectionCheckClicked(it) },
            onOpen = { host.openSection(it) },
            onAllowAccess = { host.requestAllFilesAccess() },
        )
        deleteButton.root.onClick { host.cleaner.onDeleteClicked(key) }
    }

    override fun observeState() {
        host.cleaner.state.collectState(::render)
    }

    override fun onDestroyView() {
        cards = null
        super.onDestroyView()
    }

    private fun render(state: CleanerUiState) = with(binding) {
        val section = state.section(key)
        if (section == null) {
            // Everything in it was deleted: back to the results, once.
            if (!isLeaving) parentFragmentManager.popBackStack()
            isLeaving = true
            return
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
