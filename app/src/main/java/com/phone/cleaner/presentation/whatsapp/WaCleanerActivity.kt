package com.phone.cleaner.presentation.whatsapp

import com.phone.cleaner.BuildConfig
import com.phone.cleaner.R
import com.phone.cleaner.domain.view_models.WaCleanerViewModel
import com.phone.cleaner.presentation.base.BaseCleanerActivity
import com.phone.cleaner.presentation.components.CleanerScreen
import com.phone.cleaner.presentation.components.StorageAccessGate
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * WA (WhatsApp) Cleaner (Figma 1:997 / 1:1042 / 1:1160 / 1:1250 / 1:1526 / 1:1428), opened from
 * Home's quick action. WhatsApp keeps its media outside MediaStore's reach, so the scan needs All
 * files access.
 */
class WaCleanerActivity : BaseCleanerActivity() {

    override val cleaner: WaCleanerViewModel by viewModel()

    override val screen = CleanerScreen(
        title = R.string.action_wa_cleaner,
        scanTitle = R.string.wa_scanning_title,
        scanTextPadded = true,
        robotCard = R.drawable.img_robo_card_wa,
        chips = CleanerScreen.Chips.TOTAL_AND_STATUS,
        emptyMessage = R.string.wa_cleaner_none,
        adsKey = "wa_cleaner",
    )

    override val accessLevel = StorageAccessGate.Level.ALL_FILES

    override val logTag: String = TAG

    override fun setupViews() {
        // Debug builds only (adb: am start ... --el debug_scan_step_delay_ms 300): slows the scan
        // down so its page can be checked. In release BuildConfig.DEBUG is false and this is gone.
        if (BuildConfig.DEBUG) {
            cleaner.debugStepDelayMs = intent.getLongExtra(EXTRA_DEBUG_STEP_DELAY_MS, 0L)
        }
        super.setupViews()
    }

    private companion object {
        const val TAG = "WaCleanerActivity"
        const val EXTRA_DEBUG_STEP_DELAY_MS = "debug_scan_step_delay_ms"
    }
}
