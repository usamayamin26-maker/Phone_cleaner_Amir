package com.phone.cleaner.presentation.components

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.updateLayoutParams
import com.phone.cleaner.databinding.ViewAppDialogBinding
import com.phone.cleaner.utils.gone
import com.phone.cleaner.utils.onClick
import com.phone.cleaner.utils.visible

/**
 * The app's one dialog, from the design's dialog component (Figma 1:984 "Are You Sure?", 1:1881
 * uninstall, 1:2884 "Permission is required"): optional icon, title, message, a primary button and
 * a text button on a white card. Every confirmation in the app goes through it, so a delete is
 * never confirmed by anything else.
 *
 * The screen behind is dimmed by [Scrim] and, where the platform can (Android 12+ with blur on),
 * blurred by the design's 12 px.
 */
object AppDialog {

    /** How strongly the screen behind is dimmed: Figma's overlay is #171A2B at 7% or 50%. */
    enum class Scrim(val dimAmount: Float) {
        /** "Are You Sure?" and uninstall (1:983, 1:1880). */
        LIGHT(0.07f),

        /** "Permission is required" (1:2883). */
        STRONG(0.5f),
    }

    class Spec(
        val title: CharSequence,
        val message: CharSequence,
        val primaryLabel: CharSequence,
        val secondaryLabel: CharSequence,
        val icon: Drawable? = null,
        /** False for the uninstall dialog, whose button is 211 px wide and centred. */
        val primaryFullWidth: Boolean = true,
        val scrim: Scrim = Scrim.LIGHT,
    )

    /**
     * Shows the dialog. [onCancel] runs when it is dismissed without a button (back, a tap outside);
     * each button closes the dialog and runs its own callback.
     */
    fun show(
        context: Context,
        spec: Spec,
        onPrimary: () -> Unit,
        onSecondary: () -> Unit = {},
        onCancel: () -> Unit = {},
    ): Dialog {
        val binding = ViewAppDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context)
        dialog.setContentView(binding.root)

        with(binding) {
            if (spec.icon != null) {
                dialogIcon.setImageDrawable(spec.icon)
                dialogIcon.visible()
            } else {
                dialogIcon.gone()
            }
            dialogTitle.text = spec.title
            dialogMessage.text = spec.message
            dialogPrimary.text = spec.primaryLabel
            dialogSecondary.text = spec.secondaryLabel
            if (!spec.primaryFullWidth) {
                dialogPrimary.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                }
                dialogPrimary.minWidth = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._169sdp)
            }
            dialogPrimary.onClick {
                dialog.dismiss()
                onPrimary()
            }
            dialogSecondary.onClick {
                dialog.dismiss()
                onSecondary()
            }
        }
        dialog.setOnCancelListener { onCancel() }

        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(spec.scrim.dimAmount)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attributes = attributes.apply {
                    blurBehindRadius = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
                }
            }
        }
        dialog.show()
        return dialog
    }
}
