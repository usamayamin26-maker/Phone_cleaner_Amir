package com.phone.cleaner.di

import androidx.room.Room
import com.phone.cleaner.data.local.AppDatabase
import com.phone.cleaner.data.repository.BatteryRepositoryImp
import com.phone.cleaner.data.repository.DeviceInfoRepositoryImp
import com.phone.cleaner.domain.repository.BatteryRepository
import com.phone.cleaner.domain.repository.DeviceInfoRepository
import com.phone.cleaner.domain.view_models.BatteryViewModel
import com.phone.cleaner.domain.view_models.DeviceInfoViewModel
import com.phone.cleaner.domain.view_models.HomeViewModel
import com.phone.cleaner.presentation.adapters.DeviceInfoSectionAdapter
import com.phone.cleaner.presentation.adapters.InfoListAdapter
import com.phone.cleaner.utils.Constants
import com.phone.cleaner.utils.SharedPref
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The app's only Koin module. Per feature, grouped together: repositories as
 * `single<Interface> { Imp() }`, ViewModels via `viewModel { }`, adapters via `factory { }`.
 *
 * The feature library modules (`storageModule`, `batteryModule`, ...) stay in their own libraries
 * and are only assembled in [com.phone.cleaner.BaseApp].
 */
val SharedModule = module {

    single { SharedPref(androidContext()) }

    // ---- local database ----
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, Constants.DB_NAME).build()
    }
    single { get<AppDatabase>().aiScanResultDao() }

    // ---- home ----
    viewModel { HomeViewModel(get()) }

    // ---- device info ----
    single<DeviceInfoRepository> { DeviceInfoRepositoryImp(get()) }
    viewModel { DeviceInfoViewModel(get()) }
    factory { DeviceInfoSectionAdapter() }
    factory { InfoListAdapter() }

    // ---- battery ----
    single<BatteryRepository> { BatteryRepositoryImp(get(), get()) }
    viewModel { BatteryViewModel(get()) }
}
