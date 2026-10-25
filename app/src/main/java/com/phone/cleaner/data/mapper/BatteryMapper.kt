package com.phone.cleaner.data.mapper

import ai.jhw.battery.model.BatteryInfo
import com.phone.cleaner.domain.entities.BatteryDetails

fun BatteryInfo.toEntity(lastFullChargeAt: Long): BatteryDetails =
    BatteryDetails(info = this, lastFullChargeAt = lastFullChargeAt)
