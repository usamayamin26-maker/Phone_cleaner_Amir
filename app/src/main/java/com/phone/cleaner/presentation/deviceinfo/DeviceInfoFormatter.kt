package com.phone.cleaner.presentation.deviceinfo

import ai.jhw.deviceinfo.model.CameraDetails
import ai.jhw.deviceinfo.model.CameraFacing
import ai.jhw.deviceinfo.model.DisplayDetails
import ai.jhw.deviceinfo.model.MemoryDetails
import ai.jhw.deviceinfo.model.WifiStandard
import android.content.Context
import androidx.annotation.StringRes
import com.phone.cleaner.R
import com.phone.cleaner.domain.entities.DeviceInfoContent
import com.phone.cleaner.domain.entities.InfoListItem
import com.phone.cleaner.utils.appLocale
import com.phone.cleaner.utils.formatNumber
import com.phone.cleaner.utils.formatPercent
import com.phone.cleaner.utils.orNotAvailable
import com.phone.cleaner.utils.toReadableSize
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Turns what the device reported into the label/value cards of a detail screen, in the app's
 * language. "Device Information" shows the three rows of its design, then model, brand, Android
 * version and security patch in the same cards; the other sections have no design and show the
 * values a user can read something from.
 */
class DeviceInfoFormatter(private val context: Context) {

    fun format(content: DeviceInfoContent): List<InfoListItem> = when (content) {
        is DeviceInfoContent.Device -> with(content.device) {
            listOf(
                row(R.string.label_screen_resolution, resolution(content.display)),
                row(R.string.label_screen_size, screenSize(content.display)),
                row(R.string.label_android_id, androidId),
                row(R.string.label_model, model),
                row(R.string.label_brand, brand.replaceFirstChar { it.titlecase(context.appLocale) }),
                row(
                    R.string.label_android_version,
                    string(R.string.format_android_version, androidVersion, number(apiLevel)),
                ),
                row(R.string.label_security_patch, securityPatch?.let(::date)),
            )
        }
        is DeviceInfoContent.Processor -> with(content.processor) {
            listOf(
                row(R.string.label_processor, socModel ?: hardware),
                row(R.string.label_manufacturer, socManufacturer),
                row(R.string.label_hardware, hardware),
                row(R.string.label_cores, number(coreCount)),
                row(R.string.label_architecture, supportedAbis.firstOrNull()),
                row(
                    R.string.label_max_frequency,
                    // A core that reports 0 kHz (emulators do) has not reported a frequency.
                    cores.mapNotNull { it.maxFreqKhz }.filter { it > 0 }.maxOrNull()?.let(::frequency),
                ),
                row(R.string.label_governor, governor),
            )
        }
        is DeviceInfoContent.Wifi -> with(content.wifi) {
            listOf(
                row(
                    R.string.label_status,
                    when {
                        !isWifiEnabled -> context.getString(R.string.state_off)
                        isConnected -> context.getString(R.string.wifi_connected)
                        else -> context.getString(R.string.wifi_not_connected)
                    },
                ),
                row(R.string.label_network_name, ssid),
                row(R.string.label_signal_strength, rssiDbm?.let { string(R.string.format_dbm, number(it)) }),
                row(R.string.label_link_speed, linkSpeedMbps?.let { string(R.string.format_mbps, number(it)) }),
                row(R.string.label_frequency, frequencyMhz?.let { string(R.string.format_mhz, number(it)) }),
                row(R.string.label_wifi_standard, wifiStandard?.let(::wifiStandard)),
                row(R.string.label_ip_address, ipv4Address),
                row(R.string.label_ipv6_address, ipv6Address),
                row(R.string.label_gateway, gateway),
                row(R.string.label_dns, dnsServers.joinToString("\n").ifEmpty { null }),
            )
        }
        is DeviceInfoContent.Display -> with(content.display) {
            listOf(
                row(R.string.label_screen_resolution, resolution(this)),
                row(R.string.label_screen_size, screenSize(this)),
                row(R.string.label_density, string(R.string.format_dpi, number(densityDpi), densityBucket)),
                row(R.string.label_refresh_rate, hertz(refreshRateHz)),
                row(
                    R.string.label_supported_refresh_rates,
                    supportedRefreshRatesHz.joinToString(", ") { hertz(it) }.ifEmpty { null },
                ),
                row(R.string.label_hdr, yesNo(isHdrSupported, R.string.value_supported, R.string.value_not_supported)),
                row(
                    R.string.label_brightness,
                    brightnessFraction?.let { context.formatPercent(it * 100, 0) }
                        ?: context.getString(R.string.value_brightness_auto),
                ),
                row(R.string.label_screen_timeout, screenTimeoutMillis?.let(::duration)),
            )
        }
        is DeviceInfoContent.Ram -> memory(content.ram.memory)
        is DeviceInfoContent.Rom -> with(content.storage) {
            memory(memory) + listOf(
                row(R.string.label_system, systemBytes?.toReadableSize(context)),
                row(R.string.label_removable_storage, yesNo(hasRemovableStorage)),
            )
        }
        is DeviceInfoContent.Cameras -> cameras(content.cameras)
    }

    private fun memory(memory: MemoryDetails): List<InfoListItem> = listOf(
        row(R.string.label_total, memory.totalBytes.toReadableSize(context)),
        row(R.string.label_used, memory.usedBytes.toReadableSize(context)),
        row(R.string.label_available, memory.availableBytes.toReadableSize(context)),
        row(R.string.label_usage, context.formatPercent(memory.usedPercent)),
    )

    /** One heading and block per camera; "Rear Camera 2" only when there is more than one rear camera. */
    private fun cameras(cameras: List<CameraDetails>): List<InfoListItem> {
        if (cameras.isEmpty()) return listOf(row(R.string.info_camera, null))
        val perFacing = cameras.groupingBy { it.facing }.eachCount()
        val seen = mutableMapOf<CameraFacing, Int>()
        return cameras.flatMap { camera ->
            val name = context.getString(
                when (camera.facing) {
                    CameraFacing.BACK -> R.string.camera_back
                    CameraFacing.FRONT -> R.string.camera_front
                    CameraFacing.EXTERNAL -> R.string.camera_external
                    CameraFacing.UNKNOWN -> R.string.camera_other
                },
            )
            val index = (seen[camera.facing] ?: 0) + 1
            seen[camera.facing] = index
            val title = if ((perFacing[camera.facing] ?: 0) > 1) {
                string(R.string.format_camera_numbered, name, number(index))
            } else {
                name
            }
            listOf(InfoListItem.Heading(title)) + cameraRows(camera)
        }
    }

    private fun cameraRows(camera: CameraDetails): List<InfoListItem> = with(camera) {
        val width = maxWidthPx
        val height = maxHeightPx
        val sensorWidth = sensorWidthMm
        val sensorHeight = sensorHeightMm
        listOf(
            row(R.string.label_megapixels, megapixels?.let { string(R.string.format_megapixels, number(it, 1)) }),
            row(
                R.string.label_max_resolution,
                if (width != null && height != null) {
                    string(R.string.format_resolution, number(width), number(height))
                } else {
                    null
                },
            ),
            row(
                R.string.label_aperture,
                apertures.joinToString(", ") { string(R.string.format_aperture, number(it, 2)) }.ifEmpty { null },
            ),
            row(
                R.string.label_focal_length,
                focalLengthsMm.joinToString(", ") { string(R.string.format_mm, number(it, 2)) }.ifEmpty { null },
            ),
            row(
                R.string.label_sensor_size,
                if (sensorWidth != null && sensorHeight != null) {
                    string(R.string.format_sensor_size, number(sensorWidth, 2), number(sensorHeight, 2))
                } else {
                    null
                },
            ),
            row(R.string.label_flash, yesNo(hasFlash)),
            row(R.string.label_optical_stabilization, yesNo(hasOpticalStabilization)),
            row(R.string.label_autofocus, yesNo(hasAutoFocus)),
            row(R.string.label_digital_zoom, maxDigitalZoom?.let { string(R.string.format_zoom, number(it, 1)) }),
        )
    }

    private fun resolution(display: DisplayDetails): String =
        string(R.string.format_resolution, number(display.widthPx), number(display.heightPx))

    private fun screenSize(display: DisplayDetails): String? =
        display.diagonalInches?.let { string(R.string.format_inches, number(it, 2)) }

    private fun frequency(khz: Long): String =
        if (khz >= 1_000_000) {
            string(R.string.format_ghz, number(khz / 1_000_000.0, 2))
        } else {
            string(R.string.format_mhz, number(khz / 1_000.0))
        }

    private fun hertz(value: Float): String = string(R.string.format_hz, number(value))

    private fun wifiStandard(standard: WifiStandard): String? = when (standard) {
        WifiStandard.LEGACY -> context.getString(R.string.wifi_standard_legacy)
        WifiStandard.WIFI_4 -> string(R.string.format_wifi_generation, number(4))
        WifiStandard.WIFI_5 -> string(R.string.format_wifi_generation, number(5))
        WifiStandard.WIFI_6 -> string(R.string.format_wifi_generation, number(6))
        WifiStandard.WIFI_7 -> string(R.string.format_wifi_generation, number(7))
        WifiStandard.UNKNOWN -> null
    }

    private fun duration(millis: Long): String {
        // "Never" is stored as the largest int: the screen does not time out.
        if (millis >= Int.MAX_VALUE) return context.getString(R.string.value_never)
        val seconds = millis / 1000
        return if (seconds < 60) {
            string(R.string.format_seconds, number(seconds))
        } else {
            string(R.string.format_minutes, number(seconds / 60.0, 1))
        }
    }

    /** The build's "2026-09-05" as a date in the app's language; the raw text if it is not one. */
    private fun date(isoDate: String): String = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(isoDate)
        if (parsed == null) isoDate else DateFormat.getDateInstance(DateFormat.MEDIUM, context.appLocale).format(parsed)
    } catch (e: ParseException) {
        isoDate
    }

    private fun yesNo(
        value: Boolean,
        @StringRes yes: Int = R.string.value_yes,
        @StringRes no: Int = R.string.value_no,
    ): String = context.getString(if (value) yes else no)

    private fun row(@StringRes label: Int, value: String?): InfoListItem.Row =
        InfoListItem.Row(context.getString(label), value.orNotAvailable(context))

    private fun string(@StringRes format: Int, vararg args: String): String = context.getString(format, *args)

    private fun number(value: Number, maxFractionDigits: Int = 0): String =
        context.formatNumber(value, maxFractionDigits)
}
