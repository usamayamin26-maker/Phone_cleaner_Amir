# Release checklist - pending

Everything the app needs before the first Play Store release, beyond the code. Tick an item when
it is done and say where it landed.

| # | Item | Waiting on | Notes |
|---|---|---|---|
| 1 | Privacy policy URL | company | `privacy_policy_url` in values/strings.xml is still `PLACEHOLDER_PRIVACY_URL`. The policy must say every scan and the AI model run on the phone and no file or data leaves it. Play requires the link. |
| 2 | App icon | designer | Adaptive icon (separate foreground and background layers) and the 512 x 512 Play Store icon. The launcher icon is still Android Studio's default. |
| 3 | Ads (Firebase done) | ad unit IDs, placements | Firebase is wired: google-services.json for `com.jhw.aicleaner`, Analytics, Crashlytics, FCM. Still to do: AdMob + Remote Config, reusing UniversalRemoteControl's `ads/` package. |
| 4 | Release keystore and signing | owner | Same pattern as UniversalRemoteControl (`jks/`, passwords in `keystore.properties`, never committed). Then an R8 run of the real `release` build; `staging` already passes. |
| 5 | Play Console permission declarations and Data safety | to be written | All files access (MANAGE_EXTERNAL_STORAGE), Usage access (PACKAGE_USAGE_STATS), REQUEST_DELETE_PACKAGES; Data safety: no data collected or shared, all processing on device. |
| 6 | Translations review | native speakers | 17 languages were machine-written by the developer tools; brand names stay English. |
