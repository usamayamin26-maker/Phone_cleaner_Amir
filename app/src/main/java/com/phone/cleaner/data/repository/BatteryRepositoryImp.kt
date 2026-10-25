package com.phone.cleaner.data.repository

import ai.jhw.battery.model.BatteryInfo
import android.content.Intent
import android.util.Log
import com.phone.cleaner.data.mapper.toEntity
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.BatteryDetails
import com.phone.cleaner.domain.repository.BatteryRepository
import com.phone.cleaner.utils.SharedPref
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class BatteryRepositoryImp(
    private val battery: ai.jhw.battery.domain.repository.BatteryRepository,
    private val sharedPref: SharedPref,
) : BatteryRepository {

    override suspend fun getBatteryInfo(): ApiResult<BatteryDetails> = try {
        ApiResult.Success(record(battery.read()))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Battery read failed", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }

    override fun observeBatteryInfo(): Flow<ApiResult<BatteryDetails>> =
        battery.observe()
            .map<BatteryInfo, ApiResult<BatteryDetails>> { ApiResult.Success(record(it)) }
            .catch { e ->
                Log.e(TAG, "Battery updates failed", e)
                emit(ApiResult.Failure(e.message ?: "Something went wrong"))
            }

    override fun batterySaverSettingsIntent(): Intent = battery.batterySaverSettingsIntent()

    /**
     * The platform does not keep "last fully charged", so every reading that shows a full battery
     * moves the stored time forward. While the phone stays plugged in at 100% the time keeps
     * moving, so after unplugging it reads as the moment the charger came off.
     */
    private fun record(info: BatteryInfo): BatteryDetails {
        if (info.isFullyCharged) sharedPref.lastFullChargeAt = System.currentTimeMillis()
        return info.toEntity(sharedPref.lastFullChargeAt)
    }

    private companion object {
        const val TAG = "BatteryRepositoryImp"
    }
}
