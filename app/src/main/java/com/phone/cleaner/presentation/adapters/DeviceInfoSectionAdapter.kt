package com.phone.cleaner.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phone.cleaner.databinding.ItemDeviceInfoSectionBinding
import com.phone.cleaner.domain.entities.DeviceInfoSection
import com.phone.cleaner.utils.onClick

/** The fixed list of Device Info sections. */
class DeviceInfoSectionAdapter : RecyclerView.Adapter<DeviceInfoSectionAdapter.ViewHolder>() {

    private val sections = DeviceInfoSection.entries

    var onSectionClick: (DeviceInfoSection) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemDeviceInfoSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(sections[position])

    override fun getItemCount(): Int = sections.size

    inner class ViewHolder(private val binding: ItemDeviceInfoSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(section: DeviceInfoSection) {
            binding.sectionIcon.setImageResource(section.icon)
            binding.sectionTitle.setText(section.title)
            binding.root.onClick { onSectionClick(section) }
        }
    }
}
