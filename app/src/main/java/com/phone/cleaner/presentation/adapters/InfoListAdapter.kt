package com.phone.cleaner.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phone.cleaner.databinding.ItemInfoHeadingBinding
import com.phone.cleaner.databinding.ItemInfoRowBinding
import com.phone.cleaner.domain.entities.InfoListItem

/** The label/value cards of a Device Info detail screen, with a heading per camera. */
class InfoListAdapter : ListAdapter<InfoListItem, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is InfoListItem.Heading -> TYPE_HEADING
        is InfoListItem.Row -> TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADING) {
            HeadingHolder(ItemInfoHeadingBinding.inflate(inflater, parent, false))
        } else {
            RowHolder(ItemInfoRowBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is InfoListItem.Heading -> (holder as HeadingHolder).bind(item)
            is InfoListItem.Row -> (holder as RowHolder).bind(item)
        }
    }

    class HeadingHolder(private val binding: ItemInfoHeadingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InfoListItem.Heading) {
            binding.headingTitle.text = item.title
            ViewCompat.setAccessibilityHeading(binding.headingTitle, true)
        }
    }

    class RowHolder(private val binding: ItemInfoRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InfoListItem.Row) {
            binding.rowLabel.text = item.label
            binding.rowValue.text = item.value
        }
    }

    private object Diff : DiffUtil.ItemCallback<InfoListItem>() {
        override fun areItemsTheSame(oldItem: InfoListItem, newItem: InfoListItem): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: InfoListItem, newItem: InfoListItem): Boolean =
            oldItem == newItem
    }

    private companion object {
        const val TYPE_HEADING = 0
        const val TYPE_ROW = 1
    }
}
