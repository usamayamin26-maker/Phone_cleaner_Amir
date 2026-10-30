package com.phone.cleaner.presentation.deviceinfo

import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.addCallback
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.ActivityDeviceInfoBinding
import com.phone.cleaner.domain.entities.DeviceInfoSection
import com.phone.cleaner.presentation.adapters.DeviceInfoSectionAdapter
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.presentation.battery.BatteryInfoActivity
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyNavigationBarBottomMarginAboveSlot
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
        // The slot is the bottom-most view, so it clears the gesture bar; the list pays only while
        // the slot is gone, through its own gone margin.
        adSlotBottom.applyNavigationBarBottomMargin()
        sections.applyNavigationBarBottomMarginAboveSlot()

        header.headerTitle.setText(R.string.device_info_title)
        header.backButton.root.onClick { leaveWithAd(BACK_INTERSTITIAL_FLAG) }
        header.backButton.root.expandTouchTarget()
        // System back does exactly what the screen's own back button does.
        onBackPressedDispatcher.addCallback(this@DeviceInfoActivity) { leaveWithAd(BACK_INTERSTITIAL_FLAG) }

        adapter.onSectionClick = ::open
        sections.adapter = adapter
    }

    /** Bound from here, not once at creation, so coming back from a detail re-attaches the ads. */
    override fun onResume() {
        super.onResume()
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top")
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom")
    }

    override fun onDestroy() {
        AdsController.forgetSlots(SCREEN_KEY)
        super.onDestroy()
    }

    /** A section opens behind the destination's own entry interstitial. */
    private fun open(section: DeviceInfoSection) {
        val flag = if (section == DeviceInfoSection.BATTERY) {
            "is_show_battery_info_btn_inter"
        } else {
            "is_show_device_info_detail_btn_inter"
        }
        withEntryAd(flag) {
            val intent = if (section == DeviceInfoSection.BATTERY) {
                Intent(this, BatteryInfoActivity::class.java)
            } else {
                DeviceInfoDetailActivity.intent(this, section)
            }
            startActivity(intent)
        }
    }

    private companion object {
        /** This screen's ad slots and its entry in the ad caches. */
        const val SCREEN_KEY = "device_info"

        const val BACK_INTERSTITIAL_FLAG = "is_show_device_info_back_inter"
    }
}
