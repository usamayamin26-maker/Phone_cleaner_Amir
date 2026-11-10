package com.phone.cleaner.domain.entities

import ai.jhw.battery.model.BatteryInfo

/** One battery reading, as the Battery Info screen shows it. */
data class BatteryDetails(
    val info: BatteryInfo,
)
