package com.phone.cleaner.ads

/**
 * AI Phone Cleaner's side of the shared ads package (`ads/`, the same in every project): the Remote
 * Config parameter and its default JSON, handed to `AdsController.init` in
 * [com.phone.cleaner.BaseApp].
 *
 * [DEFAULT_JSON] lists **every** ads key the app reads; debug builds log any key a screen reads that
 * is not here (`AdsConfig: Unknown ads key …`), which is what catches a typo. Every flag is 0 and
 * every unit id blank, so a build that never reaches Remote Config shows no ad at all. Adding a
 * screen or a placement = adding its keys here and to docs/PhoneCleanerAds.json, which is the value
 * the console holds.
 *
 * Key shapes: `<screen>_<top|mid|bottom>_banner` / `_native` (slots; banner 0..5, native 0..5),
 * `is_show_*` (full-screen placements), `admob_<units>_banner_id` / `_native_id` (per-screen units,
 * blank = the shared `admob_app_*_id`), `counter_*` (how often each group of buttons shows an
 * interstitial).
 *
 * **Screens and their keys.** One screen key per screen, and the cleaners count as three screens
 * each, because the scanning page, the results and a section's page are separate fragments with
 * separate slots and their own entries in the ad caches:
 *
 * | Screen key | Where | Slots |
 * | --- | --- | --- |
 * | `splash` | [com.phone.cleaner.presentation.onboarding.SplashActivity] | top, bottom |
 * | `language` | [com.phone.cleaner.presentation.onboarding.LanguageActivity] | top, bottom |
 * | `onboarding1`..`onboarding4` | [com.phone.cleaner.presentation.onboarding.OnboardingActivity] | top, bottom |
 * | `home` | [com.phone.cleaner.presentation.MainActivity] | top, mid, bottom |
 * | `settings` | [com.phone.cleaner.presentation.settings.SettingsActivity] | top, mid, bottom |
 * | `scan_now` | [com.phone.cleaner.presentation.junkcleaner.ScanNowActivity] | top, mid, bottom |
 * | `ai_cleaner_scan` / `_results` / `_groups` / `_detail` | AI Junk Cleaner's four pages | results also mid |
 * | `wa_cleaner_scan` / `_results` / `_groups` / `_detail` | WA Cleaner's four pages | results also mid |
 * | `duplicates_scan` / `_results` / `_groups` / `_detail` | Delete Duplicates' four pages | results also mid |
 * | `app_cleaner` | [com.phone.cleaner.presentation.appmanager.AppCleanerActivity] | top, bottom |
 * | `device_info` | [com.phone.cleaner.presentation.deviceinfo.DeviceInfoActivity] | top, bottom |
 * | `device_info_detail` | [com.phone.cleaner.presentation.deviceinfo.DeviceInfoDetailActivity] | top, mid, bottom |
 * | `battery_info` | [com.phone.cleaner.presentation.battery.BatteryInfoActivity] | top, mid, bottom |
 * | `exit` | the exit sheet on Home's back | bottom |
 *
 * A **mid slot only where the screen scrolls past two blocks of content** - Home, Settings, Scan
 * Now, a cleaner's results, Device Information and Battery Info. The list screens (Language, App
 * Cleaner, Device Info, a cleaner's pages) have nothing to put a slot between, and the scanning
 * pages are one centred block with "Stop Scanning" under it.
 *
 * **Unit groups** (`units =` on a slot call) are `splash`, `language`, `onboarding`, `home`,
 * `settings`, `scan_now`, `ai_cleaner`, `wa_cleaner`, `duplicates`, `app_cleaner`, `device_info`,
 * `device_info_detail` and `battery_info`; a cleaner's four pages share its group. The exit sheet
 * passes no group and serves from the shared `admob_app_*_id`.
 */
object AppAds {

    const val REMOTE_CONFIG_KEY = "PhoneCleanerAds"

    const val DEFAULT_JSON = """
        {
          "in_app_update_type": 0,
          "is_show_admob_resume_app_open": 0,
          "counter_quick_action": 1,
          "counter_screen_buttons": 1,
          "counter_back_btn": 1,
          "splash_ad_preference": 0,
          "is_show_admob_splash_app_open": 0,
          "is_show_admob_splash_inter": 0,
          "splash_top_banner": 0,
          "splash_top_native": 0,
          "splash_bottom_banner": 0,
          "splash_bottom_native": 0,
          "is_show_language_screen": 0,
          "is_show_language_screen_first_session": 1,
          "language_top_banner": 0,
          "language_top_native": 0,
          "language_bottom_banner": 0,
          "language_bottom_native": 0,
          "is_show_admob_language_done_btn_inter": 0,
          "is_show_language_btn_inter": 0,
          "is_show_language_back_inter": 0,
          "is_show_onboarding_screen": 0,
          "is_show_onboarding_screen_first_session": 1,
          "is_show_admob_onboarding_skip_btn_inter": 0,
          "is_show_admob_onboarding_full_screen_native": 0,
          "is_show_admob_onboarding_get_started_inter": 0,
          "onboarding1_top_banner": 0,
          "onboarding1_top_native": 0,
          "onboarding1_bottom_banner": 0,
          "onboarding1_bottom_native": 0,
          "onboarding2_top_banner": 0,
          "onboarding2_top_native": 0,
          "onboarding2_bottom_banner": 0,
          "onboarding2_bottom_native": 0,
          "onboarding3_top_banner": 0,
          "onboarding3_top_native": 0,
          "onboarding3_bottom_banner": 0,
          "onboarding3_bottom_native": 0,
          "onboarding4_top_banner": 0,
          "onboarding4_top_native": 0,
          "onboarding4_bottom_banner": 0,
          "onboarding4_bottom_native": 0,
          "home_top_banner": 0,
          "home_top_native": 0,
          "home_mid_banner": 0,
          "home_mid_native": 0,
          "home_bottom_banner": 0,
          "home_bottom_native": 0,
          "settings_top_banner": 0,
          "settings_top_native": 0,
          "settings_mid_banner": 0,
          "settings_mid_native": 0,
          "settings_bottom_banner": 0,
          "settings_bottom_native": 0,
          "is_show_settings_btn_inter": 0,
          "is_show_settings_back_inter": 0,
          "scan_now_top_banner": 0,
          "scan_now_top_native": 0,
          "scan_now_mid_banner": 0,
          "scan_now_mid_native": 0,
          "scan_now_bottom_banner": 0,
          "scan_now_bottom_native": 0,
          "is_show_scan_now_btn_inter": 0,
          "is_show_scan_now_back_inter": 0,
          "ai_cleaner_scan_top_banner": 0,
          "ai_cleaner_scan_top_native": 0,
          "ai_cleaner_scan_bottom_banner": 0,
          "ai_cleaner_scan_bottom_native": 0,
          "ai_cleaner_results_top_banner": 0,
          "ai_cleaner_results_top_native": 0,
          "ai_cleaner_results_mid_banner": 0,
          "ai_cleaner_results_mid_native": 0,
          "ai_cleaner_results_bottom_banner": 0,
          "ai_cleaner_results_bottom_native": 0,
          "ai_cleaner_groups_top_banner": 0,
          "ai_cleaner_groups_top_native": 0,
          "ai_cleaner_groups_bottom_banner": 0,
          "ai_cleaner_groups_bottom_native": 0,
          "ai_cleaner_detail_top_banner": 0,
          "ai_cleaner_detail_top_native": 0,
          "ai_cleaner_detail_bottom_banner": 0,
          "ai_cleaner_detail_bottom_native": 0,
          "is_show_ai_cleaner_btn_inter": 0,
          "is_show_ai_cleaner_back_inter": 0,
          "is_show_ai_cleaner_detail_btn_inter": 0,
          "is_show_ai_cleaner_detail_back_inter": 0,
          "wa_cleaner_scan_top_banner": 0,
          "wa_cleaner_scan_top_native": 0,
          "wa_cleaner_scan_bottom_banner": 0,
          "wa_cleaner_scan_bottom_native": 0,
          "wa_cleaner_results_top_banner": 0,
          "wa_cleaner_results_top_native": 0,
          "wa_cleaner_results_mid_banner": 0,
          "wa_cleaner_results_mid_native": 0,
          "wa_cleaner_results_bottom_banner": 0,
          "wa_cleaner_results_bottom_native": 0,
          "wa_cleaner_groups_top_banner": 0,
          "wa_cleaner_groups_top_native": 0,
          "wa_cleaner_groups_bottom_banner": 0,
          "wa_cleaner_groups_bottom_native": 0,
          "wa_cleaner_detail_top_banner": 0,
          "wa_cleaner_detail_top_native": 0,
          "wa_cleaner_detail_bottom_banner": 0,
          "wa_cleaner_detail_bottom_native": 0,
          "is_show_wa_cleaner_btn_inter": 0,
          "is_show_wa_cleaner_back_inter": 0,
          "is_show_wa_cleaner_detail_btn_inter": 0,
          "is_show_wa_cleaner_detail_back_inter": 0,
          "duplicates_scan_top_banner": 0,
          "duplicates_scan_top_native": 0,
          "duplicates_scan_bottom_banner": 0,
          "duplicates_scan_bottom_native": 0,
          "duplicates_results_top_banner": 0,
          "duplicates_results_top_native": 0,
          "duplicates_results_mid_banner": 0,
          "duplicates_results_mid_native": 0,
          "duplicates_results_bottom_banner": 0,
          "duplicates_results_bottom_native": 0,
          "duplicates_groups_top_banner": 0,
          "duplicates_groups_top_native": 0,
          "duplicates_groups_bottom_banner": 0,
          "duplicates_groups_bottom_native": 0,
          "duplicates_detail_top_banner": 0,
          "duplicates_detail_top_native": 0,
          "duplicates_detail_bottom_banner": 0,
          "duplicates_detail_bottom_native": 0,
          "is_show_duplicates_btn_inter": 0,
          "is_show_duplicates_back_inter": 0,
          "is_show_duplicates_detail_btn_inter": 0,
          "is_show_duplicates_detail_back_inter": 0,
          "app_cleaner_top_banner": 0,
          "app_cleaner_top_native": 0,
          "app_cleaner_bottom_banner": 0,
          "app_cleaner_bottom_native": 0,
          "is_show_app_cleaner_btn_inter": 0,
          "is_show_app_cleaner_back_inter": 0,
          "device_info_top_banner": 0,
          "device_info_top_native": 0,
          "device_info_bottom_banner": 0,
          "device_info_bottom_native": 0,
          "is_show_device_info_btn_inter": 0,
          "is_show_device_info_back_inter": 0,
          "device_info_detail_top_banner": 0,
          "device_info_detail_top_native": 0,
          "device_info_detail_mid_banner": 0,
          "device_info_detail_mid_native": 0,
          "device_info_detail_bottom_banner": 0,
          "device_info_detail_bottom_native": 0,
          "is_show_device_info_detail_btn_inter": 0,
          "is_show_device_info_detail_back_inter": 0,
          "battery_info_top_banner": 0,
          "battery_info_top_native": 0,
          "battery_info_mid_banner": 0,
          "battery_info_mid_native": 0,
          "battery_info_bottom_banner": 0,
          "battery_info_bottom_native": 0,
          "is_show_battery_info_btn_inter": 0,
          "is_show_battery_info_back_inter": 0,
          "exit_bottom_banner": 0,
          "exit_bottom_native": 0,
          "native_ad_tag_text_color": "",
          "native_ad_tag_bg_color": "",
          "native_ad_tag_border_color": "",
          "native_heading_text_color": "",
          "native_description_text_color": "",
          "native_button_text_color": "",
          "native_button_bg_color": "",
          "native_ad_bg_color": "",
          "native_ad_border_color": "",
          "app_id": "",
          "admob_app_banner_id": "",
          "admob_app_native_id": "",
          "admob_app_inter_id": "",
          "admob_resume_app_open_id": "",
          "admob_splash_banner_id": "",
          "admob_splash_native_id": "",
          "admob_splash_app_open_id": "",
          "admob_splash_inter_id": "",
          "admob_language_banner_id": "",
          "admob_language_native_id": "",
          "admob_language_done_btn_inter_id": "",
          "admob_onboarding_banner_id": "",
          "admob_onboarding_native_id": "",
          "admob_onboarding_full_screen_native_id": "",
          "admob_onboarding_get_started_inter_id": "",
          "admob_home_banner_id": "",
          "admob_home_native_id": "",
          "admob_settings_banner_id": "",
          "admob_settings_native_id": "",
          "admob_scan_now_banner_id": "",
          "admob_scan_now_native_id": "",
          "admob_ai_cleaner_banner_id": "",
          "admob_ai_cleaner_native_id": "",
          "admob_wa_cleaner_banner_id": "",
          "admob_wa_cleaner_native_id": "",
          "admob_duplicates_banner_id": "",
          "admob_duplicates_native_id": "",
          "admob_app_cleaner_banner_id": "",
          "admob_app_cleaner_native_id": "",
          "admob_device_info_banner_id": "",
          "admob_device_info_native_id": "",
          "admob_device_info_detail_banner_id": "",
          "admob_device_info_detail_native_id": "",
          "admob_battery_info_banner_id": "",
          "admob_battery_info_native_id": ""
        }
    """
}
