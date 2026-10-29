package com.phone.cleaner.presentation.components

import android.app.Activity
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.phone.cleaner.R
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.databinding.DialogExitBottomSheetBinding
import com.phone.cleaner.utils.onClick

/**
 * The sheet Home's back press opens: "Leave the app?", an optional native ad, and Stay / Exit.
 * [onExit] runs only when Exit is tapped; every other way out of the sheet - Stay, the scrim, a
 * swipe down, a second back press - simply dismisses it and the app stays where it was.
 *
 * Built like [AppDialog]: a function that inflates its binding, wires it and hands back the dialog,
 * with no state of its own.
 *
 * The gesture-bar clearance is Material's, not this file's: `enableEdgeToEdge` on the sheet theme
 * runs the surface behind the navigation bar and pads the content clear of it. A sheet has its own
 * window, so the activity's `applyNavigationBarBottomMargin` does not reach it - and adding the
 * inset here instead counts it twice.
 *
 * **The ad is bound before the sheet is shown, deliberately.** `AdsController.slot` waits on the
 * ads bootstrap, and by the time Home exists the bootstrap is long since ready, so that callback
 * runs synchronously: the slot is already holding its shimmer skeleton, at the exact height the ad
 * will be, before the sheet measures itself. Showing first and binding after would open the sheet
 * at its no-ad height and then grow it under the user's thumb, which on a sheet moves the buttons
 * they were reaching for. With the flags at 0 the loader collapses the slot in that same
 * synchronous pass, so the sheet measures exactly as it does with no ad at all.
 *
 * The ad is an inline native like any other slot - the same view, the same call, its own screen
 * key. It is not a full-screen ad and deliberately touches neither the minimum gap between
 * full-screen ads nor any counter: the sheet is a question the user asked for by pressing back, not
 * an interruption the app chose to add.
 */
fun exitBottomSheet(
    activity: Activity,
    inflater: LayoutInflater,
    onExit: () -> Unit,
): BottomSheetDialog {
    val binding = DialogExitBottomSheetBinding.inflate(inflater)
    val dialog = BottomSheetDialog(activity, R.style.Theme_PhoneCleaner_BottomSheet).apply {
        setContentView(binding.root)
    }

    binding.buttonExitStay.onClick { dialog.dismiss() }
    binding.buttonExitConfirm.onClick {
        dialog.dismiss()
        onExit()
    }

    // No units group: the exit slot serves from the shared admob_app_*_id.
    AdsController.slot(activity, binding.adSlotBottom, "${EXIT_SHEET_SCREEN_KEY}_bottom")

    return dialog
}

/**
 * This sheet's ad slot and its entry in the native ad cache, unique per screen. Public because the
 * host that shows the sheet owns the key's lifetime: Home hands it to `AdsController.forgetSlots`
 * when it is destroyed.
 */
const val EXIT_SHEET_SCREEN_KEY = "exit"
