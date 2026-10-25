package com.phone.cleaner.presentation

import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.doOnLayout
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ActivityMainBinding
import com.phone.cleaner.databinding.ItemQuickActionBinding
import com.phone.cleaner.databinding.ItemUsagePillBinding
import com.phone.cleaner.domain.view_models.HomeUiState
import com.phone.cleaner.domain.view_models.HomeViewModel
import com.phone.cleaner.presentation.aicleaner.AiCleanerActivity
import com.phone.cleaner.presentation.appmanager.AppCleanerActivity
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.presentation.battery.BatteryInfoActivity
import com.phone.cleaner.presentation.deviceinfo.DeviceInfoActivity
import com.phone.cleaner.presentation.duplicates.DuplicatesActivity
import com.phone.cleaner.presentation.settings.SettingsActivity
import com.phone.cleaner.presentation.whatsapp.WaCleanerActivity
import com.phone.cleaner.utils.SharedPref
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatPercent
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.playAnimatedImage
import com.phone.cleaner.utils.toReadableSize
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * The app's main screen: Home from the design. Every feature (Device Info, Battery Info, WA Cleaner,
 * ...) opens on top of it as its own activity. Scan Now opens the AI Junk Cleaner, like its quick
 * action.
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityMainBinding =
        ActivityMainBinding::inflate

    // The blue header runs behind the status bar, and its title is white.
    override val applyDefaultInsets: Boolean = false
    override val darkStatusBarContent: Boolean = true

    private val viewModel: HomeViewModel by viewModel()
    private val sharedPref: SharedPref by inject()

    override fun setupViews() = with(binding) {
        // From now on a launch is not a first run: the splash goes straight to Home.
        sharedPref.hasOpenedMainScreen = true

        statusAnchor.applyStatusBarTopMargin()
        quickActions.applyNavigationBarBottomMargin()

        robot.playAnimatedImage(R.drawable.img_robot)
        settingsButton.expandTouchTarget()

        storagePill.usageTitle.setText(R.string.home_storage_usage)
        ramPill.usageTitle.setText(R.string.home_ram_usage)

        actionJunk.bind(R.drawable.img_action_junk_cleaner, R.string.action_ai_junk_cleaner)
        actionDuplicates.bind(R.drawable.img_action_delete_duplicates, R.string.action_delete_duplicates)
        actionAppCleaner.bind(R.drawable.img_action_app_cleaner, R.string.action_app_cleaner)
        actionWaCleaner.bind(R.drawable.img_action_wa_cleaner, R.string.action_wa_cleaner)
        actionDeviceInfo.bind(R.drawable.img_action_device_info, R.string.device_info_title)
        actionBatteryInfo.bind(R.drawable.img_action_battery_info, R.string.battery_info_title)

        stackQuickActionsIfWordsDoNotFit()

        settingsButton.onClick {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }
        scanNow.onClick {
            startActivity(Intent(this@MainActivity, AiCleanerActivity::class.java))
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
            binding.storagePill.setUsage(
                used = formatPercent(storage.usedPercent),
                total = storage.totalBytes.toReadableSize(this),
            )
        }
        state.ram?.memory?.let { ram ->
            binding.ramPill.setUsage(
                used = ram.usedBytes.toReadableSize(this),
                total = ram.totalBytes.toReadableSize(this),
            )
        }
        if (state.error != null) {
            showToast(getString(R.string.error_generic))
            viewModel.clearError()
        }
    }

    private fun ItemUsagePillBinding.setUsage(used: String, total: String) {
        usageValue.text = getString(R.string.home_used_of, used, total)
    }

    private fun ItemQuickActionBinding.bind(@DrawableRes icon: Int, @StringRes title: Int) {
        actionIcon.setImageResource(icon)
        actionTitle.setText(title)
    }

    /**
     * The design puts each label beside its icon. At a large font size, or with a long word in
     * some languages, a single word can be wider than the room left beside the icon, and the
     * label would break inside the word. Then every card puts its label under the icon instead,
     * so the grid stays even.
     */
    private fun stackQuickActionsIfWordsDoNotFit() = with(binding) {
        val cards = listOf(
            actionJunk, actionDuplicates, actionAppCleaner,
            actionWaCleaner, actionDeviceInfo, actionBatteryInfo,
        )
        quickActions.doOnLayout {
            val wordDoesNotFit = cards.any { card ->
                val row = card.actionRow
                val room = row.width - row.paddingStart - row.paddingEnd -
                    card.actionIconFrame.width - card.actionTitle.marginStart
                val longestWord = card.actionTitle.text.split(' ', '\n')
                    .maxOf { card.actionTitle.paint.measureText(it) }
                longestWord > room
            }
            if (wordDoesNotFit) cards.forEach { it.stackVertically() }
        }
    }

    private fun ItemQuickActionBinding.stackVertically() {
        actionRow.orientation = LinearLayout.VERTICAL
        actionRow.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        actionTitle.maxWidth = Int.MAX_VALUE
        actionTitle.updateLayoutParams<LinearLayout.LayoutParams> {
            marginStart = 0
            topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._9sdp)
            width = LinearLayout.LayoutParams.MATCH_PARENT
        }
    }
}
