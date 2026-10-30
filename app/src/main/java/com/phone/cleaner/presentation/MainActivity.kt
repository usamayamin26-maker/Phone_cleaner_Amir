package com.phone.cleaner.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.ActivityMainBinding
import com.phone.cleaner.databinding.ItemQuickActionBinding
import com.phone.cleaner.domain.view_models.HomeUiState
import com.phone.cleaner.domain.view_models.HomeViewModel
import com.phone.cleaner.presentation.aicleaner.AiCleanerActivity
import com.phone.cleaner.presentation.appmanager.AppCleanerActivity
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.presentation.battery.BatteryInfoActivity
import com.phone.cleaner.presentation.components.EXIT_SHEET_SCREEN_KEY
import com.phone.cleaner.presentation.components.exitBottomSheet
import com.phone.cleaner.presentation.deviceinfo.DeviceInfoActivity
import com.phone.cleaner.presentation.duplicates.DuplicatesActivity
import com.phone.cleaner.presentation.junkcleaner.ScanNowActivity
import com.phone.cleaner.presentation.settings.SettingsActivity
import com.phone.cleaner.presentation.whatsapp.WaCleanerActivity
import com.phone.cleaner.utils.InAppUpdateManager
import com.phone.cleaner.utils.SharedPref
import com.phone.cleaner.utils.applyFittingTextSize
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.fittingTextSize
import com.phone.cleaner.utils.formatPercent
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.keepTogether
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

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Held only so a second back press does not stack a second sheet, and to close it on destroy. */
    private var exitSheet: BottomSheetDialog? = null

    /** Google Play in-app update (`in_app_update_type` in the ads Remote Config JSON). */
    private val inAppUpdate = InAppUpdateManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Once per launch, not on a recreate (a language change recreates the whole stack).
        if (savedInstanceState == null) inAppUpdate.checkForUpdate()
    }

    override fun setupViews() = with(binding) {
        // From now on a launch is not a first run: the splash goes straight to Home.
        sharedPref.hasOpenedMainScreen = true

        statusAnchor.applyStatusBarTopMargin()
        // The pinned slot is the bottom-most view of the window, so it clears the gesture bar. The
        // quick actions keep their own clearance as the end of the scrolling content: their bottom
        // points at the parent, not at the slot, so they cannot hand it over through a gone margin,
        // and with an ad showing the inset is simply extra scrollable space behind it.
        quickActions.applyNavigationBarBottomMargin()
        adSlotBottom.applyNavigationBarBottomMargin()

        settingsButton.expandTouchTarget()

        actionJunk.bind(R.drawable.ic_qa_ai_junk, R.string.action_ai_junk_cleaner, QuickActionTint.AI_JUNK)
        actionDuplicates.bind(R.drawable.ic_qa_duplicates, R.string.action_delete_duplicates, QuickActionTint.DUPLICATES)
        actionAppCleaner.bind(R.drawable.ic_qa_app_cleaner, R.string.action_app_cleaner, QuickActionTint.APP_CLEANER)
        actionWaCleaner.bind(R.drawable.ic_qa_wa, R.string.action_wa_cleaner, QuickActionTint.WA)
        actionDeviceInfo.bind(R.drawable.ic_qa_device_info, R.string.device_info_title, QuickActionTint.DEVICE_INFO)
        actionBatteryInfo.bind(R.drawable.ic_qa_battery_info, R.string.battery_info_title, QuickActionTint.BATTERY_INFO)

        fitLabelsToTheirBoxes()

        // Every one of these opens a screen, so each goes behind the destination's own entry
        // interstitial. The gear is a screen button; Scan Now and the six cards are Home's own
        // navigation row and share counter_quick_action, so the console can let one in every N
        // taps of that row show an ad without touching the buttons on the inner screens.
        settingsButton.onClick {
            withEntryAd("is_show_settings_btn_inter") { open(SettingsActivity::class.java) }
        }
        scanNow.onClick {
            withEntryAd("is_show_scan_now_btn_inter", COUNTER_QUICK_ACTION) {
                open(ScanNowActivity::class.java)
            }
        }
        actionJunk.root.onClick {
            withEntryAd("is_show_ai_cleaner_btn_inter", COUNTER_QUICK_ACTION) {
                open(AiCleanerActivity::class.java)
            }
        }
        actionWaCleaner.root.onClick {
            withEntryAd("is_show_wa_cleaner_btn_inter", COUNTER_QUICK_ACTION) {
                open(WaCleanerActivity::class.java)
            }
        }
        actionDuplicates.root.onClick {
            withEntryAd("is_show_duplicates_btn_inter", COUNTER_QUICK_ACTION) {
                open(DuplicatesActivity::class.java)
            }
        }
        actionAppCleaner.root.onClick {
            withEntryAd("is_show_app_cleaner_btn_inter", COUNTER_QUICK_ACTION) {
                open(AppCleanerActivity::class.java)
            }
        }
        actionDeviceInfo.root.onClick {
            withEntryAd("is_show_device_info_btn_inter", COUNTER_QUICK_ACTION) {
                open(DeviceInfoActivity::class.java)
            }
        }
        actionBatteryInfo.root.onClick {
            withEntryAd("is_show_battery_info_btn_inter", COUNTER_QUICK_ACTION) {
                open(BatteryInfoActivity::class.java)
            }
        }

        setupBackPress()
    }

    private fun open(screen: Class<*>) = startActivity(Intent(this, screen))

    /**
     * Back on Home asks before leaving, through the exit sheet. Through the dispatcher rather than
     * `onBackPressed`, which is deprecated and is not what the predictive-back gesture consults.
     */
    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = showExitSheet()
        })
    }

    /**
     * One sheet at a time: back is easy to press twice, and without this the second press builds a
     * second sheet behind the first, whose Exit would then be waiting after the first is dismissed.
     */
    private fun showExitSheet() {
        if (exitSheet?.isShowing == true) return
        exitSheet = exitBottomSheet(this, layoutInflater) { finish() }.also { it.show() }
    }

    /**
     * The slots are bound from here rather than once at creation: Home is visible from now on, and
     * coming back to it from a feature screen is exactly when a cached ad should be re-attached or
     * an expired one replaced.
     */
    private fun bindAdSlots() {
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotMid, "${SCREEN_KEY}_mid", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom", units = SCREEN_KEY)
    }

    /**
     * The sheet holds this activity's window, so it goes down with it; the ads package asks the
     * host to forget the keys it owns, and the exit slot's key is this activity's.
     */
    override fun onDestroy() {
        exitSheet?.dismiss()
        exitSheet = null
        AdsController.forgetSlots(SCREEN_KEY, EXIT_SHEET_SCREEN_KEY)
        super.onDestroy()
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
    }

    override fun onResume() {
        super.onResume()
        // RAM in use changes all the time; the cards show it as of now.
        viewModel.loadUsage()
        bindAdSlots()
        maybeAskNotificationPermission()
    }

    /**
     * Android 13+ needs POST_NOTIFICATIONS for FCM (Firebase Cloud Messaging) notifications. Asked
     * once, the first time Home is on screen, from the activity itself rather than a one-shot
     * event (the project's never-from-onStart rule). Denying it changes nothing else in the app.
     */
    private fun maybeAskNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (sharedPref.isNotificationPermissionAsked) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return

        sharedPref.isNotificationPermissionAsked = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun render(state: HomeUiState) {
        state.storage?.memory?.let { storage ->
            binding.storageValue.text = getString(
                R.string.home_used_of,
                formatPercent(storage.usedPercent).keepTogether(),
                storage.totalBytes.toReadableSize(this).keepTogether(),
            )
            binding.storageUsage.fitUsageText(binding.storageValue)
        }
        state.ram?.memory?.let { ram ->
            binding.ramValue.text = getString(
                R.string.home_used_of,
                ram.usedBytes.toReadableSize(this).keepTogether(),
                ram.totalBytes.toReadableSize(this).keepTogether(),
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

    private companion object {
        /** This screen's ad slots and its entry in the ad caches. */
        const val SCREEN_KEY = "home"

        /** Scan Now and the six quick action cards: Home's own navigation row, counted together. */
        const val COUNTER_QUICK_ACTION = "counter_quick_action"
    }
}
