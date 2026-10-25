package com.phone.cleaner.presentation.adapters

import ai.jhw.storage.model.FileItem
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ItemCleanerGridFileBinding
import com.phone.cleaner.utils.clipToRoundRect
import com.phone.cleaner.utils.loadFilePreview
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.toReadableSize
import com.phone.cleaner.utils.visibleIf

/** A section's photos and videos as the design's grid (Figma 1:142), each with its own check. */
class CleanerGridAdapter : ListAdapter<CleanerGridAdapter.Item, CleanerGridAdapter.TileViewHolder>(DIFF) {

    data class Item(val file: FileItem, val isSelected: Boolean)

    var onFileClick: ((FileItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val binding = ItemCleanerGridFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.fileThumb.clipToRoundRect(parent.resources.getDimension(com.intuit.sdp.R.dimen._8sdp))
        return TileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) = holder.bind(getItem(position))

    inner class TileViewHolder(
        private val binding: ItemCleanerGridFileBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) = with(binding) {
            val context = root.context
            val file = item.file
            if (fileThumb.tag != file.path) {
                fileThumb.tag = file.path
                fileThumb.loadFilePreview(file)
            }
            fileCheck.visibleIf(item.isSelected)

            // TalkBack: the file's name and size, read as a checkbox that is on or off.
            root.contentDescription = context.getString(
                R.string.cleaner_file_description,
                file.name,
                file.sizeBytes.toReadableSize(context),
            )
            ViewCompat.setStateDescription(
                root,
                context.getString(if (item.isSelected) R.string.state_selected else R.string.state_not_selected),
            )
            root.onClick(intervalMs = 0) { onFileClick?.invoke(file) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.file.path == newItem.file.path
            override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
        }
    }
}
