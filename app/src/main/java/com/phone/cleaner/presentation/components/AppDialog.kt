package com.phone.cleaner.presentation.components

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import com.phone.cleaner.R
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

    enum class Variant {
        /** "Are You Sure?" and "Permission is required": a full-width 18 px bold button. */
        STANDARD,

        /**
         * The uninstall dialog (1:1881): the app's icon, its message 3 px under the title on a
         * 15.5 px line, and a 211 px centred button with a 16 px medium label.
         */
        APP,
    }

    class Spec(
        val title: CharSequence,
        val message: CharSequence,
        val primaryLabel: CharSequence,
        val secondaryLabel: CharSequence,
        val icon: Drawable? = null,
        val variant: Variant = Variant.STANDARD,
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
            if (spec.variant == Variant.APP) {
                dialogMessage.updateLayoutParams<LinearLayout.LayoutParams> {
                    topMargin = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)
                }
                TextViewCompat.setLineHeight(
                    dialogMessage,
                    context.resources.getDimensionPixelSize(com.intuit.ssp.R.dimen._12ssp),
                )
                dialogPrimary.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                }
                dialogPrimary.minWidth = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._169sdp)
                dialogPrimary.typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
                dialogPrimary.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    context.resources.getDimension(com.intuit.ssp.R.dimen._13ssp),
                )
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
