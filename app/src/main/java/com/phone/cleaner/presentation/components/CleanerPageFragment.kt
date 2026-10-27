package com.phone.cleaner.presentation.components

import ai.jhw.junkcleaner.model.JunkCategory
import ai.jhw.junkcleaner.model.JunkItem
import ai.jhw.junkcleaner.model.JunkType
import ai.jhw.junkcleaner.model.UnavailableReason
import ai.jhw.storage.model.FileItem
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.phone.cleaner.R
import com.phone.cleaner.databinding.FragmentCleanerPageBinding
import com.phone.cleaner.databinding.ViewSelectAllBinding
import com.phone.cleaner.domain.entities.CleanerSection
import com.phone.cleaner.domain.entities.CleanerSectionType
import com.phone.cleaner.domain.view_models.CleanerUiState
import com.phone.cleaner.presentation.adapters.CleanerFileAdapter
import com.phone.cleaner.presentation.adapters.CleanerGridAdapter
import com.phone.cleaner.presentation.base.BaseFragment
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.toReadableSize
import com.phone.cleaner.utils.visibleIf
import org.koin.android.ext.android.inject

/**
 * A section's own page with "Select All" and "Delete": photos and videos as the design's grid
 * (Figma 1:142 / 1:208 / 1:1526), every other kind of file as its list (1:1160 / 1:1250). The
 * Junk Files list (no design) is the same list, headed by category.
 */
class CleanerPageFragment : BaseFragment<FragmentCleanerPageBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCleanerPageBinding =
        FragmentCleanerPageBinding::inflate

    private val host: CleanerHost get() = requireActivity() as CleanerHost
    private val gridAdapter: CleanerGridAdapter by inject()
    private val fileAdapter: CleanerFileAdapter by inject()

    private val key: String get() = requireArguments().getString(ARG_KEY).orEmpty()
    private val isGrid: Boolean get() = requireArguments().getBoolean(ARG_GRID)

    private var selectAll: ViewSelectAllBinding? = null
    private var isLeaving = false

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        deleteContainer.applyNavigationBarBottomMargin()
        header.backButton.root.onClick { parentFragmentManager.popBackStack() }
        header.backButton.root.expandTouchTarget()

        selectAll = ViewSelectAllBinding.inflate(layoutInflater, header.headerTrailing, true).also {
            it.root.onClick(intervalMs = 0) { host.cleaner.onSelectAllClicked(key) }
        }

        if (isGrid) {
            // Figma 1:240: four to a row, 4 px apart, 12 px between rows, 23 px from the edges.
            val side = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._18sdp)
            items.setPaddingRelative(side, items.paddingTop, side, items.paddingBottom)
            items.layoutManager = GridLayoutManager(requireContext(), SPAN_COUNT)
            items.addItemDecoration(
                GridSpacingDecoration(
                    spanCount = SPAN_COUNT,
                    spacingPx = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._3sdp),
                    rowSpacingPx = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                ),
            )
            items.adapter = gridAdapter
            gridAdapter.onFileClick = { host.cleaner.onFileClicked(key, it) }
        } else {
            // Figma 1:1189: a 20 px gutter, rows 12 px apart.
            val side = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp)
            items.setPaddingRelative(side, items.paddingTop, side, items.paddingBottom)
            items.layoutManager = LinearLayoutManager(requireContext())
            items.adapter = fileAdapter
            fileAdapter.onFileClick = { host.cleaner.onFileClicked(key, it) }
        }
        deleteButton.root.onClick { host.cleaner.onDeleteClicked(key) }
    }

    override fun observeState() {
        host.cleaner.state.collectState(::render)
    }

    override fun onDestroyView() {
        selectAll = null
        super.onDestroyView()
    }

    private fun render(state: CleanerUiState): Unit = with(binding) {
        val section = state.section(key)
        if (section == null || section.files.isEmpty()) {
            // Everything in it was deleted: back to where it was opened from, once.
            if (!isLeaving) parentFragmentManager.popBackStack()
            isLeaving = true
            return@with
        }
        val context = requireContext()
        header.headerTitle.setText(section.type.pageTitle)

        val allSelected = state.isAllSelected(key)
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

        if (isGrid) {
            gridAdapter.submitList(section.files.map { CleanerGridAdapter.Item(it, it.path in state.selectedPaths) })
        } else {
            fileAdapter.submitList(listItems(context, section, state))
        }

        val selected = state.selected(key)
        deleteButton.root.text = getString(
            R.string.cleaner_delete_button,
            context.formatNumber(selected.size),
            selected.sumOf { it.sizeBytes }.toReadableSize(context),
        )
        deleteButton.root.isEnabled = selected.isNotEmpty() && !state.isDeleting
        deleteButton.root.alpha = if (deleteButton.root.isEnabled) 1f else DISABLED_ALPHA
    }

    private fun listItems(context: Context, section: CleanerSection, state: CleanerUiState): List<CleanerFileAdapter.Item> {
        fun row(file: FileItem, detail: String?) =
            CleanerFileAdapter.Item.File(file, file.path in state.selectedPaths, detail)

        if (section.type != CleanerSectionType.JUNK_FILES) {
            // The design's "Files" list (1:1160) has no second line; the lists with no design show the size.
            val showSize = section.type != CleanerSectionType.DOCUMENTS
            return section.files.map { row(it, if (showSize) it.sizeBytes.toReadableSize(context) else null) }
        }

        // Junk: each category that has items under its heading; one that could not be scanned as
        // a muted line with the reason. All files access missing is the results card's job.
        val files = section.files.associateBy { it.path }
        return state.junkCategories.flatMap<_, CleanerFileAdapter.Item> { category ->
            val reason = category.unavailableReason
            when {
                reason != null && reason != UnavailableReason.NEEDS_FULL_ACCESS ->
                    listOf(CleanerFileAdapter.Item.Note(getString(R.string.junk_unavailable, getString(category.type.label), getString(reason.label))))
                category.items.isEmpty() -> emptyList()
                else -> buildList<CleanerFileAdapter.Item> {
                    add(CleanerFileAdapter.Item.Header(category.header(context)))
                    category.items.forEach { item -> files[item.path]?.let { add(row(it, item.detail(context))) } }
                }
            }
        }
    }

    private fun JunkCategory.header(context: Context): String =
        getString(R.string.junk_name_size, getString(type.label), totalBytes.toReadableSize(context))

    private fun JunkItem.detail(context: Context): String = when (type) {
        JunkType.EMPTY_FOLDERS -> getString(R.string.junk_empty_folder)
        JunkType.RESIDUAL_FILES, JunkType.APP_CACHE ->
            packageName?.let { getString(R.string.junk_name_size, it, sizeBytes.toReadableSize(context)) }
                ?: sizeBytes.toReadableSize(context)
        else -> sizeBytes.toReadableSize(context)
    }

    private val JunkType.label: Int
        get() = when (this) {
            JunkType.APK_FILES -> R.string.junk_apk_files
            JunkType.TEMP_FILES -> R.string.junk_temp_files
            JunkType.THUMBNAILS -> R.string.junk_thumbnails
            JunkType.EMPTY_FOLDERS -> R.string.junk_empty_folders
            JunkType.RESIDUAL_FILES -> R.string.junk_residual_files
            JunkType.APP_CACHE -> R.string.junk_app_cache
        }

    private val UnavailableReason.label: Int
        get() = when (this) {
            UnavailableReason.NEEDS_FULL_ACCESS -> R.string.junk_reason_full_access
            UnavailableReason.BLOCKED_BY_ANDROID -> R.string.junk_reason_blocked
            UnavailableReason.NEEDS_PACKAGE_VISIBILITY -> R.string.junk_reason_package_visibility
        }

    companion object {
        private const val ARG_KEY = "section_key"
        private const val ARG_GRID = "grid"
        private const val SPAN_COUNT = 4
        private const val DISABLED_ALPHA = 0.5f

        fun newInstance(key: String, grid: Boolean) = CleanerPageFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_KEY, key)
                putBoolean(ARG_GRID, grid)
            }
        }
    }
}
