# Project specific R8 rules. Libraries ship their own consumer rules (Room, kotlinx.serialization,
# Koin, LiteRT); add a rule here only for something this app reaches by name, with a comment that
# says what breaks without it.

# Keep line numbers so a release crash still points at the right line.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- AdMob mediation ----------
# The adapters and their partner SDKs ship consumer rules that keep what the Mobile Ads SDK loads
# by name, Yandex's four custom-event classes included, so there is nothing to keep here. Pangle's
# SDK is the one gap: two of its own nested enums are referenced from its code but are not in the
# AAR (Android Archive), and R8 warns about them on every minified build. Nothing of ours touches
# them.
-dontwarn com.bytedance.sdk.openadsdk.core.model.NetExtParams$RenderType
-dontwarn com.bytedance.sdk.openadsdk.core.settings.TTSdkSettings$FETCH_REQUEST_SOURCE
