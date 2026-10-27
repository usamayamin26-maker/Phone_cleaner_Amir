package com.phone.cleaner.presentation.deviceinfo

import android.content.Intent
import android.view.LayoutInflater
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ActivityDeviceInfoBinding
import com.phone.cleaner.domain.entities.DeviceInfoSection
import com.phone.cleaner.presentation.adapters.DeviceInfoSectionAdapter
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.presentation.battery.BatteryInfoActivity
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.onClick
import org.koin.android.ext.android.inject

/** Device Info: the list of sections. Battery Information opens Battery Info. */
class DeviceInfoActivity : BaseActivity<ActivityDeviceInfoBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityDeviceInfoBinding =
        ActivityDeviceInfoBinding::inflate

    override val applyDefaultInsets: Boolean = false

    private val adapter: DeviceInfoSectionAdapter by inject()

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        sections.applyNavigationBarBottomMargin()

        header.headerTitle.setText(R.string.device_info_title)
        header.backButton.root.onClick { finish() }
        header.backButton.root.expandTouchTarget()

        adapter.onSectionClick = ::open
        sections.adapter = adapter
    }

    private fun open(section: DeviceInfoSection) {
        val intent = if (section == DeviceInfoSection.BATTERY) {
            Intent(this, BatteryInfoActivity::class.java)
        } else {
            DeviceInfoDetailActivity.intent(this, section)
        }
        startActivity(intent)
    }
}
