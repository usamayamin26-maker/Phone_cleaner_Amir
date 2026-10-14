package ai.jhw.deviceinfo.model

/** The "Processor Information" screen. */
data class ProcessorDetails(
    /** The SoC (System on Chip) name when the device reports one, e.g. "Tensor G1" / "SM8650". */
    val socModel: String?,
    val socManufacturer: String?,
    /** The `Hardware` line of /proc/cpuinfo, or Build.HARDWARE. */
    val hardware: String,
    val coreCount: Int,
    /** Supported ABIs (Application Binary Interfaces), most preferred first, e.g. "arm64-v8a". */
    val supportedAbis: List<String>,
    val cores: List<CpuCore>,
    /** The CPU (Central Processing Unit) frequency governor, e.g. "schedutil". */
    val governor: String?,
)

/** One core. Frequencies are in kHz as the kernel reports them; null when not readable. */
data class CpuCore(
    val index: Int,
    val currentFreqKhz: Long?,
    val minFreqKhz: Long?,
    val maxFreqKhz: Long?,
    /** False when the kernel has taken the core offline to save power. */
    val isOnline: Boolean,
)
