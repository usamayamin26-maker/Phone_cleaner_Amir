package com.phone.cleaner.presentation

import android.content.Intent
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ActivityMainBinding
import com.phone.cleaner.databinding.ItemQuickActionBinding
import com.phone.cleaner.domain.view_models.HomeUiState
import com.phone.cleaner.domain.view_models.HomeViewModel
import com.phone.cleaner.presentation.aicleaner.AiCleanerActivity
import com.phone.cleaner.presentation.appmanager.AppCleanerActivity
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.presentation.battery.BatteryInfoActivity
import com.phone.cleaner.presentation.deviceinfo.DeviceInfoActivity
import com.phone.cleaner.presentation.duplicates.DuplicatesActivity
import com.phone.cleaner.presentation.junkcleaner.ScanNowActivity
import com.phone.cleaner.presentation.settings.SettingsActivity
import com.phone.cleaner.presentation.whatsapp.WaCleanerActivity
import com.phone.cleaner.utils.SharedPref
import com.phone.cleaner.utils.applyFittingTextSize
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.fittingTextSize
import com.phone.cleaner.utils.formatPercent
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.toReadableSize
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.sqrt

/**
 * The app's main screen: Home from the design. Every feature (Device Info, Battery Info, WA Cleaner,
 * ...) opens on top of it as its own activity. Scan Now opens the health check; the AI Junk
 * Cleaner quick action opens the AI cleaner.
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityMainBinding =
        ActivityMainBinding::inflate

    // The blue gradient runs behind the status bar, and the title on it is white.
    override val applyDefaultInsets: Boolean = false
    override val darkStatusBarContent: Boolean = true

    private val viewModel: HomeViewModel by viewModel()
    private val sharedPref: SharedPref by inject()

    override fun setupViews() = with(binding) {
        // From now on a launch is not a first run: the splash goes straight to Home.
        sharedPref.hasOpenedMainScreen = true

        statusAnchor.applyStatusBarTopMargin()
        quickActions.applyNavigationBarBottomMargin()

        settingsButton.expandTouchTarget()

        actionJunk.bind(R.drawable.ic_qa_ai_junk, R.string.action_ai_junk_cleaner, QuickActionTint.AI_JUNK)
        actionDuplicates.bind(R.drawable.ic_qa_duplicates, R.string.action_delete_duplicates, QuickActionTint.DUPLICATES)
        actionAppCleaner.bind(R.drawable.ic_qa_app_cleaner, R.string.action_app_cleaner, QuickActionTint.APP_CLEANER)
        actionWaCleaner.bind(R.drawable.ic_qa_wa, R.string.action_wa_cleaner, QuickActionTint.WA)
        actionDeviceInfo.bind(R.drawable.ic_qa_device_info, R.string.device_info_title, QuickActionTint.DEVICE_INFO)
        actionBatteryInfo.bind(R.drawable.ic_qa_battery_info, R.string.battery_info_title, QuickActionTint.BATTERY_INFO)

        fitLabelsToTheirBoxes()

        settingsButton.onClick {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }
        scanNow.onClick {
            startActivity(Intent(this@MainActivity, ScanNowActivity::class.java))
        }
        actionJunk.root.onClick {
            startActivity(Intent(this@MainActivity, AiCleanerActivity::class.java))
        }
        actionWaCleaner.root.onClick {
            startActivity(Intent(this@MainActivity, WaCleanerActivity::class.java))
        }
        actionDuplicates.root.onClick {
            startActivity(Intent(this@MainActivity, DuplicatesActivity::class.java))
        }
        actionAppCleaner.root.onClick {
            startActivity(Intent(this@MainActivity, AppCleanerActivity::class.java))
        }
        actionDeviceInfo.root.onClick {
            startActivity(Intent(this@MainActivity, DeviceInfoActivity::class.java))
        }
        actionBatteryInfo.root.onClick {
            startActivity(Intent(this@MainActivity, BatteryInfoActivity::class.java))
        }
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
    }

    override fun onResume() {
        super.onResume()
        // RAM in use changes all the time; the cards show it as of now.
        viewModel.loadUsage()
    }

    private fun render(state: HomeUiState) {
        state.storage?.memory?.let { storage ->
            binding.storageValue.text = getString(
                R.string.home_used_of,
                formatPercent(storage.usedPercent),
                storage.totalBytes.toReadableSize(this),
            )
            binding.storageUsage.fitUsageText(binding.storageValue)
        }
        state.ram?.memory?.let { ram ->
            binding.ramValue.text = getString(
                R.string.home_used_of,
                ram.usedBytes.toReadableSize(this),
                ram.totalBytes.toReadableSize(this),
            )
            binding.ramUsage.fitUsageText(binding.ramValue)
        }
        if (state.error != null) {
            showToast(getString(R.string.error_generic))
            viewModel.clearError()
        }
    }

    /** Each card's colours (Figma 34:65 and 34:71): the icon and arrow, the icon tile, the arrow circle. */
    private enum class QuickActionTint(
        @ColorRes val accent: Int,
        @ColorRes val tile: Int,
        @ColorRes val arrowFill: Int,
    ) {
        AI_JUNK(R.color.qa_ai_junk_accent, R.color.qa_ai_junk_tile, R.color.qa_ai_junk_arrow_fill),
        DUPLICATES(R.color.qa_duplicates_accent, R.color.qa_duplicates_tile, R.color.qa_duplicates_arrow_fill),
        APP_CLEANER(R.color.qa_app_cleaner_accent, R.color.qa_app_cleaner_tile, R.color.qa_app_cleaner_arrow_fill),
        WA(R.color.qa_wa_accent, R.color.qa_wa_tile, R.color.qa_wa_arrow_fill),
        DEVICE_INFO(R.color.qa_device_info_accent, R.color.qa_device_info_tile, R.color.qa_device_info_arrow_fill),
        BATTERY_INFO(R.color.qa_battery_info_accent, R.color.qa_battery_info_tile, R.color.qa_battery_info_arrow_fill),
    }

    private fun ItemQuickActionBinding.bind(@DrawableRes icon: Int, @StringRes title: Int, tint: QuickActionTint) {
        val accent = ContextCompat.getColorStateList(this@MainActivity, tint.accent)
        actionIcon.setImageResource(icon)
        actionIcon.imageTintList = accent
        actionArrow.imageTintList = accent
        actionIconTile.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, tint.tile)
        actionArrowCircle.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, tint.arrowFill)
        actionTitle.setText(title)
    }

    /**
     * The quick action cards, the Scan Now circle and the room beside the circle for Storage and
     * RAM Usage are boxes the design fixes in width (the circle in height too). At a large font
     * size, or with a long word in some languages, a label could break inside a word or spill out
     * of the circle. Then the label is made smaller until it fits; the six card labels shrink
     * together, and so do the two usage titles, so they stay even.
     */
    private fun fitLabelsToTheirBoxes() = with(binding) {
        val cardTitles = listOf(
            actionJunk, actionDuplicates, actionAppCleaner,
            actionWaCleaner, actionDeviceInfo, actionBatteryInfo,
        ).map { it.actionTitle }
        quickActions.doOnLayout {
            val size = cardTitles.minOf { it.fittingTextSize(it.width) }
            cardTitles.forEach { it.applyFittingTextSize(size) }
        }
        // Posted: a size set while the other block is still in its layout pass keeps its old height.
        storageUsage.doOnLayout {
            storageUsage.post {
                val size = minOf(
                    storageTitle.fittingTextSize(storageUsage.roomForText()),
                    ramTitle.fittingTextSize(ramUsage.roomForText()),
                )
                storageTitle.applyFittingTextSize(size)
                ramTitle.applyFittingTextSize(size)
            }
        }
        scanNowCircle.doOnLayout { circle ->
            // The label and the broom stay inside the square that fits in the circle.
            val inner = (circle.width / sqrt(2.0)).toInt()
            val label = scanNowLabel
            label.applyFittingTextSize(
                label.fittingTextSize(label.maxWidth, inner - scanNowIcon.height),
            )
        }
    }

    /** Fits a usage value, which changes on every load, to the room beside the Scan Now ring. */
    private fun LinearLayout.fitUsageText(value: TextView) = doOnLayout {
        post { value.applyFittingTextSize(value.fittingTextSize(roomForText())) }
    }

    /** A usage block is its icon tile and then its text column, which gets the rest of the width. */
    private fun LinearLayout.roomForText(): Int {
        val tile = getChildAt(0)
        val text = getChildAt(1)
        return width - paddingStart - paddingEnd - tile.width - text.marginStart - text.marginEnd
    }
}
