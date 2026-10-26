package com.phone.cleaner.presentation.components

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ViewRateUsDialogBinding
import com.phone.cleaner.presentation.components.AppDialog.applyDialogWindow
import com.phone.cleaner.utils.expandTouchTarget
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.onClick

/**
 * Rate Us (Figma 1:4356 / 1:4395): an emoji, the thanks, five stars, "Done" and "Cancel". Done is
 * enabled once a star is chosen and hands the rating to [onRated]; what follows is the screen's
 * decision. The emoji turns to heart eyes, in its yellow ring, at four or five stars.
 */
object RateUsDialog {

    private const val STARS = 5
    private const val HAPPY_FROM = 4
    private const val DISABLED_ALPHA = 0.5f

    fun show(context: Context, onRated: (stars: Int) -> Unit): Dialog {
        val binding = ViewRateUsDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context)
        dialog.setContentView(binding.root)

        val stars = with(binding) { listOf(rateStar1, rateStar2, rateStar3, rateStar4, rateStar5) }
        var rating = 0

        fun render() = with(binding) {
            stars.forEachIndexed { index, star ->
                val chosen = index < rating
                star.setImageResource(if (chosen) R.drawable.ic_star_rate else R.drawable.ic_star_rate_empty)
                ViewCompat.setStateDescription(
                    star,
                    context.getString(if (chosen) R.string.state_selected else R.string.state_not_selected),
                )
            }
            val happy = rating >= HAPPY_FROM
            rateEmoji.setText(if (happy) R.string.rate_emoji_love else R.string.rate_emoji_grin)
            rateEmojiRing.setBackgroundResource(if (happy) R.drawable.shape_rate_emoji_ring else 0)
            rateDone.isEnabled = rating > 0
            rateDone.alpha = if (rateDone.isEnabled) 1f else DISABLED_ALPHA
        }

        stars.forEachIndexed { index, star ->
            star.contentDescription = context.getString(
                R.string.rate_star_description,
                context.formatNumber(index + 1),
                context.formatNumber(STARS),
            )
            star.onClick(intervalMs = 0) {
                rating = index + 1
                render()
            }
            star.expandTouchTarget()
        }
        binding.rateDone.onClick {
            dialog.dismiss()
            onRated(rating)
        }
        binding.rateCancel.onClick { dialog.dismiss() }
        render()

        dialog.applyDialogWindow(AppDialog.Scrim.LIGHT)
        dialog.show()
        return dialog
    }
}
