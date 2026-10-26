package com.phone.cleaner.presentation.aicleaner

import com.phone.cleaner.R
import com.phone.cleaner.domain.view_models.AiCleanerViewModel
import com.phone.cleaner.presentation.base.BaseCleanerActivity
import com.phone.cleaner.presentation.components.CleanerScreen
import com.phone.cleaner.presentation.components.StorageAccessGate
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * AI Junk Cleaner (Figma 1:3 / 1:349 / 1:48 / 1:142 / 1:208 / 1:337), opened from Home's quick
 * action and from Scan Now. Photos and videos are enough to scan; the Junk Files section asks for
 * All files access from its own card.
 */
class AiCleanerActivity : BaseCleanerActivity() {

    override val cleaner: AiCleanerViewModel by viewModel()

    override val screen = CleanerScreen(
        title = R.string.action_ai_junk_cleaner,
        scanTitle = R.string.cleaner_loading_title,
        scanTextPadded = false,
        robotCard = R.drawable.img_robo_card_ai,
        chips = CleanerScreen.Chips.SIZES,
        emptyMessage = R.string.ai_cleaner_none,
    )

    override val accessLevel = StorageAccessGate.Level.MEDIA

    override val logTag: String = TAG

    override fun onAllFilesAccessGranted() = cleaner.onJunkAccessGranted()

    private companion object {
        const val TAG = "AiCleanerActivity"
    }
}
