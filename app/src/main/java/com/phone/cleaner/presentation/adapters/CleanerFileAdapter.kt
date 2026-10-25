package com.phone.cleaner.presentation.adapters

import ai.jhw.storage.model.FileItem
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ItemCleanerFileRowBinding
import com.phone.cleaner.databinding.ItemCleanerListHeaderBinding
import com.phone.cleaner.databinding.ItemCleanerListNoteBinding
import com.phone.cleaner.utils.clipToRoundRect
import com.phone.cleaner.utils.loadFilePreview
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.visibleIf

/**
 * A section's files as the design's list (Figma 1:1160), each row with its check. The Junk Files
 * list adds a heading per category and a muted line for a category that could not be scanned.
 */
class CleanerFileAdapter : ListAdapter<CleanerFileAdapter.Item, RecyclerView.ViewHolder>(DIFF) {

    sealed interface Item {
        val key: String

        data class Header(val title: String) : Item {
            override val key: String get() = "header:$title"
        }

        data class Note(val text: String) : Item {
            override val key: String get() = "note:$text"
        }

        /** [detail] is the second line, or null for none (the design's list has none). */
        data class File(val file: FileItem, val isSelected: Boolean, val detail: String?) : Item {
            override val key: String get() = file.path
        }
    }

    var onFileClick: ((FileItem) -> Unit)? = null

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is Item.Header -> TYPE_HEADER
        is Item.Note -> TYPE_NOTE
        is Item.File -> TYPE_FILE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemCleanerListHeaderBinding.inflate(inflater, parent, false))
            TYPE_NOTE -> NoteViewHolder(ItemCleanerListNoteBinding.inflate(inflater, parent, false))
            else -> FileViewHolder(ItemCleanerFileRowBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Item.Header -> (holder as HeaderViewHolder).binding.listHeader.text = item.title
            is Item.Note -> (holder as NoteViewHolder).binding.listNote.text = item.text
            is Item.File -> (holder as FileViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(val binding: ItemCleanerListHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    class NoteViewHolder(val binding: ItemCleanerListNoteBinding) : RecyclerView.ViewHolder(binding.root)

    inner class FileViewHolder(
        private val binding: ItemCleanerFileRowBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.fileIcon.clipToRoundRect(binding.root.resources.getDimension(com.intuit.sdp.R.dimen._4sdp))
        }

        fun bind(item: Item.File) = with(binding) {
            val context = root.context
            val file = item.file
            if (fileIcon.tag != file.path) {
                fileIcon.tag = file.path
                fileIcon.loadFilePreview(file, inset = 0)
            }
            fileName.text = file.name
            fileDetail.text = item.detail
            fileDetail.visibleIf(item.detail != null)
            fileCheck.setBackgroundResource(if (item.isSelected) R.drawable.shape_check_on else R.drawable.shape_check_off)
            fileCheckTick.visibleIf(item.isSelected)

            root.contentDescription = listOfNotNull(file.name, item.detail).joinToString(", ")
            ViewCompat.setStateDescription(
                root,
                context.getString(if (item.isSelected) R.string.state_selected else R.string.state_not_selected),
            )
            root.onClick(intervalMs = 0) { onFileClick?.invoke(file) }
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_NOTE = 1
        const val TYPE_FILE = 2

        val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.key == newItem.key
            override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
        }
    }
}
