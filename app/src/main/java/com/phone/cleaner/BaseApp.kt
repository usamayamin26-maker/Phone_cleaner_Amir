package com.phone.cleaner

import ai.jhw.aicleaner.di.aiCleanerModule
import ai.jhw.appmanager.di.appManagerModule
import ai.jhw.appusage.di.appUsageModule
import ai.jhw.battery.di.batteryModule
import ai.jhw.compressor.di.compressorModule
import ai.jhw.deviceinfo.di.deviceInfoModule
import ai.jhw.duplicates.di.duplicatesModule
import ai.jhw.junkcleaner.di.junkCleanerModule
import ai.jhw.storage.di.storageModule
import ai.jhw.whatsapp.di.whatsAppModule
import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.phone.cleaner.ads.AdsController
import com.phone.cleaner.ads.AdsSetup
import com.phone.cleaner.ads.AppAds
import com.phone.cleaner.di.SharedModule
import com.phone.cleaner.messaging.NotificationChannels
import com.phone.cleaner.utils.SharedPref
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BaseApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)

        startKoin {
            androidContext(this@BaseApp)
            modules(
                SharedModule,
                // Feature library modules - each registers its own singles.
                storageModule,
                junkCleanerModule,
                aiCleanerModule,
                whatsAppModule,
                duplicatesModule,
                appManagerModule,
                deviceInfoModule,
                batteryModule,
                appUsageModule,
                compressorModule,
            )
        }

        applyStoredLanguage()

        // The ads package registers the resume app open ad and the full-screen state tracking
        // itself from here; consent, the Mobile Ads SDK and the config fetch start in
        // SplashActivity (AdsController.start), because the consent form needs an Activity.
        AdsController.init(
            this,
            AdsSetup(
                remoteConfigKey = AppAds.REMOTE_CONFIG_KEY,
                defaultJson = AppAds.DEFAULT_JSON,
                isDebug = BuildConfig.DEBUG,
            ),
        )
    }

    /**
     * Coil's app-wide image loader. The cleaners show photos and videos from MediaStore content
     * URIs; the video decoder turns a video into a frame from its start.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()

    /**
     * Applies the language the user picked, here in the application, before the first activity
     * exists.
     *
     * Below API (Application Programming Interface) level 33 a locale change can only reach a live
     * activity by recreating it. With no activity alive yet the locale is simply part of the
     * configuration every activity is then created with, so nothing has to be recreated.
     *
     * From API level 33 the locale lives in the framework's [LocaleManager]. It is addressed
     * directly rather than through `AppCompatDelegate`, because AppCompat reaches LocaleManager
     * through a live activity and silently does nothing when there is none.
     */
    private fun applyStoredLanguage() {
        val tag = get<SharedPref>().appLanguage
        val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java)?.applicationLocales?.toLanguageTags()
        } else {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }
        if (current == tag) {
            Log.d(TAG, "Locale already in effect: $tag")
            return
        }
        Log.d(TAG, "Applying stored locale: $tag (was \"$current\")")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java)?.applicationLocales =
                LocaleList.forLanguageTags(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    private companion object {
        const val TAG = "BaseApp"
    }
}
