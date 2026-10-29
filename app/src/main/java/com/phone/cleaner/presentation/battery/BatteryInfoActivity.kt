package com.phone.cleaner.presentation.battery

import ai.jhw.battery.model.BatteryHealth
import ai.jhw.battery.model.CapacitySource
import android.content.ActivityNotFoundException
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Switch
import androidx.activity.addCallback
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.ActivityBatteryInfoBinding
import com.phone.cleaner.domain.entities.BatteryDetails
import com.phone.cleaner.domain.view_models.BatteryUiEvent
import com.phone.cleaner.domain.view_models.BatteryUiState
import com.phone.cleaner.domain.view_models.BatteryViewModel
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyNavigationBarBottomMarginAboveSlot
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.orNotAvailable
import com.phone.cleaner.utils.setOn
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Battery Info: live values while the screen is visible. The Power Saving Mode switch shows the
 * system's state; a tap opens the Battery Saver settings, because an app cannot switch it.
 */
class BatteryInfoActivity : BaseActivity<ActivityBatteryInfoBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityBatteryInfoBinding =
        ActivityBatteryInfoBinding::inflate

    override val applyDefaultInsets: Boolean = false

    private val viewModel: BatteryViewModel by viewModel()

    /** Read by the accessibility delegate, so TalkBack announces a switch that is on or off. */
    private var powerSaveOn = false

    override fun setupViews() = with(binding) {
        header.root.applyStatusBarTopMargin()
        // The slot is the bottom-most view, so it clears the gesture bar; the scroll pays only
        // while the slot is gone, through its own gone margin.
        adSlotBottom.applyNavigationBarBottomMargin()
        content.applyNavigationBarBottomMarginAboveSlot()

        header.headerTitle.setText(R.string.battery_info_title)
        header.backButton.root.onClick { leaveWithAd(BACK_INTERSTITIAL_FLAG) }
        header.backButton.root.expandTouchTarget()
        // System back does exactly what the screen's own back button does.
        onBackPressedDispatcher.addCallback(this@BatteryInfoActivity) { leaveWithAd(BACK_INTERSTITIAL_FLAG) }

        powerSaving.contentDescription = getString(R.string.battery_power_saving)
        powerSaving.onClick { viewModel.onPowerSavingClicked() }
        ViewCompat.setAccessibilityDelegate(powerSaving, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = Switch::class.java.name
                info.isCheckable = true
                info.setChecked(
                    if (powerSaveOn) {
                        AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE
                    } else {
                        AccessibilityNodeInfoCompat.CHECKED_STATE_FALSE
                    },
                )
            }
        })
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
        viewModel.events.collectEvents(::handle)
    }

    override fun onDestroy() {
        AdsController.forgetSlots(SCREEN_KEY)
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startObserving()
    }

    override fun onResume() {
        super.onResume()
        // Back from the Battery Saver settings: show what the user chose there.
        viewModel.refresh()
        // The slots are bound here too, so coming back re-attaches a cached ad or replaces an
        // expired one.
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotMid, "${SCREEN_KEY}_mid", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom", units = SCREEN_KEY)
    }

    override fun onStop() {
        viewModel.stopObserving()
        super.onStop()
    }

    private fun render(state: BatteryUiState) {
        state.battery?.let(::renderBattery)
        if (state.error != null) {
            showToast(getString(R.string.error_generic))
            viewModel.clearError()
        }
    }

    private fun renderBattery(battery: BatteryDetails) = with(binding) {
        val info = battery.info

        powerSaveOn = info.isPowerSaveMode
        powerSavingSwitch.setOn(info.isPowerSaveMode)
        ViewCompat.setStateDescription(
            powerSaving,
            getString(if (info.isPowerSaveMode) R.string.state_on else R.string.state_off),
        )

        healthValue.setText(healthText(info.health))

        chargedValue.text = when {
            info.isCharging -> getString(R.string.battery_charging)
            battery.lastFullChargeAt > 0 -> DateUtils.getRelativeTimeSpanString(
                battery.lastFullChargeAt,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS,
            ).toString()
            else -> getString(R.string.value_not_available)
        }

        temperatureValue.text = info.temperatureCelsius
            ?.let { getString(R.string.format_celsius, formatNumber(it, maxFractionDigits = 1)) }
            .orNotAvailable(this@BatteryInfoActivity)

        val millivolts = info.voltageMillivolts
        voltageValue.text = millivolts
            ?.let { getString(R.string.format_volts, formatNumber(it / 1000.0, 3, 3)) }
            .orNotAvailable(this@BatteryInfoActivity)
        voltageMillivolts.text = millivolts
            ?.let { getString(R.string.format_millivolts, formatNumber(it)) }
            .orNotAvailable(this@BatteryInfoActivity)

        capacityValue.text = info.capacityMah
            ?.let { getString(R.string.format_mah, formatNumber(it)) }
            .orNotAvailable(this@BatteryInfoActivity)
        capacityLabel.setText(
            if (info.capacitySource == CapacitySource.CHARGE_COUNTER_ESTIMATE) {
                R.string.battery_capacity_approx
            } else {
                R.string.battery_capacity
            },
        )
    }

    private fun handle(event: BatteryUiEvent) {
        when (event) {
            is BatteryUiEvent.OpenBatterySaverSettings -> try {
                // The Battery Saver settings are another app: coming back is not a return from
                // background, so no resume app open ad.
                AdsController.skipNextResume()
                startActivity(event.intent)
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No Battery Saver settings screen", e)
                showToast(getString(R.string.battery_saver_unavailable))
            }
        }
    }

    private fun healthText(health: BatteryHealth): Int = when (health) {
        BatteryHealth.GOOD -> R.string.health_good
        BatteryHealth.OVERHEAT -> R.string.health_overheat
        BatteryHealth.DEAD -> R.string.health_dead
        BatteryHealth.OVER_VOLTAGE -> R.string.health_over_voltage
        BatteryHealth.COLD -> R.string.health_cold
        BatteryHealth.FAILURE -> R.string.health_failure
        BatteryHealth.UNKNOWN -> R.string.health_unknown
    }

    private companion object {
        const val TAG = "BatteryInfoActivity"

        /** This screen's ad slots and its entry in the ad caches. */
        const val SCREEN_KEY = "battery_info"

        const val BACK_INTERSTITIAL_FLAG = "is_show_admob_battery_info_back_inter"
    }
}
