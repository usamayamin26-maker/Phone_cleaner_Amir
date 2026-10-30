# CLAUDE.md

## Rules

- Har prompt ke aakhir mein natija docs/last-prompt-result.md mein likho — har dafa ISI file ko
  overwrite karo. Date wali ya nayi file kabhi nahi banani, purani reports archive nahi karni.
- Commit: message mein Claude / AI ka koi zikr nahi (na "Co-Authored-By", na "Generated with",
  na link). Author meri git config wala, badalna nahi. Har commit ke baad `git log -1` dikhao.
- Push kabhi nahi, jab tak main na kahun.
- Code se pehle chhota plan; "go" par code.

Read "Project guidelines" and "Reference implementation" at the end of this file before any work and
follow them strictly. docs/project-guidelines.md is only the short app overview (features).

This app is built the way UniversalRemoteControl is: one `:app` module, layer-first
(com.phone.cleaner: di/, utils/, data/{local,repository,mapper}, domain/{entities,repository,
view_models}, presentation/), plus one Android library module per feature under package
`ai.jhw.<feature>` (:storage, :junkcleaner, :aicleaner, :whatsapp, :duplicates, :appmanager,
:deviceinfo, :battery, :appusage, :compressor). Library modules hold the device work and no UI;
the app wraps them in repositories that return ApiResult<T>. The module table and each module's
inner layout are in "Project guidelines" below.

The reference for porting a feature is the old PhoneCleanerMM source (Compose, package
com.mna.cleaner.junk.phonecleaner). Take its logic, never its structure or its UI: no use case
classes in the app, no Compose, no Navigation Component, no Hilt.

There is no core package: base classes live in presentation/base/, shared custom views in
presentation/components/.

XML layouts with ViewBinding, Kotlin, Koin, kotlinx.serialization and Room. Standalone full
screens are Activities; fragments only where a fragment is the right tool (a page inside a screen,
a reusable screen part). No Navigation Component. Fragments live in their screen-area package
(presentation/home, presentation/deviceinfo, presentation/battery, ...) and extend BaseFragment<VB>
from presentation/base (ViewBinding, binding nulled in onDestroyView, collectState/collectEvents
with viewLifecycleOwner + repeatOnLifecycle(STARTED)).

No network client: there is no backend. Add data/remote ("Reference implementation" below, copied as-is) only
when a feature needs one, and ask first.

This app deletes the user's files. Nothing is deleted without the design's "Are You Sure?"
confirmation, scanning never deletes, and a test run on the owner's phone never deletes, moves or
compresses a file it did not create ("Project guidelines" below, "Deleting files" and "Device
testing").

Inner screens use one shared back button (res/layout/view_back_button.xml, built with the first
inner screen from the design) - never a new or circular variant. Include it with its own id; the
include's root takes the click, the spoken label and expandTouchTarget().

Inner screen header: always res/layout/view_inner_header.xml, identical on every screen - same title
font and size (never restyled from code), a 14sdp start/end gutter, directly under the status bar
(no extra top margin on the include), and outside any scroll view so it never moves when the
content changes (scan -> results). Screen content uses the same 14sdp side gutter.

UI rules in short: sdp for every dimension and ssp for every text size and line height; a box
around text is minHeight/minWidth, never a fixed size (no dimens.xml, no hardcoded
dp/sp); Figma px -> sdp/ssp: round(px * 300 / 375) — never copy Figma px values 1:1 (the
frames are 375 wide: the 3x exports are 1125 px; confirm in Figma before the first screen);
every color named in values/colors.xml (light only - no values-night); no hardcoded
user-visible text outside values/strings.xml (translated into all 18 languages); no type.xml and no style per view; drawables named shape_ / img_ / ic_; fonts in
res/font and Lottie JSON in res/raw. Theme parent is Theme.Material3.Light.NoActionBar: the app is light
only, whatever the phone's setting.

Localization: the same 18 languages as UniversalRemoteControl, done the same way - en (default) plus
es, fr, de, pt, it, ja, ko, zh, ar, hi, ru, tr, nl, pl, sv, nb, da: a values-<tag>/strings.xml per
language with every string translated, res/xml/locales_config.xml listing all 18 and
android:localeConfig on the application, the stored language applied in BaseApp (already there),
and a Language screen with the AppLanguage list (tag, flag emoji, native and English name string)
as in that project. Every new string is added to all 18 files in the same task - never English-only.
Arabic is right to left, so every screen is checked in Arabic.

Names: the app (launcher label, app_name) is "AI Phone Cleaner"; the feature is "AI Junk Cleaner"
everywhere (Home quick action, onboarding, the cleaner's screen header) and only the splash keeps
"Ai Junk Cleaner"; the Home header reads "Smart Cleaner Pro". Four separate strings.

Brand names are never translated: "AI Phone Cleaner", "AI Junk Cleaner", the splash's "Ai Junk
Cleaner" and "Smart Cleaner Pro" stay in English in all 18 languages. They are `translatable="false"` in values/strings.xml and do
not appear in any values-<tag>/strings.xml. Feature names (Device Info, Battery Info, Delete
Duplicates, ...) are translated like any other text.


Screens that exist in the design keep the gutter measured from Figma; screens with no design
frame use a 14sdp gutter.

## UI checklist (every screen)

Work through this list for every new layout and every new state, and record in the task report that
it passes.

### Sizes

- Convert every Figma value: `sdp` / `ssp` = `round(FigmaPx * 300 / 375)` - the design frames
  are 375 wide (confirm in Figma before the first screen). Never copy a Figma px value 1:1.

### Typography

- Every `TextView` names its own weight file - `@font/roboto_regular` (400), `@font/roboto_medium`
  (500), `@font/roboto_semi_bold` (600), `@font/roboto_bold` (700) - matching the Figma
  `fontWeight`. The design uses Roboto throughout. The theme sets no font family.
- Every `TextView` sets `android:textColor` to a color resource. Nothing relies on a theme default.
- `letterSpacing`, `textAllCaps` and line height are copied from Figma. Line height is set through
  `app:lineHeight` only, never `android:lineHeight`.
- Line height is `ssp` (`@dimen/_16ssp`), never `sdp`, `dp` or a `lineSpacingMultiplier`. The text
  is `ssp` and follows the user's font size; an `sdp` line box does not, so at font size 2.0 the
  lines of a paragraph overlap. `_Nssp` and `_Nsdp` hold the same number, so the Figma pitch at the
  default font size is unchanged. A unit test fails on anything else (added with the first
  screen). Measurements are in "Project guidelines" below.
- Gradient text is drawn with a gradient span (add a shared component the first time a design needs
  it). Two-tone text keeps the whole sentence in one string plus a
  highlight string, and applies the span in code.

### Colors

- Every color — text, icon tint, fill, stroke, selected and pressed states — is a named resource
  read from Figma and named by its role (`text_primary`, `card_surface`), never by its value
  ("white", "purple"). Selected and pressed states get their own resources.
- The app is light only: there is no dark design and none is built. No values-night/ folder, no
  mode checks in code.
- Gradient angle and direction come from the Figma gradient handles. Diagonal is common, so never
  assume left to right.
- Layers with opacity 0 or `visible: false` are not drawn. Use a transparent color resource when the
  name still has to exist.

### Layout

- Reproduce the Figma composition — what is centred on what, the overlaps, the offsets. Do not
  restack elements into a more convenient chain.
- System bars: top content gets `applyStatusBarTopMargin()` plus the Figma gap; bottom calls to
  action and the bottom navigation get `applyNavigationBarBottomMargin()`. Backgrounds and glows may
  draw behind the bars.
- Text next to a trailing view (radio, arrow, switch) uses `layout_width="0dp"`, is constrained to
  that view's start with the Figma gap, and sets `maxLines` with `ellipsize="end"`. Nothing overlaps.
- Right to left: `start` / `end` only, never `left` / `right`. A `TextView` showing user content or
  another language sets `android:textDirection="locale"` and `android:textAlignment="viewStart"`.
  Directional icons set `android:autoMirrored="true"`.
- Elevated surfaces — search fields, cards, sheets — are a `MaterialCardView` with the Figma radius,
  elevation, fill and stroke, never a flat shape drawable where the design shows elevation.
- No layout-editor artifacts: no constraint bias and no absolute positions that the design does not
  have.
- Touch targets are at least 48dp, and every clickable item has a ripple or pressed state.
- Every screen survives font size 2.0. There are no fixed sizes around text: use `wrap_content` +
  `minHeight`/`minWidth`, and only icon tiles are fixed. `maxLines`/`ellipsize` apply only to user
  content (file, folder and app names), never to the app's own wording. An emoji used as an icon
  is `sdp` + `importantForAccessibility="no"`. Compositions are checked on the emulator at
  1.0 / 1.3 / 2.0.

### Resources

- No hardcoded user-visible text anywhere.
- Resource names keep the `shape_` / `img_` / `ic_` / `anim_` prefixes.
- Icons come out of Figma as SVG, are converted to `ic_*` vector drawables and are tinted with color
  resources.

### Verification before reporting

- Run the screen on the test phone (a Pixel 6 Pro emulator if none is attached), screenshot it
  (with the phone itself in dark mode too, to prove the app stays light), and compare
  each against the Figma export side by side. List every remaining difference with its reason.
- Check the screen once with the app in Arabic (right to left) and once on an API 26 emulator at
  font size 2.0.
- Go through this checklist item by item for each new layout and state, and state the result in the
  task report.

Design sources, in this order:
1. Figma, file "Phone-Cleaner-JH",
   key br4oFzSKn2rE3cmrmTsdYk
   (https://www.figma.com/design/br4oFzSKn2rE3cmrmTsdYk/Phone-Cleaner-JH). Frame node ids:
   - inner-screen background: 1-4211
   - splash: 1-4189
   - onboarding: 1-3705 and 1-3962 (slides 3 and 4; slides 1 and 2 share their layout)
   - home: 1-2286 (superseded by home v2)
   - home v2: 34-8 - the current Home. Fetched on its own into
     ~/Downloads/phone_cleaner_ui/figma-home-v2.json (nodes endpoint, ids=34:8); its visual
     reference is ~/Downloads/phone_cleaner_ui/home_v2.png, exported at 4x (1500 px wide), not 3x
   - battery info: 1-2023
   - device info: 1-1896 (list) and 1-1979 (Device Information detail)
   - onboarding slides 1 and 2: 1-2896 and 1-3178 (their 375 x 420 phone images: 1-3054, 1-3459,
     1-3853, 1-4123 for slides 1 to 4)
   - settings: 1-4213; language list: 1-4281 (its header still reads "Settings")
   - rate-us dialog (not built; Rate Us opens the Play Store): 1-4356 and 1-4395
   - permission dialog ("Permission is required", over an older Home variant 1-2684): 1-2884
   - app cleaner (section 1-1777): list 1-1778 (app_cleaner_1), uninstall dialog frame 1-1829
     (app_cleaner_2) with the dialog itself 1-1881
   - delete duplicates (section 1-482): scanning 1-483 (delete_duplicate_1), results 1-528 (_2),
     Images detail 1-661 (_3), 1-827 (_4), 1-738 (_5), "Are You Sure?" frame 1-910 (_6) with the
     dialog 1-984
   - ai junk cleaner (section 1-2): scanning 1-3, results 1-349, Images 1-48, Gallery 1-142 and
     1-208, "Are You Sure?" frame 1-263 with the dialog 1-337
   - wa cleaner (section 1-996): scanning 1-997, results 1-1042, Files 1-1160 and 1-1250, Gallery
     1-1526 and 1-1592, "Are You Sure?" frames 1-1354 and 1-1440 with the dialogs 1-1428 and 1-1514
   - scan now (section 1-1647): 1-1648 and 1-1705 ("Device is fully cleaned")
   Other screens: find them in the file by the export names below and add their ids here.
   Figma calls are scarce: the account is on the free Starter plan, where the MCP and the REST
   API each allow only a handful of calls a month. So:
   - The whole design is downloaded ONCE, with a single REST request for the full file, into
     ~/Downloads/phone_cleaner_ui/figma-nodes.json (outside the repo, never deleted):
     GET https://api.figma.com/v1/files/br4oFzSKn2rE3cmrmTsdYk with header
     X-Figma-Token: $FIGMA_TOKEN (set in ~/.zshenv; never print it). It holds every frame, so
     every later session finds frames by name and reads sizes, spacing, colours, radii, shadows,
     fonts and line heights from that file - no further calls.
   - A new Figma request only when I say the design changed. Never one request per frame.
   - No image export calls. Assets come from ~/Downloads/phone_cleaner_ui/resources/; anything
     missing there is listed in the report for the owner to export by hand.
   - The PNG screenshots are the visual reference for every screen. When a value is not in
     figma-nodes.json and no call is left, take it from the PNG and flag it as unverified.
2. ~/Downloads/phone_cleaner_ui/*.png - exported screens at 3x, the reference for side-by-side
   comparison. File names say which screen: home (old), home_v2 (4x), splash, boarding_1..4, setting, device_info_1..2,
   batter_info_1..2, app_cleaner_1..2, junk_cleaner_1..5, wa_cleaner_1..7, delete_duplicate_1..6.
3. ~/Downloads/phone_cleaner_ui/resources/ - exported assets (ic_*.svg, img_*.png). SVGs become
   ic_* vector drawables; PNGs go to drawable-xxhdpi (or WebP) under their img_* names. Anything
   the screen needs that is not there is exported from Figma through the MCP, never redrawn.

Logging is android.util.Log with a private TAG constant per class, in the app and in every library
module. No logging library.

Git commits must contain no Claude attribution of any kind: no Co-Authored-By, no
"Generated with Claude Code", no Claude-Session trailer, no Claude or Claude Code author, and no
mention of Claude or Claude Code anywhere in the message. Plain commit messages only, made as the
repository's own git user.

At the end of every task, overwrite docs/last-prompt-result.md with that task's full final report,
replacing the previous content instead of appending.

docs/last-prompt-result.md is local only (.gitignore). Never stage or commit it.

docs/ holds only: project-guidelines.md (short app overview), PhoneCleanerAds.json (the Remote
Config ads value, equal to AppAds.DEFAULT_JSON) and last-prompt-result.md — no new doc files. The
house rules, the reference code and the release checklist are the last sections of this file.

---

## Project guidelines

House rules for this repository. Read this together with "Reference implementation" below, which holds the
canonical network layer, repository and ViewModel reference code, and follow both strictly. The
same rules run the UniversalRemoteControl project; only the parts about televisions were replaced.

### Modules

`:app` is one module, organised layer-first. Every feature's device work lives in its own Android
library module, one per feature, package `ai.jhw.<feature>`:

| Module | Package | Feature |
|---|---|---|
| `:storage` | `ai.jhw.storage` | Shared file layer: MediaStore queries, sizes, delete flow, large files |
| `:junkcleaner` | `ai.jhw.junkcleaner` | Smart Scan ("Scan Now"): cache, residual, empty folders, APK (Android Package), trash |
| `:aicleaner` | `ai.jhw.aicleaner` | AI (Artificial Intelligence) Junk Cleaner: LiteRT (TensorFlow Lite) classifier |
| `:whatsapp` | `ai.jhw.whatsapp` | WA (WhatsApp) Cleaner: WhatsApp and WhatsApp Business media |
| `:duplicates` | `ai.jhw.duplicates` | Delete Duplicates: hash matching, keep best one |
| `:appmanager` | `ai.jhw.appmanager` | App Cleaner: installed apps, uninstall |
| `:deviceinfo` | `ai.jhw.deviceinfo` | Device, Processor, WiFi, Display, RAM, ROM, Camera information |
| `:battery` | `ai.jhw.battery` | Battery Info: health, temperature, voltage, capacity, last full charge |
| `:appusage` | `ai.jhw.appusage` | App Usage Report and Power Usage (UsageStatsManager) |
| `:compressor` | `ai.jhw.compressor` | Photo Compressor |

Each library module looks the same inside:

```
ai/jhw/<feature>/
  di/<Feature>Module.kt      val <feature>Module = module { ... }
  model/                     plain data classes the module returns
  domain/repository/         the module's repository interface
  repo/                      its *RepositoryImpl
  utils/                     helpers private to the module
```

- A library module has no UI: no activity, fragment, layout, ViewModel or string the user reads.
  It returns data and plain results; the app turns them into text.
- A library module declares the permissions and `<queries>` its work needs in its own
  `AndroidManifest.xml`; the manifest merger brings them into the app.
- Cleaner modules depend on `:storage` for file access and the delete flow; no module depends on
  another feature module.
- Each module registers its own singles in its Koin module, assembled only in `BaseApp`.
- Use case classes are allowed inside a library module where the source had one; the app itself
  has none.

### Package layout

The app package `com.phone.cleaner` is organised layer-first, never feature-first:

```
di/SharedModule.kt
utils/
data/{local,repository,mapper}      data/remote only if a backend is added
domain/{entities,repository,view_models}
presentation/{base,components,adapters,onboarding,home,deviceinfo,battery,junkcleaner,aicleaner,
              whatsapp,duplicates,appmanager,appusage,compressor,settings}
```

There is no `core` package. Base classes such as `BaseActivity` and `BaseFragment` live in
`presentation/base/`, and shared custom views live in `presentation/components/`.

- FCM (Firebase Cloud Messaging) lives in `messaging/`: `AppFirebaseMessagingService` and
  `NotificationChannels`, as in UniversalRemoteControl. Firebase Analytics and Crashlytics need no
  code: the plugins and the BoM in `:app` set them up. The applicationId is `com.jhw.aicleaner`
  (it must match `app/google-services.json`); the Kotlin namespace stays `com.phone.cleaner`.
- Room lives in `data/local/`: `AppDatabase.kt`, `dao/`, `entities/`.
- Repository interfaces live in `domain/repository/`; their `*RepositoryImp` implementations live in
  `data/repository/` and wrap the feature library modules (and the DAOs (Data Access Objects)).
- ViewModels and their UiState classes live in `domain/view_models/`.
- Never create feature-first packages: no `*_feature` packages, no per-feature
  data/domain/presentation trees inside `:app`.
- The root package contains only `BaseApp.kt`. Every activity lives under `presentation/` —
  `MainActivity` directly in `presentation/`, all others in their screen-area sub-package.
- Fragments live in the screen-area package they belong to, never in a shared `fragments` package.
- The first-run screens (splash, language, onboarding) live in `presentation/onboarding/`, as in
  UniversalRemoteControl. The language screen is opened from Settings too and stays there.
- Keep a `.gitkeep` only in empty leaf packages; delete it as soon as the package has real files.

### Preferences — utils/SharedPref.kt

One class in `utils/` with a `private val sharedPref` and a `private val prefEdit`, and one Kotlin
property per key:

```kotlin
var lastFullChargeAt: Long
    get() = sharedPref.getLong("lastFullChargeAt", 0L)
    set(value) = prefEdit.putLong("lastFullChargeAt", value).apply()
```

Never add generic `put*`/`get*` helper methods.

### JSON — utils/SerializingHelper.kt

kotlinx.serialization only. `SerializingHelper` (`getJson` / `toJson` / `fromJson`) holds the only
`Json` instance in the app. Never construct `Json { }` anywhere else. `fromJson` and `toJson` return
`null` on failure and never throw.

### Extensions — utils/Extensions.kt

One file with top-level extension functions, grouped by receiver (String / View / Long / Context).
No `utils/extensions/` subpackage and no per-receiver files.

### Dependency injection

Exactly one app-level Koin module: `di/SharedModule.kt`, declaring `val SharedModule`. Inside it,
per feature and grouped together:

- repositories as `single<Interface> { Imp() }`
- ViewModels via `viewModel { }`
- adapters via `factory { }`

The library modules (`storageModule`, `batteryModule`, ...) stay in their own libraries and are only
assembled in the application class.

### Repositories and networking

There is no backend today, so there is no `data/remote` package and no Ktor dependency. When one is
needed, add it through `data/remote/NetworkClient`, exactly as written in "Reference implementation" below.

Repositories wrap the feature library modules and the DAOs and return `ApiResult<T>`
(`ApiResult.Success` / `ApiResult.Failure`), the same shape the network code uses. ViewModels use
repositories only. No use case classes in the app and no separate DTO (Data Transfer Object)
classes.

### ViewModel pattern

- An `XUiState` data class held in a `MutableStateFlow`, exposed with `asStateFlow()`.
- One-shot effects (toast, navigation, dialogs) go through a `SharedFlow<XUiEvent>` alongside the
  state.
- Cancel the previous `Job` before starting a new load. A scan the user stops ("Stop Scanning")
  cancels its job; a cancelled scan deletes nothing.
- Activities collect both with `repeatOnLifecycle(STARTED)`.

**Never emit a one-shot event from `onStart`.** `collectEvents` subscribes from
`lifecycleScope.launch { repeatOnLifecycle(STARTED) { ... } }`, which only reaches `collect` after
`onStart` has returned, and the event flows have `replay = 0`, so anything emitted during startup is
delivered to nobody and silently dropped. Work that must happen on every entry to a screen (asking
for storage or usage access) is driven from the activity or fragment itself, behind its own latch,
rather than round-tripped through a one-shot event. State is not affected — a `StateFlow` holds its
value, so a collector that subscribes late still sees it.

### Deleting files

This app deletes the user's files, so every delete path follows the same rules:

- Nothing is deleted without the "Are You Sure?" confirmation from the design, and the dialog names
  what is about to go (count and size).
- Scanning never deletes. "Scanning your device safely — nothing is deleted yet" has to stay true.
- Deletes go through `:storage` only. Media the app does not own goes through
  `MediaStore.createDeleteRequest` (API 30+) where All files access is not granted.
- After a delete, the AI scan cache (`AiScanResultDao.deleteByPaths`) and any other cache that
  named those files is updated in the same repository call.

### UI

XML layouts with ViewBinding and a Material3 theme. No Navigation Component and no Compose.

Activities and fragments are split by role:

- A standalone full screen is an activity extending `BaseActivity<VB>` — splash, language,
  onboarding, and every feature screen opened from Home (Device Info, Battery Info, WA Cleaner,
  Delete Duplicates, ...).
- A fragment is used where a fragment is the right tool: a tab of the bottom navigation, a page
  inside a screen, or a reusable part of a screen. Fragments extend `BaseFragment<VB>` from
  `presentation/base/`, which inflates the ViewBinding, nulls it in `onDestroyView` and collects
  state and events with `viewLifecycleOwner` + `repeatOnLifecycle(STARTED)`.
- `MainActivity` hosts Home. If a bottom navigation is added later, it hosts the tabs itself,
  without the Navigation Component: it adds all tab fragments once, in one transaction, and
  switches between them with `show()` / `hide()`, so each tab keeps its views, its scroll position
  and its ViewModel. `hide()` does not stop a fragment: a tab that holds a live subscription (a
  running scan) stops it in `onHiddenChanged`.
- Inner screens use the shared back button (`res/layout/view_back_button.xml`) — never a new or
  circular variant. Include it with its own id; the include's root is what takes the click, the
  spoken label and `expandTouchTarget()`.

#### Theme

The theme parent is `Theme.Material3.Light.NoActionBar`. The app is light only - there is no dark
design and none is built - so it stays light whatever the phone's setting. There is no
`values-night/` folder.

#### Sizes and text sizes

Use `sdp` (`com.intuit.sdp:sdp-android`) for every dimension and `ssp`
(`com.intuit.ssp:ssp-android`) for every text size, so layouts scale across screen sizes. There is
no `res/values/dimens.xml` — never create one, and never hardcode `dp` or `sp` in a layout.

A line height belongs to the text, not to the layout: `app:lineHeight` is always `ssp`, never `sdp`.
See the Typography rule in the UI checklist below for why.

Both libraries scale against a 300dp-wide baseline: `_100sdp` is 100dp on a 300dp-wide screen but
130dp on a 390dp-wide one. A value copied straight out of Figma therefore renders too large by the
ratio between the design frame and 300. Always convert:

```
Figma px -> sdp/ssp: round(px * 300 / designFrameWidth)
```

Never copy Figma px values 1:1. The PhoneCleaner design draws its screens in 375px-wide frames
(the 3x exports are 1125 px wide; confirm in Figma), so the factor is `300 / 375`: a 24px gap is
`@dimen/_19sdp` and a 20px title is `@dimen/_16ssp`. Round to the nearest whole number, with a floor of 1; keep a 0 as 0, and keep
hairline (1px) strokes at `@dimen/_1sdp` so borders stay visible.

Screens that exist in the design keep the gutter measured from Figma; screens with no design
frame use a 14sdp gutter.

#### Colors

Every color used in the designs gets a named entry in `res/values/colors.xml`, named by its role.
Never write a raw hex value in a layout, a style or code.

#### Strings and languages

Localization: the same 18 languages as UniversalRemoteControl, done the same way - en (default) plus
es, fr, de, pt, it, ja, ko, zh, ar, hi, ru, tr, nl, pl, sv, nb, da: a values-<tag>/strings.xml per
language with every string translated, res/xml/locales_config.xml listing all 18 and
android:localeConfig on the application, the stored language applied in BaseApp (already there),
and a Language screen with the AppLanguage list (tag, flag emoji, native and English name string)
as in that project. Every new string is added to all 18 files in the same task - never English-only.
Arabic is right to left, so every screen is checked in Arabic.

Names: the app (launcher label, app_name) is "AI Phone Cleaner"; the feature is "AI Junk Cleaner"
everywhere (Home quick action, onboarding, the cleaner's screen header) and only the splash keeps
"Ai Junk Cleaner"; the Home header reads "Smart Cleaner Pro". Four separate strings.

Brand names are never translated: "AI Phone Cleaner", "AI Junk Cleaner", the splash's "Ai Junk
Cleaner" and "Smart Cleaner Pro" stay in English in all 18 languages. They are `translatable="false"` in values/strings.xml and do
not appear in any values-<tag>/strings.xml. Feature names (Device Info, Battery Info, Delete
Duplicates, ...) are translated like any other text.

Never hardcode user-visible text in a layout or in code. Every such string lives in
`res/values/strings.xml`, with its translation in every `values-<tag>/strings.xml`.

Where the design paints part of a sentence differently - a different colour, weight or a gradient -
keep the whole sentence in one string and put the highlighted part in a second string, then apply
the span in code with `TextView.setGradientHighlight` / `setColorHighlight` from `utils/Extensions`.
Never split the sentence into two views or hardcode the highlighted words.

#### Styles

There is no `res/values/type.xml`. Do not create a style per view; add a style only for a pattern
that repeats many times. Keep `res/values/shapes.xml` only for as long as the theme actually
references it.

#### Resource naming and assets

- Drawables: `shape_` for XML shape drawables, `img_` for PNG or WEBP images, `ic_` for vector
  icons.
- Fonts go in `res/font`, one file per weight: `roboto_regular`, `roboto_medium`,
  `roboto_semi_bold`, `roboto_bold`. Every `TextView` names the concrete weight file the design
  asks for, for example `android:fontFamily="@font/roboto_semi_bold"`.
  The theme deliberately sets no `android:fontFamily`: a theme-level font family overrides the one
  a view sets for itself, which silently renders every label in one weight.
- Lottie JSON animations go in `res/raw`, played with `com.airbnb.android:lottie`.


### UI checklist (every screen)

Work through this list for every new layout and every new state, and record in the task report that
it passes.

#### Sizes

- Convert every Figma value: `sdp` / `ssp` = `round(FigmaPx * 300 / 375)`. Never copy a Figma px
  value 1:1.

#### Typography

- Every `TextView` names its own weight file - `@font/roboto_regular` (400), `@font/roboto_medium`
  (500), `@font/roboto_semi_bold` (600), `@font/roboto_bold` (700) - matching the Figma
  `fontWeight`. The design uses Roboto throughout. The theme sets no font family.
- Every `TextView` sets `android:textColor` to a color resource. Nothing relies on a theme default.
- `letterSpacing`, `textAllCaps` and line height are copied from Figma. Line height is set through
  `app:lineHeight` only, never `android:lineHeight`.
- Line height is `ssp`: `app:lineHeight="@dimen/_16ssp"`, never `@dimen/_16sdp`, never `dp`, and never
  a `lineSpacingMultiplier` in its place. Convert it with the same `round(FigmaPx * 300 / 375)` as
  the text size.
  - **Why not sdp.** The text is `ssp`, which follows the user's font size (Settings > Display >
    Font size, up to 2.0 on a Pixel). `sdp` does not. With an `sdp` line height the glyphs grow and
    the line box stays put, so the lines of a paragraph overlap. Measured on this app before the
    fix: every `sdp` line height overlapped its own lines at 2.0 (by up to 46 px on API 26). On
    API 37, the three `_14sdp` ones already overlapped at 1.3.
  - **Why ssp keeps the design exact.** `_Nssp` and `_Nsdp` hold the same number in every
    screen-width bucket. The only difference is the unit (`sp` against `dp`), and an `sp` is a `dp`
    at the default font size. Measured: all 43 line heights kept their pixel pitch at font scale
    1.0 when moved from `sdp` to `ssp`. AppCompat keeps the unit of `app:lineHeight`, so on API 34+
    the platform also scales an `sp` line height in proportion to the non-linearly scaled text.
    Pitch/text ratio at 2.0 stayed within 0.986 to 1.018 of the ratio at 1.0.
  - **Why not a multiplier.** `lineSpacingMultiplier` scales the font's own line metrics, not the
    Figma line height. A two-decimal multiplier fitted to each view's current pitch was measured
    1 to 8 px off at font scale 1.0 on every one of the 43 views.
  - A unit test that fails on any line height that is not `sp` is added with the first screen.
- Gradient text is drawn with a gradient span (add a shared component the first time a design needs
  it). Two-tone text keeps the whole sentence in one string plus a
  highlight string, and applies the span in code.

#### Colors

- Every color — text, icon tint, fill, stroke, selected and pressed states — is a named resource
  read from Figma and named by its role (`text_primary`, `card_surface`), never by its value
  ("white", "purple"). Selected and pressed states get their own resources.
- The app is light only: there is no dark design and none is built. No values-night/ folder, no
  mode checks in code.
- Gradient angle and direction come from the Figma gradient handles. Diagonal is common, so never
  assume left to right.
- Layers with opacity 0 or `visible: false` are not drawn. Use a transparent color resource when the
  name still has to exist.

#### Layout

- Reproduce the Figma composition — what is centred on what, the overlaps, the offsets. Do not
  restack elements into a more convenient chain.
- System bars: top content gets `applyStatusBarTopMargin()` plus the Figma gap; bottom calls to
  action and the bottom navigation get `applyNavigationBarBottomMargin()`. Backgrounds and glows may
  draw behind the bars.
- Text next to a trailing view (radio, arrow, switch) uses `layout_width="0dp"`, is constrained to
  that view's start with the Figma gap, and sets `maxLines` with `ellipsize="end"`. Nothing overlaps.
- Right to left: `start` / `end` only, never `left` / `right`. A `TextView` showing user content or
  another language sets `android:textDirection="locale"` and `android:textAlignment="viewStart"`.
  Directional icons set `android:autoMirrored="true"`.
- Elevated surfaces — search fields, cards, sheets — are a `MaterialCardView` with the Figma radius,
  elevation, fill and stroke, never a flat shape drawable where the design shows elevation.
- No layout-editor artifacts: no constraint bias and no absolute positions that the design does not
  have.
- Touch targets are at least 48dp, and every clickable item has a ripple or pressed state.

#### Resources

- No hardcoded user-visible text anywhere.
- Resource names keep the `shape_` / `img_` / `ic_` / `anim_` prefixes.
- Icons come out of Figma as SVG, are converted to `ic_*` vector drawables and are tinted with color
  resources.

#### Verification before reporting

- Run the screen on the test phone (a Pixel 6 Pro emulator if none is attached), screenshot it
  (with the phone itself in dark mode too, to prove the app stays light), and compare
  each against the Figma export side by side. List every remaining difference with its reason.
- Check the screen once with the app in a right-to-left language (Arabic) and once on an API 26
  emulator at font size 2.0.
- Go through this checklist item by item for each new layout and state, and state the result in the
  task report.


### Device testing

Screens are checked on a physical phone that belongs to the project owner and carries their own
photos, messages and documents. Everything on it that was not put there for the test run is
off limits. A cleaner app makes this stricter than usual: a test run must never delete, move or
compress a file it did not create.

- Every scan, delete and compress test runs against test files generated for the run (images,
  videos, duplicate copies, a fake WhatsApp media folder, junk folders) and copied to the device.
  They are deleted again afterwards.
- Never press a Delete, Clean, Compress or Uninstall button while the list on screen contains a
  single item that was not created for the run. Check with a screenshot first.
- Read any device setting before changing it, write the value down, and put that value back when
  the run ends: night mode, animation scales, the locale, Do Not Disturb, the font scale, power
  saving. Never change a setting whose original value has not been recorded.

  ```
  adb shell settings get secure ui_night_mode      # 0 auto, 1 light, 2 dark
  adb shell settings get global window_animation_scale
  adb shell settings get system font_scale
  adb shell settings get global zen_mode           # 0 off, 1 priority, 2 none, 3 alarms
  ```

  Restore every one of them even when the run ends badly. If a value was not recorded and cannot
  be recovered, say so in the task report and name the setting.
- Turn Do Not Disturb on before the run (`adb shell cmd notification set_dnd priority`) and put it
  back afterwards, so a notification can never appear over the app under test.
- If a picker, gallery grid or file list puts personal media on screen, leave it immediately. Do
  not screenshot it, do not keep a screenshot already taken, do not describe what was in it, and
  record in the task report that it happened.
- Stop and report rather than carrying on when the debug connection drops, a tap lands outside the
  app under test, or the owner picks the device up mid-run.
- Never write, overwrite or delete any file on the phone outside a path created for the run.
  Temporary files (UI dumps, logs, probes) go to a fresh unique path under /data/local/tmp/ (for
  example /data/local/tmp/pc_<timestamp>/), checked to be absent before the first write and removed
  at the end - never /sdcard/ui.xml or any other fixed name on shared storage. On 2026-10-27 a UI
  dump to /sdcard/ui.xml overwrote and then deleted a file of that name that was already there.
- Logcat filters are narrow enough to log no installed-app names, account names or file names of
  the owner's data. If something personal slips into a log, delete those lines and the local copy
  at once, clear the phone's log, and say so in the report.
- Never queue a tap before a screenshot has confirmed what is actually on screen. One
  `adb shell input tap` per screenshot, and the screenshot comes first.
- Change the app's language through its own language screen, never with
  `adb shell cmd locale set-app-locales`: `BaseApp.applyStoredLanguage()` reverts it on every
  process start.

### Persistence and logging

Room is in use for the local cache (`data/local`, schemas exported to `app/schemas`). Every table
can be rebuilt by scanning again. Bump the database version and add a migration with every schema
change. Small values go in `SharedPref`; kotlinx.serialization goes through `SerializingHelper`.

Logging uses `android.util.Log` — in the app and in every library module. Each class declares its
own private `TAG` constant and logs through it. Do not add a logging library.

### Ads

AdMob with mediation, driven by one Firebase Remote Config JSON parameter, **`PhoneCleanerAds`**.
The package is `app/src/main/java/com/phone/cleaner/ads/` - UniversalRemoteControl's `ads/` bundle,
the same file for file. **Only `AppAds.kt` is this app's**: the parameter name and the default JSON.
Nothing else in the package is edited per app, and no screen calls a loader or a manager directly -
`AdsController` is the one entry point. How the package works and how to use it from a screen:
`app/src/main/java/com/phone/cleaner/ads/README.md`.

**The one rule: consent -> SDK -> config -> any ad.** `AdsBootstrap` owns that order, and
`AdsController.slot()` waits on it, so nothing is requested before UMP (User Messaging Platform)
consent has settled. `AdsController.init` runs in `BaseApp`; `AdsController.start` runs in
`SplashActivity`, because the consent form needs an Activity.

- **Inline slots.** Every screen has a top and a bottom slot, and a mid slot where it scrolls past
  two blocks of content (Home, Settings, Scan Now, a cleaner's results, Device Information, Battery
  Info). A slot is an `AdSlotView` in the screen's XML with constraints and **no `layout_margin*`**:
  the gutter and the vertical gap belong to the slot, not the host. Each slot reads
  `<screen>_<position>_banner` (0 off, 1..5 banner modes) and `_native` (0 off, 1..5 layouts); the
  native wins if both are on, and both at 0 collapses the slot so the screen measures as it did
  before. Bound from `onResume`, forgotten in `onDestroy` / `onDestroyView`.
- **Full screen.** The splash ad (`splash_ad_preference`, loaded behind a Continue button and never
  shown on its own), an entry interstitial on every button that opens a screen
  (`is_show_admob_<screen>_btn_inter`), a back interstitial on every inner screen
  (`is_show_admob_<screen>_back_inter`), the onboarding full-screen native, and the resume app open
  ad on a real return from background.
- **Counters.** `counter_quick_action` (Home's Scan Now and the six quick action cards),
  `counter_screen_buttons` (a button that opens a screen from an inner screen) and
  `counter_back_btn`. 0 never shows, 1 shows on every qualifying click, N on every Nth; a missing
  key reads as 1 and counters are never persisted.
- **Where a full-screen ad must never appear.** No full-screen ad while a cleaner's scan is
  running: a scanning page binds inline slots only, and its Stop Scanning and its back go straight
  through. Nothing opens over the "Are You Sure?" delete dialog or over a system consent dialog -
  the delete flow asks for no ad at all, and the launch-flow interstitials are the only ones posted
  behind a frame.
- **This app deletes files, so an ad never sits between the user and a delete.** The ad is on the
  way *in* to a screen and on the way *out* of it, never on Delete, Clean or Uninstall.
- **Ads are not live yet:** the manifest carries Google's test AdMob application id and every unit
  id in the JSON is blank, so the app requests nothing but Google's test units (which
  `AdUnitIds` substitutes in debug builds anyway). The real app id and unit ids are item 3 of
  "Release checklist" below.

### Remote Config JSON files

| File | What it is |
|---|---|
| `PhoneCleanerAds.json` | The console value of the `PhoneCleanerAds` parameter, equal to `AppAds.DEFAULT_JSON`. Every flag 0 and every unit id blank, so a build that never reaches the console shows no ad. A key added on one side only is a key that reads as off. |

Numbers in the console are unquoted: `"home_top_banner": 1`, never `"1"`. A quoted number on a
screen key reads as its default (off); on one of the package's typed keys it is a parse error and
**every ad in the app goes off**, with one line under `RemoteConfigManager`.

### Other

- In-app updates (Google Play): `in_app_update_type` in the ads JSON - 0 off, 1 flexible, 2
  immediate. It ships at 0. `utils/InAppUpdateManager.kt`, one instance per `MainActivity`.
- Firebase: Analytics, Crashlytics, Remote Config, Cloud Messaging.

### Writing conventions

- Spell out an abbreviation in full the first time it appears in a comment or document, for example
  RAM (Random Access Memory).
- Commit messages: plain descriptive text only — no Co-Authored-By trailers, no tool-generated
  footers, no session links and no mention of Claude or Claude Code anywhere in a commit.

---

## Reference implementation — network layer, repository and ViewModel pattern

This is the canonical code every project of ours follows. Copy it as-is; the only Android
adaptations allowed are: Ktor `OkHttp` engine, `android.util.Log` instead of `println`, and Koin
bindings.
Do NOT rename methods or classes (`makeNetworkRequest`, `ApiResult.Success/Failure`, `ApiData`).

### data/remote/NetworkUtil.kt
```kotlin
object NetworkUtil {
    const val BASE_URL = "https://example.com" // TODO real backend, if/when one exists
}
```

### data/remote/RequestType.kt
```kotlin
sealed class RequestType(val link: String) {
    data class Get(val url: String) : RequestType(link = url)
    data class Post(val url: String, val body: Any?) : RequestType(link = url)
    data class Delete(val url: String) : RequestType(link = url)
    data class DeleteWithBody(val url: String, val body: Any?) : RequestType(link = url)
    data class UploadFile(
        val url: String,
        val fileBytes: ByteArray,
        val fileName: String,
        val fileParamName: String = "file",
        val formData: Map<String, String> = emptyMap()
    ) : RequestType(link = url)
    data class Multipart(
        val url: String,
        val fields: Map<String, String>,
        val files: Map<String, Pair<ByteArray, String>?> = emptyMap()
    ) : RequestType(link = url)
}
```

### data/remote/SessionManager.kt
```kotlin
sealed interface SessionEvent {
    data object Unauthorized : SessionEvent
}

class SessionManager {
    private val _events = MutableSharedFlow<SessionEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    private var alreadyTriggered = false

    suspend fun notifyUnauthorized() {
        if (alreadyTriggered) return
        alreadyTriggered = true
        _events.emit(SessionEvent.Unauthorized)
    }

    fun reset() {
        alreadyTriggered = false
    }
}
```

### data/remote/ApiData.kt
```kotlin
@Serializable
data class ApiData<T : Any>(
    val status: Boolean = false,
    val message: String = "",
    val data: T? = null,
)
```

### data/remote/ApiResult.kt
```kotlin
sealed class ApiResult<T>(
    val data: T? = null,
    val message: String? = null,
) {
    class Success<T>(data: T) : ApiResult<T>(data = data)
    class Failure<T>(message: String) : ApiResult<T>(message = message)
}
```

### data/remote/NetworkClient.kt
```kotlin
class NetworkClient(val sessionManager: SessionManager) {
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(SerializingHelper.getJson())
        }
        val timeOut = 600_000L
        install(HttpTimeout) {
            requestTimeoutMillis = timeOut
            connectTimeoutMillis = timeOut
            socketTimeoutMillis = timeOut
        }
    }
    suspend inline fun <reified T> makeNetworkRequest(
        requestType: RequestType,
        headers: Map<String, String>? = null,
    ): ApiResult<T> {
        return withContext(Dispatchers.Default) {
            try {
                logIt("----------------------------------------------")
                logIt("Network Hit Url : ${requestType.link}")
                logIt("Network Hit Headers : $headers")
                val httpResponse = requestType.getHttpBuilder(headers)
                val statusCode = httpResponse.status.value
                val response: String = httpResponse.body()
                val newResponse: T = if (T::class == String::class) {
                    response as T
                } else {
                    SerializingHelper.getJson().decodeFromString(response)
                }
                logIt("Network Response-1:$response")
                logIt("Network Response-2:$newResponse")
                if (statusCode == 401) sessionManager.notifyUnauthorized()
                ApiResult.Success(newResponse)
            } catch (e: ClientRequestException) {
                val status = e.response.status.value
                if (status == 401) sessionManager.notifyUnauthorized()
                logEx("ClientRequestException: Status: ${e.response.status} Message: ${e.message}")
                ApiResult.Failure(e.message)
            } catch (e: ServerResponseException) {
                logEx("ServerResponseException:Status: ${e.response.status} Message: ${e.message}")
                ApiResult.Failure(e.message)
            } catch (e: Exception) {
                logEx("Network Exception:${e.message}")
                ApiResult.Failure(e.message ?: "Unknown error")
            }
        }
    }
    suspend fun RequestType.getHttpBuilder(
        headers: Map<String, String>? = null
    ): HttpResponse {
        return when (this) {
            is RequestType.Get -> {
                httpClient.get(url) {
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                }
            }
            is RequestType.Post -> {
                httpClient.post(url) {
                    header("Content-Type", "application/json")
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                    setBody((this@getHttpBuilder).body)
                }
            }
            is RequestType.Delete -> {
                httpClient.delete(url) {
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                }
            }
            is RequestType.DeleteWithBody -> {
                httpClient.delete(url) {
                    header("Content-Type", "application/json")
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                    setBody((this@getHttpBuilder).body)
                }
            }
            is RequestType.UploadFile -> {
                httpClient.post(url) {
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    key = fileParamName,
                                    value = fileBytes,
                                    headers = Headers.build {
                                        append(
                                            "Content-Disposition",
                                            "form-data; name=\"$fileParamName\"; filename=\"$fileName\""
                                        )
                                        append("Content-Type", "application/octet-stream")
                                    }
                                )
                                formData.forEach { (key, value) ->
                                    append(key, value)
                                }
                            }
                        ))
                }
            }
            is RequestType.Multipart -> {
                httpClient.post(url) {
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                fields.forEach { (key, value) ->
                                    append(key, value)
                                }
                                files.forEach { (key, pair) ->
                                    pair?.let { (bytes, name) ->
                                        append(
                                            key = key,
                                            value = bytes,
                                            headers = Headers.build {
                                                append(
                                                    "Content-Disposition",
                                                    "form-data; name=\"$key\"; filename=\"$name\""
                                                )
                                                append("Content-Type", "application/octet-stream")
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    )
                }
            }
        }
    }
    fun logIt(msg: String) {
        println("[NetworkClient] $msg")
    }
    fun logEx(msg: String) {
        println("[NetworkClient ERROR] $msg")
    }
}
```

### Repository pattern — domain/repository (interface) + data/repository (Imp)
```kotlin
interface SubResidentRepository {
    suspend fun getAllSubResidents(token: String): ApiResult<List<SubResident>>
    suspend fun addSubResident(token: String, subResident: SubResidentRequest): ApiResult<SubResident>
}

class SubResidentRepositoryImp(private val networkClient: NetworkClient) : SubResidentRepository {

    override suspend fun getAllSubResidents(token: String): ApiResult<List<SubResident>> {
        val url = "${NetworkUtil.BASE_URL}/api/manager/sub-residents"
        val header = mapOf("Authorization" to "Bearer $token")

        val response = networkClient.makeNetworkRequest<ApiData<List<SubResident>>>(
            requestType = RequestType.Get(url), headers = header
        )

        return when (response) {
            is ApiResult.Failure -> ApiResult.Failure(response.message ?: "Something went wrong")
            is ApiResult.Success -> {
                response.data?.let {
                    if (it.status) {
                        val data = it.data ?: return ApiResult.Failure("No data received, try again")
                        ApiResult.Success(data)
                    } else {
                        ApiResult.Failure(it.message)
                    }
                } ?: ApiResult.Failure("No data received, try again")
            }
        }
    }

    override suspend fun addSubResident(token: String, subResident: SubResidentRequest): ApiResult<SubResident> {
        val url = "${NetworkUtil.BASE_URL}/api/manager/sub-residents"
        val header = mapOf("Authorization" to "Bearer $token")

        val response = networkClient.makeNetworkRequest<ApiData<SubResident>>(
            requestType = RequestType.Post(url, subResident), headers = header
        )

        return when (response) {
            is ApiResult.Failure -> ApiResult.Failure(response.message ?: "Something went wrong")
            is ApiResult.Success -> {
                response.data?.let {
                    if (it.status) {
                        val data = it.data ?: return ApiResult.Failure("No data received, try again")
                        ApiResult.Success(data)
                    } else {
                        ApiResult.Failure(it.message)
                    }
                } ?: ApiResult.Failure("No data received, try again")
            }
        }
    }
}
```
The repeated `when (response)` unwrap may be extracted into a shared helper
(e.g. `ApiResult<ApiData<T>>.unwrap()`) — behaviour must stay identical.

PhoneCleaner has no backend today: there is no `data/remote` package yet. The network code above
is the reference for the day one is added, and is then copied as-is.

Repositories that talk to the device follow the same shape but wrap the feature library modules
(`:storage`, `:battery`, `:deviceinfo`, ...) and the Room DAOs instead of `NetworkClient`, and
return `ApiResult<T>` too:

```kotlin
interface BatteryRepository {
    suspend fun getBatteryInfo(): ApiResult<BatteryInfo>
}

class BatteryRepositoryImp(
    private val battery: ai.jhw.battery.domain.repository.BatteryRepository,
    private val sharedPref: SharedPref,
) : BatteryRepository {

    override suspend fun getBatteryInfo(): ApiResult<BatteryInfo> = try {
        ApiResult.Success(battery.read().toEntity(sharedPref.lastFullChargeAt))
    } catch (e: Exception) {
        Log.e(TAG, "Battery read failed", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }

    private companion object {
        const val TAG = "BatteryRepositoryImp"
    }
}
```

Without `data/remote`, `ApiResult` lives in `domain/entities/ApiResult.kt`; it moves to
`data/remote/` unchanged when the network layer arrives.

### Room — data/local

```kotlin
@Entity(tableName = "ai_scan_results")
data class AiScanResultEntity(
    @PrimaryKey val path: String,
    val dateModified: Long,
    val isJunk: Boolean,
)

@Dao
interface AiScanResultDao {
    @Query("SELECT * FROM ai_scan_results")
    suspend fun getAll(): List<AiScanResultEntity>

    @Upsert
    suspend fun upsertAll(results: List<AiScanResultEntity>)
}

@Database(entities = [AiScanResultEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aiScanResultDao(): AiScanResultDao
}
```

The database and every DAO are `single { }` in `SharedModule`. DAOs are used by repositories only,
never by a ViewModel or an activity.

### ViewModel pattern — domain/view_models
```kotlin
data class SubResidentListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val subResidentList: List<SubResident> = emptyList(),
)

class SubResidentListViewModel(
    private val repository: SubResidentRepository,
    private val sharedPref: SharedPref,
) : ViewModel() {

    private val _state = MutableStateFlow(SubResidentListUiState())
    val state = _state.asStateFlow()

    private var job: Job? = null

    init {
        getAllSubResidents()
    }

    fun getAllSubResidents() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            val result = repository.getAllSubResidents(sharedPref.token)
            _state.value = state.value.copy(
                isLoading = false,
                error = result.message,
                subResidentList = result.data ?: emptyList()
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
```
One-shot effects (toast, navigation, show the delete confirmation) go through a
`SharedFlow<XUiEvent>` next to the state; activities collect both with `repeatOnLifecycle(STARTED)`.

---

## Release checklist - pending

Everything the app needs before the first Play Store release, beyond the code. Tick an item when
it is done and say where it landed.

| # | Item | Waiting on | Notes |
|---|---|---|---|
| 1 | Privacy policy URL | company | `privacy_policy_url` in values/strings.xml is still `PLACEHOLDER_PRIVACY_URL`. The policy must say every scan and the AI model run on the phone and no file or data leaves it. Play requires the link. |
| 2 | App icon | designer | Adaptive icon (separate foreground and background layers) and the 512 x 512 Play Store icon. The launcher icon is still Android Studio's default. |
| 3 | Real AdMob app id and unit ids | AdMob console | The `ads/` package is wired and driven by the `PhoneCleanerAds` Remote Config parameter (`docs/PhoneCleanerAds.json`). Still to come: the real AdMob application id in `AndroidManifest.xml` (it holds Google's test id) and the real unit ids in the console value (every `admob_*_id` is blank), plus each mediation network's ad source per unit - Yandex goes in as a custom event, see `ads/README.md`. Then turn the placements on one at a time from the console. |
| 4 | Release keystore and signing | owner | Same pattern as UniversalRemoteControl (`jks/`, passwords in `keystore.properties`, never committed). Then an R8 run of the real `release` build; `staging` already passes. |
| 5 | Play Console permission declarations and Data safety | to be written | All files access (MANAGE_EXTERNAL_STORAGE), Usage access (PACKAGE_USAGE_STATS), REQUEST_DELETE_PACKAGES; Data safety: no data collected or shared, all processing on device. |
| 6 | Translations review | native speakers | 17 languages were machine-written by the developer tools; brand names stay English. |
