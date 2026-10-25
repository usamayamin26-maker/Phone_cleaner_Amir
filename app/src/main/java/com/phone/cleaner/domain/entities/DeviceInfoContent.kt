package com.phone.cleaner.domain.entities

import ai.jhw.deviceinfo.model.CameraDetails
import ai.jhw.deviceinfo.model.DeviceDetails
import ai.jhw.deviceinfo.model.DisplayDetails
import ai.jhw.deviceinfo.model.ProcessorDetails
import ai.jhw.deviceinfo.model.RamDetails
import ai.jhw.deviceinfo.model.StorageDetails
import ai.jhw.deviceinfo.model.WifiDetails

/** What one Device Info detail screen shows, as read from the device. The screen formats it. */
sealed interface DeviceInfoContent {
    /** "Device Information": the design shows the resolution and size from the display. */
    data class Device(val device: DeviceDetails, val display: DisplayDetails) : DeviceInfoContent
    data class Processor(val processor: ProcessorDetails) : DeviceInfoContent
    data class Wifi(val wifi: WifiDetails) : DeviceInfoContent
    data class Display(val display: DisplayDetails) : DeviceInfoContent
    data class Ram(val ram: RamDetails) : DeviceInfoContent
    data class Rom(val storage: StorageDetails) : DeviceInfoContent
    data class Cameras(val cameras: List<CameraDetails>) : DeviceInfoContent
}
