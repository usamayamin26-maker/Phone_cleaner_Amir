package com.phone.cleaner.presentation.duplicates

import androidx.fragment.app.Fragment
import com.phone.cleaner.R
import com.phone.cleaner.domain.entities.CleanerSection
import com.phone.cleaner.domain.view_models.DuplicatesViewModel
import com.phone.cleaner.presentation.base.BaseCleanerActivity
import com.phone.cleaner.presentation.components.CleanerScreen
import com.phone.cleaner.presentation.components.StorageAccessGate
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Delete Duplicates (Figma 1:483 / 1:528 / 1:661): the shared cleaner pages, with its own page for
 * a section of duplicate groups - "Keep best one" and every copy with its check.
 */
class DuplicatesActivity : BaseCleanerActivity() {

    override val cleaner: DuplicatesViewModel by viewModel()

    override val screen = CleanerScreen(
        title = R.string.action_delete_duplicates,
        scanTitle = R.string.cleaner_loading_title,
        scanTextPadded = true,
        robot = R.drawable.img_robo_duplicate,
        robotWidth = com.intuit.sdp.R.dimen._123sdp,
        chips = CleanerScreen.Chips.SIZES,
        emptyMessage = R.string.duplicates_none,
        robotMarginStart = com.intuit.sdp.R.dimen._12sdp,
        robotOffsetTop = com.intuit.sdp.R.dimen._minus17sdp,
    )

    /** Photos and videos through MediaStore; Android is not asked for audio. */
    override val accessLevel = StorageAccessGate.Level.MEDIA

    override val logTag: String = TAG

    override fun pageFor(section: CleanerSection): Fragment =
        if (section.type in cleaner.state.value.duplicateGroups) {
            DuplicatesDetailFragment.newInstance(section.type)
        } else {
            super.pageFor(section)
        }

    private companion object {
        const val TAG = "DuplicatesActivity"
    }
}
