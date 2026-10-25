package com.phone.cleaner.domain.entities

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.phone.cleaner.R

/**
 * One row of the Device Info list, in the design's order. [BATTERY] opens Battery Info; every other
 * section opens the shared detail screen with its own header image.
 */
enum class DeviceInfoSection(@param:StringRes val title: Int, @param:DrawableRes val icon: Int) {
    DEVICE(R.string.info_device, R.drawable.img_info_device),
    PROCESSOR(R.string.info_processor, R.drawable.img_info_processor),
    BATTERY(R.string.info_battery, R.drawable.img_info_battery2),
    WIFI(R.string.info_wifi, R.drawable.img_info_wifi),
    DISPLAY(R.string.info_display, R.drawable.img_info_display),
    RAM(R.string.info_ram, R.drawable.img_info_ram),
    ROM(R.string.info_rom, R.drawable.img_info_rom),
    CAMERA(R.string.info_camera, R.drawable.img_info_camera),
}
