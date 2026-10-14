package ai.jhw.deviceinfo.di

import ai.jhw.deviceinfo.domain.repository.DeviceInfoRepository
import ai.jhw.deviceinfo.repo.DeviceInfoRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the deviceinfo layer: device, processor, WiFi, display, RAM (Random Access
 * Memory), ROM (Read-Only Memory, the internal storage) and camera information, plus the two Home
 * usage cards. Registered in BaseApp; the app wraps [DeviceInfoRepository] in its own repository.
 */
val deviceInfoModule = module {
    single<DeviceInfoRepository> { DeviceInfoRepositoryImpl(androidContext()) }
}
