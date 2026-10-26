package ai.jhw.deviceinfo.domain.repository

import ai.jhw.deviceinfo.model.CameraDetails
import ai.jhw.deviceinfo.model.DeviceDetails
import ai.jhw.deviceinfo.model.DisplayDetails
import ai.jhw.deviceinfo.model.ProcessorDetails
import ai.jhw.deviceinfo.model.RamDetails
import ai.jhw.deviceinfo.model.SecurityStatus
import ai.jhw.deviceinfo.model.StorageDetails
import ai.jhw.deviceinfo.model.WifiDetails
import android.content.Intent

/** Everything behind the Device Info screens and the two Home usage cards. All reads are cheap. */
interface DeviceInfoRepository {
    suspend fun getDevice(): DeviceDetails
    suspend fun getProcessor(): ProcessorDetails
    suspend fun getWifi(): WifiDetails
    suspend fun getDisplay(): DisplayDetails
    suspend fun getRam(): RamDetails
    suspend fun getStorage(): StorageDetails
    suspend fun getCameras(): List<CameraDetails>

    /** Developer options, USB debugging and VPN, as the Scan Now check shows them. */
    suspend fun getSecurityStatus(): SecurityStatus

    /** The system's Developer options page, where both developer settings are switched off. */
    fun developerSettingsIntent(): Intent

    /** The system's VPN page. */
    fun vpnSettingsIntent(): Intent
}
