package com.phone.cleaner.presentation.components

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ItemCleanerSectionBinding
import com.phone.cleaner.domain.entities.CleanerSection
import com.phone.cleaner.domain.view_models.CleanerUiState
import com.phone.cleaner.utils.clipToRoundRect
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.loadFilePreview
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.visibleIf

/**
 * The section cards (Figma 1:576) of a cleaner's results, and the folder groups on a section's page
 * (1:75), kept in step with the state inside [container]. A card is kept while its section is,
 * so a selection change does not reload its previews.
 */
class SectionCards(
    private val container: LinearLayout,
    private val onCheck: (key: String) -> Unit,
    private val onOpen: (key: String) -> Unit,
    private val onAllowAccess: () -> Unit,
) {
    private val cards = LinkedHashMap<String, ItemCleanerSectionBinding>()

    fun bind(sections: List<CleanerSection>, state: CleanerUiState) {
        val keys = sections.map { it.key }.toSet()
        cards.keys.filter { it !in keys }.forEach { key -> cards.remove(key)?.let { container.removeView(it.root) } }
        sections.forEachIndexed { index, section ->
            val card = cards.getOrPut(section.key) { create(section.key) }
            if (container.getChildAt(index) !== card.root) {
                (card.root.parent as? ViewGroup)?.removeView(card.root)
                container.addView(card.root, index)
            }
            card.bind(section, state)
        }
    }

    private fun create(key: String): ItemCleanerSectionBinding {
        val card = ItemCleanerSectionBinding.inflate(LayoutInflater.from(container.context), container, false)
        val radius = container.resources.getDimension(com.intuit.sdp.R.dimen._15sdp)
        card.previews().forEach { it.clipToRoundRect(radius) }
        card.sectionCheck.onClick(intervalMs = 0) { onCheck(key) }
        card.sectionCheck.expandTouchTarget()
        return card
    }

    private fun ItemCleanerSectionBinding.bind(section: CleanerSection, state: CleanerUiState) {
        val context = root.context
        val needsAccess = section.needsAccess

        sectionTitle.text = if (needsAccess) {
            context.getString(section.type.pageTitle)
        } else {
            context.getString(section.type.countTitle, context.formatNumber(section.files.size))
        }
        sectionCheck.visibleIf(!needsAccess)
        // Without its check, the title starts where the check would.
        sectionTitle.updateLayoutParams<LinearLayout.LayoutParams> {
            marginStart = if (needsAccess) 0 else marginEnd
        }
        sectionMessage.visibleIf(needsAccess)
        sectionPreviewsScroll.visibleIf(!needsAccess)
        sectionViewAll.setText(if (needsAccess) R.string.permission_allow else R.string.cleaner_view_all)
        val open = { if (needsAccess) onAllowAccess() else onOpen(section.key) }
        sectionViewAll.onClick { open() }
        root.onClick { open() }

        if (needsAccess) {
            sectionMessage.setText(R.string.junk_access_message)
            return
        }

        val included = state.isIncluded(section.key)
        sectionCheck.setBackgroundResource(if (included) R.drawable.shape_section_check_on else R.drawable.shape_check_off)
        sectionCheckTick.visibleIf(included)
        sectionCheck.contentDescription = sectionTitle.text
        ViewCompat.setStateDescription(
            sectionCheck,
            context.getString(if (included) R.string.state_selected else R.string.state_not_selected),
        )
        previews().forEachIndexed { index, preview ->
            val file = section.files.getOrNull(index)
            preview.visibleIf(file != null)
            if (file != null && preview.tag != file.path) {
                preview.tag = file.path
                preview.loadFilePreview(file)
            }
        }
    }

    private fun ItemCleanerSectionBinding.previews() =
        listOf(preview1.root, preview2.root, preview3.root, preview4.root)
}
