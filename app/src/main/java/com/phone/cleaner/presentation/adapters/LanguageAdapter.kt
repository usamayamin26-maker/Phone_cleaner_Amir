package com.phone.cleaner.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phone.cleaner.databinding.ItemLanguageBinding
import com.phone.cleaner.domain.entities.AppLanguage
import com.phone.cleaner.utils.onClick

/** The eighteen rows of the Language screen; the chosen one carries the tick. */
class LanguageAdapter : ListAdapter<AppLanguage, LanguageAdapter.LanguageViewHolder>(DIFF) {

    var selectedTag: String = ""
        set(value) {
            if (field == value) return
            val previous = field
            field = value
            notifyItemChangedForTag(previous)
            notifyItemChangedForTag(value)
        }

    var onLanguageClick: ((AppLanguage) -> Unit)? = null

    private fun notifyItemChangedForTag(tag: String) {
        val index = currentList.indexOfFirst { it.tag == tag }
        if (index != RecyclerView.NO_POSITION) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder =
        LanguageViewHolder(ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) = holder.bind(getItem(position))

    inner class LanguageViewHolder(
        private val binding: ItemLanguageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(language: AppLanguage) = with(binding) {
            val context = root.context
            val isSelected = language.tag == selectedTag
            val nativeName = context.getString(language.nativeName)
            val englishName = context.getString(language.englishName)

            languageFlag.text = language.flag
            languageNative.text = nativeName
            languageEnglish.text = englishName
            // English in English would say the same thing twice.
            languageEnglish.visibility = if (englishName == nativeName) View.GONE else View.VISIBLE
            // INVISIBLE, not GONE: the names keep the same width on every row.
            languageTick.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            // TalkBack reads the card's names as one item and announces the chosen one as selected.
            root.isSelected = isSelected
            root.onClick { onLanguageClick?.invoke(language) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppLanguage>() {
            override fun areItemsTheSame(oldItem: AppLanguage, newItem: AppLanguage) =
                oldItem.tag == newItem.tag

            override fun areContentsTheSame(oldItem: AppLanguage, newItem: AppLanguage) =
                oldItem == newItem
        }
    }
}
