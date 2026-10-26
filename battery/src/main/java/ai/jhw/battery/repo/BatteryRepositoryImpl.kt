package ai.jhw.battery.repo

import ai.jhw.battery.domain.repository.BatteryRepository
import ai.jhw.battery.model.BatteryHealth
import ai.jhw.battery.model.BatteryInfo
import ai.jhw.battery.model.BatteryPlugType
import ai.jhw.battery.model.BatteryStatus
import ai.jhw.battery.model.CapacitySource
import ai.jhw.battery.utils.PowerProfileReader
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class BatteryRepositoryImpl(private val context: Context) : BatteryRepository {

    private val batteryManager = context.getSystemService(BatteryManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)

    /** The design capacity never changes, so the reflective read happens once per process. */
    private val designCapacityMah: Int? by lazy { PowerProfileReader.batteryCapacityMah(context) }

    override suspend fun read(): BatteryInfo = withContext(Dispatchers.IO) {
        // ACTION_BATTERY_CHANGED is sticky: registering with a null receiver returns the last
        // broadcast without subscribing to anything.
        val sticky = ContextCompat.registerReceiver(
            context,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        toBatteryInfo(sticky)
    }

    override fun observe(): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val batteryIntent = if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    intent
                } else {
                    // Power saving changed: the battery broadcast itself did not, so re-read it.
                    ContextCompat.registerReceiver(
                        context,
                        null,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )
                }
                trySend(toBatteryInfo(batteryIntent))
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        // Registering for the sticky broadcast delivers the current value at once, which is the
        // flow's first emission.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate().flowOn(Dispatchers.IO)

    override fun batterySaverSettingsIntent(): Intent =
        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    override fun batteryUsageIntent(): Intent =
        Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun toBatteryInfo(intent: Intent?): BatteryInfo {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val levelPercent = if (level >= 0 && scale > 0) level * 100 / scale else null

        val temperatureTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1

        val (capacity, source) = capacity(levelPercent)

        return BatteryInfo(
            levelPercent = levelPercent,
            health = health(intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1),
            status = status(intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1),
            plugType = plugType(intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0),
            temperatureCelsius = if (temperatureTenths == Int.MIN_VALUE) null else temperatureTenths / 10f,
            // A few devices report volts instead of millivolts.
            voltageMillivolts = when {
                voltage <= 0 -> null
                voltage < 100 -> voltage * 1000
                else -> voltage
            },
            capacityMah = capacity,
            capacitySource = source,
            technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() },
            currentMicroAmps = batteryManager
                ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                ?.takeIf { it != Int.MIN_VALUE && it != 0 },
            cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                intent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)?.takeIf { it >= 0 }
            } else {
                null
            },
            chargeTimeRemainingMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                batteryManager?.computeChargeTimeRemaining()?.takeIf { it > 0 }
            } else {
                null
            },
            isPowerSaveMode = powerManager?.isPowerSaveMode == true,
        )
    }

    /**
     * The spec capacity from the power profile when the device has one; otherwise an estimate
     * from the charge counter (microampere-hours left) divided by the level.
     */
    private fun capacity(levelPercent: Int?): Pair<Int?, CapacitySource> {
        designCapacityMah?.let { return it to CapacitySource.POWER_PROFILE }

        val chargeCounter = batteryManager
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.takeIf { it > 0 && it != Int.MIN_VALUE }
        if (chargeCounter != null && levelPercent != null && levelPercent > 0) {
            val estimate = (chargeCounter.toLong() * 100 / levelPercent / 1000).toInt()
            Log.d(TAG, "Capacity estimated from charge counter: $estimate mAh")
            // Emulators and some devices report a token counter (0 or 10 mAh): no phone battery
            // is that small, so such an estimate is no estimate at all.
            if (estimate >= MIN_PLAUSIBLE_MAH) return estimate to CapacitySource.CHARGE_COUNTER_ESTIMATE
        }
        return null to CapacitySource.UNKNOWN
    }

    private fun health(value: Int) = when (value) {
        BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
        BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
        BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.FAILURE
        else -> BatteryHealth.UNKNOWN
    }

    private fun status(value: Int) = when (value) {
        BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
        BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.DISCHARGING
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NOT_CHARGING
        BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
        else -> BatteryStatus.UNKNOWN
    }

    private fun plugType(value: Int) = when (value) {
        BatteryManager.BATTERY_PLUGGED_AC -> BatteryPlugType.AC
        BatteryManager.BATTERY_PLUGGED_USB -> BatteryPlugType.USB
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryPlugType.WIRELESS
        BatteryManager.BATTERY_PLUGGED_DOCK -> BatteryPlugType.DOCK
        else -> BatteryPlugType.NONE
    }

    private companion object {
        const val TAG = "BatteryRepositoryImpl"
        const val MIN_PLAUSIBLE_MAH = 500
    }
}
