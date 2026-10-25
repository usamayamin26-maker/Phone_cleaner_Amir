package com.phone.cleaner.domain.entities

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.phone.cleaner.R

/**
 * The four onboarding pages, in the design's order. Each names a feature: the titles are the
 * same strings Home uses for it, so a renamed feature is renamed in both places.
 */
enum class OnboardingPage(
    @param:StringRes val title: Int,
    @param:StringRes val subtitle: Int,
    @param:DrawableRes val image: Int,
) {
    JUNK_CLEANER(R.string.action_ai_junk_cleaner, R.string.onboarding_junk_subtitle, R.drawable.img_boarding_1),
    DUPLICATES(R.string.action_delete_duplicates, R.string.onboarding_duplicates_subtitle, R.drawable.img_boarding_2),
    WHATSAPP(R.string.onboarding_whatsapp_title, R.string.onboarding_whatsapp_subtitle, R.drawable.img_boarding_3),
    BATTERY(R.string.battery_info_title, R.string.onboarding_battery_subtitle, R.drawable.img_boarding_4),
}
