package com.phone.cleaner.presentation.adapters

import ai.jhw.appmanager.model.InstalledApp
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phone.cleaner.databinding.ItemAppBinding
import com.phone.cleaner.utils.clipToRoundRect
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.toReadableSize

/** The App Cleaner list: icon, name and size of each app, largest first. */
class AppAdapter : ListAdapter<InstalledApp, AppAdapter.AppViewHolder>(DIFF) {

    var onAppClick: ((InstalledApp) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // The design's icon radius is 9 px of 40.
        binding.appIcon.clipToRoundRect(
            parent.resources.getDimension(com.intuit.sdp.R.dimen._7sdp),
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) = holder.bind(getItem(position))

    inner class AppViewHolder(
        private val binding: ItemAppBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: InstalledApp) = with(binding) {
            val context = root.context
            appIcon.setImageDrawable(
                try {
                    // The package manager caches icons; reading one is cheap.
                    context.packageManager.getApplicationIcon(app.packageName)
                } catch (_: PackageManager.NameNotFoundException) {
                    context.packageManager.defaultActivityIcon
                },
            )
            appName.text = app.label
            appSize.text = app.totalBytes.toReadableSize(context)
            root.onClick { onAppClick?.invoke(app) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<InstalledApp>() {
            override fun areItemsTheSame(oldItem: InstalledApp, newItem: InstalledApp) =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: InstalledApp, newItem: InstalledApp) = oldItem == newItem
        }
    }
}
