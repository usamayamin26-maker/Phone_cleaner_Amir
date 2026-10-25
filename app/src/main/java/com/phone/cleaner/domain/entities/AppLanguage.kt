package com.phone.cleaner.domain.entities

import androidx.annotation.StringRes
import com.phone.cleaner.R

/**
 * One row of the Language screen, as in UniversalRemoteControl.
 *
 * [tag] is a BCP 47 (Best Current Practice 47) language tag: the value stored in
 * `SharedPref.appLanguage` and handed to LocaleManager / AppCompatDelegate. [nativeName] is the
 * language's name in itself and is never translated; [englishName] is its name in the app's
 * current language. The flags are emoji, one mechanism for all eighteen.
 */
data class AppLanguage(
    val tag: String,
    val flag: String,
    @param:StringRes val nativeName: Int,
    @param:StringRes val englishName: Int,
) {
    companion object {
        val ALL: List<AppLanguage> = listOf(
            AppLanguage("en", "🇺🇸", R.string.lang_en_native, R.string.lang_en_english),
            AppLanguage("es", "🇪🇸", R.string.lang_es_native, R.string.lang_es_english),
            AppLanguage("fr", "🇫🇷", R.string.lang_fr_native, R.string.lang_fr_english),
            AppLanguage("de", "🇩🇪", R.string.lang_de_native, R.string.lang_de_english),
            AppLanguage("pt", "🇵🇹", R.string.lang_pt_native, R.string.lang_pt_english),
            AppLanguage("it", "🇮🇹", R.string.lang_it_native, R.string.lang_it_english),
            AppLanguage("ja", "🇯🇵", R.string.lang_ja_native, R.string.lang_ja_english),
            AppLanguage("ko", "🇰🇷", R.string.lang_ko_native, R.string.lang_ko_english),
            AppLanguage("zh", "🇨🇳", R.string.lang_zh_native, R.string.lang_zh_english),
            AppLanguage("ar", "🇸🇦", R.string.lang_ar_native, R.string.lang_ar_english),
            AppLanguage("hi", "🇮🇳", R.string.lang_hi_native, R.string.lang_hi_english),
            AppLanguage("ru", "🇷🇺", R.string.lang_ru_native, R.string.lang_ru_english),
            AppLanguage("tr", "🇹🇷", R.string.lang_tr_native, R.string.lang_tr_english),
            AppLanguage("nl", "🇳🇱", R.string.lang_nl_native, R.string.lang_nl_english),
            AppLanguage("pl", "🇵🇱", R.string.lang_pl_native, R.string.lang_pl_english),
            AppLanguage("sv", "🇸🇪", R.string.lang_sv_native, R.string.lang_sv_english),
            AppLanguage("nb", "🇳🇴", R.string.lang_nb_native, R.string.lang_nb_english),
            AppLanguage("da", "🇩🇰", R.string.lang_da_native, R.string.lang_da_english),
        )
    }
}
