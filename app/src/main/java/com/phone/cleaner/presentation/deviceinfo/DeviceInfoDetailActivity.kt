package com.phone.cleaner.presentation.deviceinfo

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ActivityDeviceInfoDetailBinding
import com.phone.cleaner.domain.entities.DeviceInfoSection
import com.phone.cleaner.domain.view_models.DeviceInfoUiState
import com.phone.cleaner.domain.view_models.DeviceInfoViewModel
import com.phone.cleaner.presentation.adapters.InfoListAdapter
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.visibleIf
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * One Device Info section: Device Information as designed, and Processor, WiFi, Display, RAM, ROM
 * and Camera in the same layout.
 */
class DeviceInfoDetailActivity : BaseActivity<ActivityDeviceInfoDetailBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityDeviceInfoDetailBinding =
        ActivityDeviceInfoDetailBinding::inflate

    override val applyDefaultInsets: Boolean = false

    private val viewModel: DeviceInfoViewModel by viewModel()
    private val adapter: InfoListAdapter by inject()

    private val section: DeviceInfoSection by lazy {
        val name = intent.getStringExtra(EXTRA_SECTION)
        DeviceInfoSection.entries.firstOrNull { it.name == name } ?: run {
            Log.w(TAG, "Unknown section \"$name\", showing Device Information")
            DeviceInfoSection.DEVICE
        }
    }

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        contentColumn.applyNavigationBarBottomMargin()

        header.headerTitle.setText(section.title)
        header.backButton.root.onClick { finish() }
        header.backButton.root.expandTouchTarget()

        headerImage.setImageResource(section.icon)
        rows.adapter = adapter

        viewModel.load(section)
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
    }

    private fun render(state: DeviceInfoUiState) {
        binding.progress.visibleIf(state.isLoading)
        val content = state.content
        adapter.submitList(if (content != null) DeviceInfoFormatter(this).format(content) else emptyList())
        if (state.error != null) {
            showToast(getString(R.string.error_generic))
            viewModel.clearError()
        }
    }

    companion object {
        private const val TAG = "DeviceInfoDetailActivity"
        private const val EXTRA_SECTION = "section"

        fun intent(context: Context, section: DeviceInfoSection): Intent =
            Intent(context, DeviceInfoDetailActivity::class.java).putExtra(EXTRA_SECTION, section.name)
    }
}
