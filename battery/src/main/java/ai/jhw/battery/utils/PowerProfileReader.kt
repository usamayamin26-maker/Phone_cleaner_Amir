package ai.jhw.battery.utils

import android.content.Context
import android.util.Log

/**
 * Reads the battery's design capacity from the platform's hidden `PowerProfile` class, which is
 * where the manufacturer's spec value (e.g. 5000 mAh) lives. There is no public API for it.
 *
 * It is reflection on a non-SDK (Software Development Kit) interface, so it can fail on any
 * device or future release; every failure returns null and the caller falls back to an estimate.
 * R8 does not touch it: the class name is a platform class, not ours.
 */
internal object PowerProfileReader {

    private const val TAG = "PowerProfileReader"
    private const val POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile"

    fun batteryCapacityMah(context: Context): Int? = try {
        val clazz = Class.forName(POWER_PROFILE_CLASS)
        val profile = clazz.getConstructor(Context::class.java).newInstance(context)
        val capacity = clazz.getMethod("getBatteryCapacity").invoke(profile) as? Double
        // Emulators and some devices report the 1000 mAh placeholder from the default profile.
        capacity?.toInt()?.takeIf { it > 1000 }
    } catch (e: Exception) {
        Log.w(TAG, "Power profile not readable: ${e.javaClass.simpleName}")
        null
    }
}
