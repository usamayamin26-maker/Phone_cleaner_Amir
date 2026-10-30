package com.phone.cleaner.presentation.junkcleaner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import com.phone.cleaner.BuildConfig
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.ActivityScanNowBinding
import com.phone.cleaner.databinding.ItemHealthCheckBinding
import com.phone.cleaner.databinding.ItemScanNowRowBinding
import com.phone.cleaner.domain.repository.StorageAccessRepository
import com.phone.cleaner.domain.view_models.JunkStatus
import com.phone.cleaner.domain.view_models.ScanNowUiEvent
import com.phone.cleaner.domain.view_models.ScanNowUiState
import com.phone.cleaner.domain.view_models.ScanNowViewModel
import com.phone.cleaner.domain.view_models.needsDeveloperSettings
import com.phone.cleaner.presentation.aicleaner.AiCleanerActivity
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.presentation.components.StorageAccessGate
import com.phone.cleaner.utils.SharedPref
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import com.phone.cleaner.utils.applyStatusBarTopMargin
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatPercent
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.toReadableSize
import com.phone.cleaner.utils.visibleIf
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Scan Now from Home (Figma 1:1648 / 1:1705): a read-only health check of junk, Developer options,
 * USB debugging and VPN. It never changes anything: a finding worth a look opens the AI Junk
 * Cleaner's Junk Files or the system screen where the user decides.
 */
class ScanNowActivity : BaseActivity<ActivityScanNowBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityScanNowBinding = ActivityScanNowBinding::inflate

    /** The header draws itself below the status bar. */
    override val applyDefaultInsets: Boolean = false

    private val viewModel: ScanNowViewModel by viewModel()
    private val storageAccess: StorageAccessRepository by inject()
    private val sharedPref: SharedPref by inject()

    /** Junk needs All files access; the junk card asks for it. Declining keeps the results. */
    private val allFilesGate by lazy {
        StorageAccessGate(
            activity = this,
            repository = storageAccess,
            sharedPref = sharedPref,
            level = StorageAccessGate.Level.ALL_FILES,
            mediaKinds = emptySet(),
            featureName = R.string.scan_now_title,
            onGranted = viewModel::onAllFilesAccessGranted,
            onDeclined = {},
        )
    }

    override fun setupViews() = with(binding) {
        // The gate registers its result launchers, which has to happen before the activity starts.
        allFilesGate
        header.root.applyStatusBarTopMargin()
        // The pinned slot is the bottom-most view of the window, so it clears the gesture bar. The
        // scrolling content keeps its own clearance for the launches where the slot is GONE: it is
        // a LinearLayout sibling, so it cannot hand the inset over through a gone margin, and with
        // an ad showing the inset is simply extra scrollable space behind it.
        scanNowContent.applyNavigationBarBottomMargin()
        adSlotBottom.applyNavigationBarBottomMargin()
        header.headerTitle.setText(R.string.scan_now_title)
        header.backButton.root.onClick { stopAndLeave() }
        header.backButton.root.expandTouchTarget()
        // System back does exactly what the screen's own back button does.
        onBackPressedDispatcher.addCallback(this@ScanNowActivity) { stopAndLeave() }

        // The design's two rows (1:1678, 1:1687) at their sizes: the security icon 23 px, the chart
        // 18 px. Junk has no row: the scan is over the moment junk is done.
        rowSecurity.bind(R.drawable.ic_security, R.string.health_security, com.intuit.sdp.R.dimen._18sdp)
        rowVpn.bind(R.drawable.img_scan_chart, R.string.health_vpn, com.intuit.sdp.R.dimen._14sdp)

        stopButton.onClick { stopAndLeave() }
        scanAgain.onClick { viewModel.startScan() }
        cardJunk.onClick { viewModel.onJunkClicked() }
        cardSecurity.onClick { viewModel.onSecurityClicked() }
        cardVpn.onClick { viewModel.onVpnClicked() }

        // Debug builds only (adb: am start ... --el debug_scan_step_delay_ms 300): slows the scan
        // down so its page can be checked. In release BuildConfig.DEBUG is false and this is gone.
        if (BuildConfig.DEBUG) {
            viewModel.debugStepDelayMs = intent.getLongExtra(EXTRA_DEBUG_STEP_DELAY_MS, 0L)
        }

        // Nothing to ask for before scanning: the settings need no permission, and junk without
        // All files access is reported as such on its card.
        if (!viewModel.hasStarted) viewModel.startScan()
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
        viewModel.events.collectEvents(::handle)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
        bindAdSlots()
    }

    /** Bound from [onResume]: coming back from the AI cleaner or a settings screen re-attaches them. */
    private fun bindAdSlots() {
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotMid, "${SCREEN_KEY}_mid", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom", units = SCREEN_KEY)
    }

    override fun onDestroy() {
        AdsController.forgetSlots(SCREEN_KEY)
        super.onDestroy()
    }

    private fun render(state: ScanNowUiState) = with(binding) {
        scanState.visibleIf(!state.isComplete)
        doneState.visibleIf(state.isComplete)

        progressRing.progress = state.progress
        progressPercent.text = formatPercent(state.progress * 100, 0)
        // A row for each finished check, as the design shows them appearing.
        rowSecurity.root.visibleIf(state.security != null)
        rowVpn.root.visibleIf(state.security != null)

        renderJunk(state.junk)
        // Nothing was read at all: the cards stay away rather than showing findings nobody made.
        val security = state.security
        cardSecurity.visibleIf(security != null)
        cardVpn.visibleIf(security != null)
        if (security != null) {
            checkDeveloper.bind(
                isOn = security.developerOptionsOn,
                off = R.string.health_developer_off,
                on = R.string.health_developer_on,
                unknown = R.string.health_developer_unknown,
            )
            checkUsb.bind(
                isOn = security.usbDebuggingOn,
                off = R.string.health_usb_off,
                on = R.string.health_usb_on,
                unknown = R.string.health_usb_unknown,
            )
            checkWireless.bind(
                isOn = security.wirelessDebuggingOn,
                off = R.string.health_wireless_off,
                on = R.string.health_wireless_on,
                unknown = R.string.health_wireless_unknown,
            )
            checkVpn.bind(
                isOn = security.vpnConnected,
                off = R.string.health_vpn_off,
                on = R.string.health_vpn_on,
            )
            cardSecurity.isClickable = security.needsDeveloperSettings
            cardVpn.isClickable = security.vpnConnected
        }
    }

    private fun renderJunk(junk: JunkStatus?) = with(binding) {
        when (junk) {
            JunkStatus.Clean, null -> {
                junkTitle.setText(R.string.health_clean_title)
                junkMessage.setText(R.string.health_clean_message)
            }
            is JunkStatus.Found -> {
                junkTitle.setText(R.string.health_junk_found_title)
                junkMessage.text = if (junk.bytes > 0) {
                    getString(R.string.health_junk_found_message, junk.bytes.toReadableSize(this@ScanNowActivity))
                } else {
                    getString(R.string.health_junk_found_message_no_size)
                }
            }
            JunkStatus.NeedsAccess -> {
                junkTitle.setText(R.string.cleaner_title_junk_files)
                junkMessage.setText(R.string.health_junk_access_message)
            }
        }
        val actionable = junk is JunkStatus.Found || junk == JunkStatus.NeedsAccess
        junkReview.visibleIf(actionable)
        junkOk.visibleIf(!actionable)
        junkIcon.visibleIf(actionable)
        cardJunk.isClickable = actionable
    }

    private fun handle(event: ScanNowUiEvent) {
        when (event) {
            // The junk card opens the AI Junk Cleaner, so it takes that screen's entry ad.
            ScanNowUiEvent.OpenJunkFiles -> withEntryAd("is_show_ai_cleaner_btn_inter") {
                startActivity(AiCleanerActivity.junkFilesIntent(this))
            }
            ScanNowUiEvent.RequestAllFilesAccess -> allFilesGate.request()
            is ScanNowUiEvent.OpenSettings -> openSettings(event.intent)
        }
    }

    private fun openSettings(intent: Intent) {
        // A system settings screen is another app: coming back from it is not a return from
        // background, so it must not show the resume app open ad.
        AdsController.skipNextResume()
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No settings screen for $intent", e)
            showToast(getString(R.string.link_open_failed))
        }
    }

    /**
     * Back, by the screen's own button, by "Stop Scanning" or by the system gesture.
     *
     * **No full-screen ad while the scan is running.** The user pressing Stop is asking for the
     * scan to end now, and an interstitial on top of that reads as the app refusing to stop. Once
     * the scan is complete the back interstitial runs like it does on every other inner screen.
     */
    private fun stopAndLeave() {
        val wasScanning = !viewModel.state.value.isComplete
        viewModel.stopScan()
        if (wasScanning) {
            finish()
            return
        }
        leaveWithAd(BACK_INTERSTITIAL_FLAG)
    }

    private fun ItemScanNowRowBinding.bind(@DrawableRes icon: Int, @StringRes label: Int, @DimenRes size: Int) {
        rowIcon.setImageResource(icon)
        val px = resources.getDimensionPixelSize(size)
        rowIcon.updateLayoutParams<LinearLayout.LayoutParams> {
            width = px
            height = px
        }
        rowLabel.setText(label)
    }

    /**
     * A finding: the tick when all is well, a chevron to the settings when it is worth a look.
     * A null [isOn] is a setting this phone does not let the app read - the row says so in its own
     * [unknown] words and leads to the settings screen, because a tick would claim something the
     * app never checked. A setting that is always readable (the VPN) never needs [unknown].
     */
    private fun ItemHealthCheckBinding.bind(
        isOn: Boolean?,
        @StringRes off: Int,
        @StringRes on: Int,
        @StringRes unknown: Int = off,
    ) {
        checkText.setText(
            when (isOn) {
                true -> on
                false -> off
                null -> unknown
            },
        )
        checkOk.visibleIf(isOn == false)
        checkReview.visibleIf(isOn != false)
        ViewCompat.setStateDescription(root, checkText.text)
    }

    private companion object {
        const val TAG = "ScanNowActivity"
        const val EXTRA_DEBUG_STEP_DELAY_MS = "debug_scan_step_delay_ms"

        /** This screen's ad slots and its entry in the ad caches. */
        const val SCREEN_KEY = "scan_now"

        const val BACK_INTERSTITIAL_FLAG = "is_show_scan_now_back_inter"
    }
}
