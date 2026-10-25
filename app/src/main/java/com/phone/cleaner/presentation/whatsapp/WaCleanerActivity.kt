package com.phone.cleaner.presentation.whatsapp

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
        robot = R.drawable.img_robo_wa,
        robotWidth = com.intuit.sdp.R.dimen._148sdp,
        chips = CleanerScreen.Chips.TOTAL_AND_STATUS,
        emptyMessage = R.string.wa_cleaner_none,
        robotOffsetTop = com.intuit.sdp.R.dimen._minus3sdp,
    )

    override val accessLevel = StorageAccessGate.Level.ALL_FILES

    override val logTag: String = TAG

    private companion object {
        const val TAG = "WaCleanerActivity"
    }
}
