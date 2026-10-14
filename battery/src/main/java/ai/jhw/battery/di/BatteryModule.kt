package ai.jhw.battery.di

import ai.jhw.battery.domain.repository.BatteryRepository
import ai.jhw.battery.repo.BatteryRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the battery layer: battery health, level, temperature, voltage, capacity,
 * charging state and power saving. Registered in BaseApp next to the app's SharedModule; the app
 * wraps [BatteryRepository] in its own repository and never calls it from an activity.
 *
 * "Last fully charged" is not kept here: the platform does not store it, so the app records it
 * from [ai.jhw.battery.model.BatteryInfo.isFullyCharged] into its SharedPref.
 */
val batteryModule = module {
    single<BatteryRepository> { BatteryRepositoryImpl(androidContext()) }
}
