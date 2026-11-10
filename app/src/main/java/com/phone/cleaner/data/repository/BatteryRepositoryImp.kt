package com.phone.cleaner.data.repository

import ai.jhw.battery.model.BatteryInfo
import android.content.Intent
import android.util.Log
import com.phone.cleaner.data.mapper.toEntity
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.BatteryDetails
import com.phone.cleaner.domain.repository.BatteryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class BatteryRepositoryImp(
    private val battery: ai.jhw.battery.domain.repository.BatteryRepository,
) : BatteryRepository {

    override suspend fun getBatteryInfo(): ApiResult<BatteryDetails> = try {
        ApiResult.Success(battery.read().toEntity())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Battery read failed", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }

    override fun observeBatteryInfo(): Flow<ApiResult<BatteryDetails>> =
        battery.observe()
            .map<BatteryInfo, ApiResult<BatteryDetails>> { ApiResult.Success(it.toEntity()) }
            .catch { e ->
                Log.e(TAG, "Battery updates failed", e)
                emit(ApiResult.Failure(e.message ?: "Something went wrong"))
            }

    override fun batterySaverSettingsIntent(): Intent = battery.batterySaverSettingsIntent()

    private companion object {
        const val TAG = "BatteryRepositoryImp"
    }
}
