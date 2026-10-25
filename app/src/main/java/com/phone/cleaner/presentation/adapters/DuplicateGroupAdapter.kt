package com.phone.cleaner.presentation.adapters

import ai.jhw.storage.model.FileItem
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ItemDuplicateFileBinding
import com.phone.cleaner.databinding.ItemDuplicateGroupBinding
import com.phone.cleaner.utils.clipToRoundRect
import com.phone.cleaner.utils.loadFilePreview
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.toReadableSize
import com.phone.cleaner.utils.visibleIf

/**
 * A section's groups (Figma 1:702): each card names the file and shows its copies, each with its
 * size and check. A large file is a card of its own with one copy.
 */
class DuplicateGroupAdapter : ListAdapter<DuplicateGroupAdapter.Item, DuplicateGroupAdapter.GroupViewHolder>(DIFF) {

    /** One card. [selected] holds the paths of this card's files that are selected. */
    data class Item(
        val key: String,
        val title: String,
        val files: List<FileItem>,
        val selected: Set<String>,
    )

    var onFileClick: ((FileItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder =
        GroupViewHolder(ItemDuplicateGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) = holder.bind(getItem(position))

    inner class GroupViewHolder(
        private val binding: ItemDuplicateGroupBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        /** Tiles are kept and reused across binds, so a selection change does not reload images. */
        private val tiles = mutableListOf<ItemDuplicateFileBinding>()

        fun bind(item: Item) = with(binding) {
            groupTitle.text = item.title
            val inflater = LayoutInflater.from(root.context)
            while (tiles.size < item.files.size) {
                val tile = ItemDuplicateFileBinding.inflate(inflater, groupFiles, false)
                tile.fileThumb.clipToRoundRect(root.resources.getDimension(com.intuit.sdp.R.dimen._12sdp))
                tiles += tile
            }
            groupFiles.removeAllViews()
            item.files.forEachIndexed { index, file ->
                val tile = tiles[index]
                groupFiles.addView(tile.root)
                bindTile(tile, file, file.path in item.selected)
            }
        }

        private fun bindTile(tile: ItemDuplicateFileBinding, file: FileItem, isSelected: Boolean) = with(tile) {
            val context = root.context
            if (fileThumb.tag != file.path) {
                fileThumb.tag = file.path
                fileThumb.loadFilePreview(file)
            }
            fileSize.text = file.sizeBytes.toReadableSize(context)
            fileCheck.setBackgroundResource(if (isSelected) R.drawable.shape_check_on else R.drawable.shape_check_off)
            fileCheckTick.visibleIf(isSelected)

            // TalkBack: the copy's name and size, read as a checkbox that is on or off.
            root.contentDescription = context.getString(R.string.duplicates_file_description, file.name, fileSize.text)
            ViewCompat.setStateDescription(
                root,
                context.getString(if (isSelected) R.string.state_selected else R.string.state_not_selected),
            )
            root.onClick(intervalMs = 0) { onFileClick?.invoke(file) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.key == newItem.key
            override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
        }
    }
}
