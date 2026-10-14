package ai.jhw.deviceinfo.utils

import ai.jhw.deviceinfo.model.DisplayDetails
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import kotlin.math.sqrt

internal object DisplayReader {

    fun read(context: Context): DisplayDetails {
        val display = context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
        val metrics = context.resources.displayMetrics
        val mode = display?.mode

        // The mode's size is the physical panel, whatever the orientation or the app window.
        val width = mode?.physicalWidth ?: metrics.widthPixels
        val height = mode?.physicalHeight ?: metrics.heightPixels

        val diagonal = if (metrics.xdpi > 0 && metrics.ydpi > 0) {
            val w = width / metrics.xdpi.toDouble()
            val h = height / metrics.ydpi.toDouble()
            sqrt(w * w + h * h)
        } else {
            null
        }

        val refreshRates = display?.supportedModes
            ?.filter { it.physicalWidth == width && it.physicalHeight == height }
            ?.map { it.refreshRate }
            ?.distinct()
            ?.sorted()
            .orEmpty()

        return DisplayDetails(
            widthPx = width,
            heightPx = height,
            densityDpi = metrics.densityDpi,
            densityBucket = densityBucket(metrics.densityDpi),
            diagonalInches = diagonal,
            refreshRateHz = mode?.refreshRate ?: display?.refreshRate ?: 60f,
            supportedRefreshRatesHz = refreshRates,
            isHdrSupported = isHdr(display),
            brightnessFraction = brightness(context),
            screenTimeoutMillis = try {
                Settings.System.getLong(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            } catch (_: Settings.SettingNotFoundException) {
                null
            },
        )
    }

    private fun densityBucket(dpi: Int): String = when {
        dpi <= 120 -> "ldpi"
        dpi <= 160 -> "mdpi"
        dpi <= 213 -> "tvdpi"
        dpi <= 240 -> "hdpi"
        dpi <= 320 -> "xhdpi"
        dpi <= 480 -> "xxhdpi"
        else -> "xxxhdpi"
    }

    private fun isHdr(display: Display?): Boolean {
        display ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            display.isHdr
        } else {
            @Suppress("DEPRECATION")
            display.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true
        }
    }

    private fun brightness(context: Context): Float? = try {
        val resolver = context.contentResolver
        val auto = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        if (auto) {
            null
        } else {
            (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS) / 255f)
                .coerceIn(0f, 1f)
        }
    } catch (_: Settings.SettingNotFoundException) {
        null
    }
}
