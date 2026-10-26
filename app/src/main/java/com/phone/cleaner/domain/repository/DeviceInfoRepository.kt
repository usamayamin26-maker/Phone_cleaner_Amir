package com.phone.cleaner.domain.repository

import ai.jhw.deviceinfo.model.CameraDetails
import ai.jhw.deviceinfo.model.DeviceDetails
import ai.jhw.deviceinfo.model.DisplayDetails
import ai.jhw.deviceinfo.model.ProcessorDetails
import ai.jhw.deviceinfo.model.RamDetails
import ai.jhw.deviceinfo.model.SecurityStatus
import ai.jhw.deviceinfo.model.StorageDetails
import ai.jhw.deviceinfo.model.WifiDetails
import android.content.Intent
import com.phone.cleaner.domain.entities.ApiResult

/** The Device Info screens and the Home usage cards, over the :deviceinfo module. */
interface DeviceInfoRepository {
    suspend fun getDevice(): ApiResult<DeviceDetails>
    suspend fun getProcessor(): ApiResult<ProcessorDetails>
    suspend fun getWifi(): ApiResult<WifiDetails>
    suspend fun getDisplay(): ApiResult<DisplayDetails>
    suspend fun getRam(): ApiResult<RamDetails>
    suspend fun getStorage(): ApiResult<StorageDetails>
    suspend fun getCameras(): ApiResult<List<CameraDetails>>

    /** Developer options, USB debugging and VPN for the Scan Now check. */
    suspend fun getSecurityStatus(): ApiResult<SecurityStatus>

    /** The system screens where those are switched off. Plain intents, nothing to fail. */
    fun developerSettingsIntent(): Intent

    fun vpnSettingsIntent(): Intent
}
