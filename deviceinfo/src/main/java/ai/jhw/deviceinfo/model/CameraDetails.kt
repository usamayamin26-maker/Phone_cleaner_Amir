package ai.jhw.deviceinfo.model

/** One camera on the "Camera Information" screen. No camera permission is needed for any of it. */
data class CameraDetails(
    val id: String,
    val facing: CameraFacing,
    /** Largest still-capture size in pixels. */
    val maxWidthPx: Int?,
    val maxHeightPx: Int?,
    /** Megapixels of the full sensor pixel array, e.g. 50.3. */
    val megapixels: Double?,
    val focalLengthsMm: List<Float>,
    /** f-numbers, e.g. 1.85. */
    val apertures: List<Float>,
    val sensorWidthMm: Float?,
    val sensorHeightMm: Float?,
    val hasFlash: Boolean,
    /** OIS (Optical Image Stabilization). */
    val hasOpticalStabilization: Boolean,
    val hasAutoFocus: Boolean,
    val maxDigitalZoom: Float?,
    val hardwareLevel: CameraHardwareLevel,
)

enum class CameraFacing { BACK, FRONT, EXTERNAL, UNKNOWN }

enum class CameraHardwareLevel { LEGACY, LIMITED, FULL, LEVEL_3, EXTERNAL, UNKNOWN }
