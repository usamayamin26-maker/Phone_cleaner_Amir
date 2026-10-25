package com.phone.cleaner.domain.repository

import android.content.Intent
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.BatteryDetails
import kotlinx.coroutines.flow.Flow

/** Battery Info, over the :battery module. Both reads record when the battery was last full. */
interface BatteryRepository {

    /** One reading now. */
    suspend fun getBatteryInfo(): ApiResult<BatteryDetails>

    /** A reading on every battery or power saving change, starting with the current one. */
    fun observeBatteryInfo(): Flow<ApiResult<BatteryDetails>>

    /** The system's Battery Saver settings: an app cannot switch power saving itself. */
    fun batterySaverSettingsIntent(): Intent
}
