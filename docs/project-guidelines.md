# Phone Cleaner

Android app in Kotlin with XML Views + ViewBinding (not Compose), Koin, Room,
kotlinx.serialization. Package `com.jhw.aicleaner`, minSdk 24. 18 languages. No backend: every
scan and the AI model run on the phone.

The device work lives in one library module per feature (package `ai.jhw.<feature>`); the app
wraps them in repositories. House rules, UI checklist, reference code and the release checklist
are in `CLAUDE.md`.

## App flow

```
Splash → (Language) → (Onboarding) → Main: Home
```

- **Splash** — optional full-screen ad behind a Continue button, then on.
- **Language** and **Onboarding** — first run; later launches follow Remote Config.

## Features

| Screen | What it does | Module |
|---|---|---|
| Home | Scan Now, six quick action cards, storage summary | — |
| Scan Now (Smart Scan) | Cache, residual files, empty folders, APK (Android Package) files, trash | `:junkcleaner` |
| AI Junk Cleaner | On-device LiteRT (TensorFlow Lite) classifier for junk photos | `:aicleaner` |
| WA (WhatsApp) Cleaner | WhatsApp and WhatsApp Business media | `:whatsapp` |
| Delete Duplicates | Hash matching, keeps the best copy | `:duplicates` |
| App Cleaner | Installed apps, uninstall | `:appmanager` |
| Photo Compressor | Compresses photos | `:compressor` |
| Device Information | Device, processor, Wi-Fi, display, RAM, ROM, camera | `:deviceinfo` |
| Battery Info | Health, temperature, voltage, capacity, last full charge | `:battery` |
| App Usage / Power Usage | Usage report from UsageStatsManager | `:appusage` |
| Settings | Language, theme and app links | — |

Shared file access and the delete flow are in `:storage`. Nothing is deleted without the
"Are You Sure?" confirmation, and scanning never deletes.

## Ads

AdMob with mediation, driven by one Remote Config JSON parameter, **`PhoneCleanerAds`**. Screens
have top / bottom slots (mid where a screen scrolls past two blocks), each showing a banner
(5 types) or a native ad (5 layouts), native wins if both are on. Full-screen: splash, entry and
back interstitials with click counters, onboarding full-screen native, resume app open. Never an
ad between the user and a delete. Nothing is requested before UMP (User Messaging Platform)
consent. The manifest has the real AdMob app id and the JSON the real unit ids: the launch flow
(splash, language, onboarding) has its own units, and from Home on every screen uses the shared
`admob_app_*_id`.

How the package works and how to use it: `app/src/main/java/com/phone/cleaner/ads/README.md`.

## Remote Config JSON files

| File | What it is |
|---|---|
| `PhoneCleanerAds.json` | Parameter `PhoneCleanerAds`, equal to `AppAds.DEFAULT_JSON` (every flag 0, the real unit ids) |

## Other

- In-app updates (Google Play): `in_app_update_type` in the ads JSON — 0 off, 1 flexible, 2 immediate (ships at 0).
- Firebase: Analytics, Crashlytics, Remote Config, Cloud Messaging.
