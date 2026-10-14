# Project guidelines

House rules for this repository. Read this together with `docs/architecture.md`, which holds the
canonical network layer, repository and ViewModel reference code, and follow both strictly. The
same rules run the UniversalRemoteControl project; only the parts about televisions were replaced.

## Modules

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

## Package layout

The app package `com.phone.cleaner` is organised layer-first, never feature-first:

```
di/SharedModule.kt
utils/
data/{local,repository,mapper}      data/remote only if a backend is added
domain/{entities,repository,view_models}
presentation/{base,components,adapters,home,deviceinfo,battery,junkcleaner,aicleaner,whatsapp,
              duplicates,appmanager,appusage,compressor,settings}
```

There is no `core` package. Base classes such as `BaseActivity` and `BaseFragment` live in
`presentation/base/`, and shared custom views live in `presentation/components/`.

- Room lives in `data/local/`: `AppDatabase.kt`, `dao/`, `entities/`.
- Repository interfaces live in `domain/repository/`; their `*RepositoryImp` implementations live in
  `data/repository/` and wrap the feature library modules (and the DAOs (Data Access Objects)).
- ViewModels and their UiState classes live in `domain/view_models/`.
- Never create feature-first packages: no `*_feature` packages, no per-feature
  data/domain/presentation trees inside `:app`.
- The root package contains only `BaseApp.kt`. Every activity lives under `presentation/` —
  `MainActivity` directly in `presentation/`, all others in their screen-area sub-package.
- Fragments live in the screen-area package they belong to, never in a shared `fragments` package.
- Keep a `.gitkeep` only in empty leaf packages; delete it as soon as the package has real files.

## Preferences — utils/SharedPref.kt

One class in `utils/` with a `private val sharedPref` and a `private val prefEdit`, and one Kotlin
property per key:

```kotlin
var lastFullChargeAt: Long
    get() = sharedPref.getLong("lastFullChargeAt", 0L)
    set(value) = prefEdit.putLong("lastFullChargeAt", value).apply()
```

Never add generic `put*`/`get*` helper methods.

## JSON — utils/SerializingHelper.kt

kotlinx.serialization only. `SerializingHelper` (`getJson` / `toJson` / `fromJson`) holds the only
`Json` instance in the app. Never construct `Json { }` anywhere else. `fromJson` and `toJson` return
`null` on failure and never throw.

## Extensions — utils/Extensions.kt

One file with top-level extension functions, grouped by receiver (String / View / Long / Context).
No `utils/extensions/` subpackage and no per-receiver files.

## Dependency injection

Exactly one app-level Koin module: `di/SharedModule.kt`, declaring `val SharedModule`. Inside it,
per feature and grouped together:

- repositories as `single<Interface> { Imp() }`
- ViewModels via `viewModel { }`
- adapters via `factory { }`

The library modules (`storageModule`, `batteryModule`, ...) stay in their own libraries and are only
assembled in the application class.

## Repositories and networking

There is no backend today, so there is no `data/remote` package and no Ktor dependency. When one is
needed, add it through `data/remote/NetworkClient`, exactly as written in `docs/architecture.md`.

Repositories wrap the feature library modules and the DAOs and return `ApiResult<T>`
(`ApiResult.Success` / `ApiResult.Failure`), the same shape the network code uses. ViewModels use
repositories only. No use case classes in the app and no separate DTO (Data Transfer Object)
classes.

## ViewModel pattern

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

## Deleting files

This app deletes the user's files, so every delete path follows the same rules:

- Nothing is deleted without the "Are You Sure?" confirmation from the design, and the dialog names
  what is about to go (count and size).
- Scanning never deletes. "Scanning your device safely — nothing is deleted yet" has to stay true.
- Deletes go through `:storage` only. Media the app does not own goes through
  `MediaStore.createDeleteRequest` (API 30+) where All files access is not granted.
- After a delete, the AI scan cache (`AiScanResultDao.deleteByPaths`) and any other cache that
  named those files is updated in the same repository call.

## UI

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

### Theme

The theme parent is `Theme.Material3.DayNight.NoActionBar`. Dark mode is supported: anything that
differs between light and dark is a resource with a `values-night/` counterpart, never a branch in
code.

### Sizes and text sizes

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

Never copy Figma px values 1:1. Read the frame width from the PhoneCleaner Figma file before the
first screen and write it here. If the frames are 390px wide the factor is `300 / 390`: a 24px gap
is `@dimen/_18sdp` and a 40px title is `@dimen/_31ssp`. Round to the nearest whole number, with a floor of 1; keep a 0 as 0, and keep
hairline (1px) strokes at `@dimen/_1sdp` so borders stay visible.

Screens that exist in the design keep the gutter measured from Figma; screens with no design
frame use a 14sdp gutter.

### Colors

Every color used in the designs gets a named entry. `res/values/colors.xml` holds the light palette
and `res/values-night/colors.xml` holds the dark one, with exactly the same names in both files.
Never write a raw hex value in a layout, a style or code.

### Strings

Never hardcode user-visible text in a layout or in code. Every such string lives in
`res/values/strings.xml`, because localization is planned.

Where the design paints part of a sentence differently - a different colour, weight or a gradient -
keep the whole sentence in one string and put the highlighted part in a second string, then apply
the span in code with `TextView.setGradientHighlight` / `setColorHighlight` from `utils/Extensions`.
Never split the sentence into two views or hardcode the highlighted words.

### Styles

There is no `res/values/type.xml`. Do not create a style per view; add a style only for a pattern
that repeats many times. Keep `res/values/shapes.xml` only for as long as the theme actually
references it.

### Resource naming and assets

- Drawables: `shape_` for XML shape drawables, `img_` for PNG or WEBP images, `ic_` for vector
  icons.
- Fonts coming from the design go in `res/font`, one file per weight (`inter_bold.ttf`,
  `inter_black.ttf`, ...). Every `TextView` names the concrete weight file the design asks for,
  for example `android:fontFamily="@font/inter_black"`, never the `@font/inter` family.
  The theme deliberately sets no `android:fontFamily`: a theme-level font family overrides the one
  a view sets for itself, which silently renders every label at Inter Regular.
- Lottie JSON animations go in `res/raw`, played with `com.airbnb.android:lottie`.


## UI checklist (every screen)

Work through this list for every new layout and every new state, and record in the task report that
it passes.

### Sizes

- Convert every Figma value: `sdp` / `ssp` = `round(FigmaPx * 300 / 390)`. Never copy a Figma px
  value 1:1.

### Typography

- Every `TextView` names its own weight file (`@font/inter_bold`, `@font/inter_semi_bold`, ...)
  matching the Figma `fontWeight`. The theme sets no font family. Roboto is used only where Figma
  uses it.
- Every `TextView` sets `android:textColor` to a color resource. Nothing relies on a theme default.
- `letterSpacing`, `textAllCaps` and line height are copied from Figma. Line height is set through
  `app:lineHeight` only, never `android:lineHeight`.
- Line height is `ssp`: `app:lineHeight="@dimen/_16ssp"`, never `@dimen/_16sdp`, never `dp`, and never
  a `lineSpacingMultiplier` in its place. Convert it with the same `round(FigmaPx * 300 / 390)` as
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
- Gradient text uses `GradientTextView`. Two-tone text keeps the whole sentence in one string plus a
  highlight string, and applies the span in code.

### Colors and modes

- Every color — text, icon tint, fill, stroke, selected and pressed states — comes from a
  light/dark pair read from both Figma frames. A resource named after a literal color ("white",
  "black") is never used for something that flips between modes. Selected states get their own pair.
  No mode checks in code.
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

### Resources

- No hardcoded user-visible text anywhere.
- Resource names keep the `shape_` / `img_` / `ic_` / `anim_` prefixes.
- Icons come out of Figma as SVG, are converted to `ic_*` vector drawables and are tinted with color
  resources.

### Verification before reporting

- Run the screen on the test phone (a Pixel 6 Pro emulator if no phone is attached) in light mode
  and in dark mode, screenshot both, and compare
  each against the Figma export side by side. List every remaining difference with its reason.
- Check the screen once with the app in a right-to-left language (Arabic) and once on an API 26
  emulator at font size 2.0.
- Go through this checklist item by item for each new layout and state, and state the result in the
  task report.


## Device testing

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
- Never queue a tap before a screenshot has confirmed what is actually on screen. One
  `adb shell input tap` per screenshot, and the screenshot comes first.
- Change the app's language through its own language screen, never with
  `adb shell cmd locale set-app-locales`: `BaseApp.applyStoredLanguage()` reverts it on every
  process start.

## Persistence and logging

Room is in use for the local cache (`data/local`, schemas exported to `app/schemas`). Every table
can be rebuilt by scanning again. Bump the database version and add a migration with every schema
change. Small values go in `SharedPref`; kotlinx.serialization goes through `SerializingHelper`.

Logging uses `android.util.Log` — in the app and in every library module. Each class declares its
own private `TAG` constant and logs through it. Do not add a logging library.

## Writing conventions

- Spell out an abbreviation in full the first time it appears in a comment or document, for example
  RAM (Random Access Memory).
- Commit messages: plain descriptive text only — no Co-Authored-By trailers, no tool-generated
  footers, no session links and no mention of Claude or Claude Code anywhere in a commit.
