package ai.jhw.deviceinfo.utils

import ai.jhw.deviceinfo.model.CameraDetails
import ai.jhw.deviceinfo.model.CameraFacing
import ai.jhw.deviceinfo.model.CameraHardwareLevel
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Log

/** Reads camera characteristics. No camera permission is needed: nothing is opened. */
internal object CameraReader {

    private const val TAG = "CameraReader"

    fun read(context: Context): List<CameraDetails> {
        val manager = context.getSystemService(CameraManager::class.java) ?: return emptyList()
        val ids = try {
            manager.cameraIdList.toList()
        } catch (e: Exception) {
            Log.w(TAG, "Camera list not readable", e)
            return emptyList()
        }
        return ids.mapNotNull { id ->
            try {
                toDetails(id, manager.getCameraCharacteristics(id))
            } catch (e: Exception) {
                Log.w(TAG, "Camera $id not readable", e)
                null
            }
        }
    }

    private fun toDetails(id: String, c: CameraCharacteristics): CameraDetails {
        val largest = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(ImageFormat.JPEG)
            ?.maxByOrNull { it.width.toLong() * it.height }
        val pixelArray = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val sensorSize = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val afModes = c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: IntArray(0)
        val oisModes = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            ?: IntArray(0)

        return CameraDetails(
            id = id,
            facing = when (c.get(CameraCharacteristics.LENS_FACING)) {
                CameraMetadata.LENS_FACING_BACK -> CameraFacing.BACK
                CameraMetadata.LENS_FACING_FRONT -> CameraFacing.FRONT
                CameraMetadata.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
                else -> CameraFacing.UNKNOWN
            },
            maxWidthPx = largest?.width,
            maxHeightPx = largest?.height,
            megapixels = pixelArray?.let { it.width.toLong() * it.height / 1_000_000.0 },
            focalLengthsMm = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                ?.toList().orEmpty(),
            apertures = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                ?.toList().orEmpty(),
            sensorWidthMm = sensorSize?.width,
            sensorHeightMm = sensorSize?.height,
            hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true,
            hasOpticalStabilization =
                CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON in oisModes,
            hasAutoFocus = afModes.any { it != CameraMetadata.CONTROL_AF_MODE_OFF },
            maxDigitalZoom = c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM),
            hardwareLevel = when (c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> CameraHardwareLevel.LEGACY
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> CameraHardwareLevel.LIMITED
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> CameraHardwareLevel.FULL
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> CameraHardwareLevel.LEVEL_3
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> CameraHardwareLevel.EXTERNAL
                else -> CameraHardwareLevel.UNKNOWN
            },
        )
    }
}
