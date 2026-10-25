package com.phone.cleaner.domain.entities

import ai.jhw.battery.model.BatteryInfo

/**
 * One battery reading plus what the platform does not keep: when the battery was last seen full,
 * in epoch milliseconds, or 0 if it never has been.
 */
data class BatteryDetails(
    val info: BatteryInfo,
    val lastFullChargeAt: Long,
)
