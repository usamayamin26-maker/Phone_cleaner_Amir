package com.phone.cleaner.data.repository

import ai.jhw.deviceinfo.model.CameraDetails
import ai.jhw.deviceinfo.model.DeviceDetails
import ai.jhw.deviceinfo.model.DisplayDetails
import ai.jhw.deviceinfo.model.ProcessorDetails
import ai.jhw.deviceinfo.model.RamDetails
import ai.jhw.deviceinfo.model.StorageDetails
import ai.jhw.deviceinfo.model.WifiDetails
import android.util.Log
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.repository.DeviceInfoRepository
import kotlinx.coroutines.CancellationException

class DeviceInfoRepositoryImp(
    private val deviceInfo: ai.jhw.deviceinfo.domain.repository.DeviceInfoRepository,
) : DeviceInfoRepository {

    override suspend fun getDevice(): ApiResult<DeviceDetails> = read("device") { deviceInfo.getDevice() }

    override suspend fun getProcessor(): ApiResult<ProcessorDetails> =
        read("processor") { deviceInfo.getProcessor() }

    override suspend fun getWifi(): ApiResult<WifiDetails> = read("wifi") { deviceInfo.getWifi() }

    override suspend fun getDisplay(): ApiResult<DisplayDetails> = read("display") { deviceInfo.getDisplay() }

    override suspend fun getRam(): ApiResult<RamDetails> = read("ram") { deviceInfo.getRam() }

    override suspend fun getStorage(): ApiResult<StorageDetails> = read("storage") { deviceInfo.getStorage() }

    override suspend fun getCameras(): ApiResult<List<CameraDetails>> =
        read("cameras") { deviceInfo.getCameras() }

    private suspend fun <T> read(what: String, block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Reading $what failed", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }

    private companion object {
        const val TAG = "DeviceInfoRepositoryImp"
    }
}
