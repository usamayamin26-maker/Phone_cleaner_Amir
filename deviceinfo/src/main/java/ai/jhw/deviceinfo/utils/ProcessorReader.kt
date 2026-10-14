package ai.jhw.deviceinfo.utils

import ai.jhw.deviceinfo.model.CpuCore
import ai.jhw.deviceinfo.model.ProcessorDetails
import android.os.Build

internal object ProcessorReader {

    private const val CPU_DIR = "/sys/devices/system/cpu"

    fun read(): ProcessorDetails {
        val coreCount = possibleCoreCount() ?: Runtime.getRuntime().availableProcessors()
        val socModel: String?
        val socManufacturer: String?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            socModel = Build.SOC_MODEL.takeUnless { it.isBlank() || it == Build.UNKNOWN }
            socManufacturer = Build.SOC_MANUFACTURER.takeUnless { it.isBlank() || it == Build.UNKNOWN }
        } else {
            socModel = null
            socManufacturer = null
        }
        return ProcessorDetails(
            socModel = socModel,
            socManufacturer = socManufacturer,
            hardware = cpuInfoHardware() ?: Build.HARDWARE,
            coreCount = coreCount,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            cores = (0 until coreCount).map(::readCore),
            governor = SysFs.readLine("$CPU_DIR/cpu0/cpufreq/scaling_governor"),
        )
    }

    /** "0-7" in /sys/devices/system/cpu/possible means eight cores, online or not. */
    private fun possibleCoreCount(): Int? {
        val range = SysFs.readLine("$CPU_DIR/possible") ?: return null
        return range.split(",").sumOf { part ->
            val bounds = part.split("-").mapNotNull { it.trim().toIntOrNull() }
            when (bounds.size) {
                1 -> 1
                2 -> bounds[1] - bounds[0] + 1
                else -> 0
            }
        }.takeIf { it > 0 }
    }

    private fun readCore(index: Int): CpuCore {
        val base = "$CPU_DIR/cpu$index"
        // cpu0 usually has no "online" file because it can never go offline.
        val online = SysFs.readLine("$base/online")?.let { it == "1" } ?: true
        return CpuCore(
            index = index,
            currentFreqKhz = if (online) SysFs.readLong("$base/cpufreq/scaling_cur_freq") else null,
            minFreqKhz = SysFs.readLong("$base/cpufreq/cpuinfo_min_freq"),
            maxFreqKhz = SysFs.readLong("$base/cpufreq/cpuinfo_max_freq"),
            isOnline = online,
        )
    }

    private fun cpuInfoHardware(): String? =
        SysFs.readLines("/proc/cpuinfo")
            .firstOrNull { it.startsWith("Hardware", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}
