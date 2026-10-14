package ai.jhw.battery.domain.repository

import ai.jhw.battery.model.BatteryInfo
import android.content.Intent
import kotlinx.coroutines.flow.Flow

interface BatteryRepository {

    /** One reading of the battery right now. */
    suspend fun read(): BatteryInfo

    /**
     * A fresh [BatteryInfo] every time the platform reports a change (level, temperature,
     * plugging in) or power saving is switched. Starts with the current reading.
     */
    fun observe(): Flow<BatteryInfo>

    /**
     * The system's Battery Saver settings. An app cannot switch power saving itself, so the
     * Power Saving Mode switch opens this screen.
     */
    fun batterySaverSettingsIntent(): Intent

    /** The system's battery usage screen, which has the real per-app battery figures. */
    fun batteryUsageIntent(): Intent
}
