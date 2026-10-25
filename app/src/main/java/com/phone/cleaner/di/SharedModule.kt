package com.phone.cleaner.di

import androidx.room.Room
import com.phone.cleaner.data.local.AppDatabase
import com.phone.cleaner.data.repository.AiCleanerRepositoryImp
import com.phone.cleaner.data.repository.AppManagerRepositoryImp
import com.phone.cleaner.data.repository.BatteryRepositoryImp
import com.phone.cleaner.data.repository.DeviceInfoRepositoryImp
import com.phone.cleaner.data.repository.DuplicatesRepositoryImp
import com.phone.cleaner.data.repository.JunkRepositoryImp
import com.phone.cleaner.data.repository.StorageAccessRepositoryImp
import com.phone.cleaner.data.repository.WhatsAppRepositoryImp
import com.phone.cleaner.domain.repository.AiCleanerRepository
import com.phone.cleaner.domain.repository.AppManagerRepository
import com.phone.cleaner.domain.repository.BatteryRepository
import com.phone.cleaner.domain.repository.DeviceInfoRepository
import com.phone.cleaner.domain.repository.DuplicatesRepository
import com.phone.cleaner.domain.repository.JunkRepository
import com.phone.cleaner.domain.repository.StorageAccessRepository
import com.phone.cleaner.domain.repository.WhatsAppRepository
import com.phone.cleaner.domain.view_models.AiCleanerViewModel
import com.phone.cleaner.domain.view_models.AppCleanerViewModel
import com.phone.cleaner.domain.view_models.BatteryViewModel
import com.phone.cleaner.domain.view_models.DeviceInfoViewModel
import com.phone.cleaner.domain.view_models.DuplicatesViewModel
import com.phone.cleaner.domain.view_models.HomeViewModel
import com.phone.cleaner.domain.view_models.LanguageViewModel
import com.phone.cleaner.domain.view_models.SettingsViewModel
import com.phone.cleaner.domain.view_models.SplashViewModel
import com.phone.cleaner.domain.view_models.WaCleanerViewModel
import com.phone.cleaner.presentation.adapters.AppAdapter
import com.phone.cleaner.presentation.adapters.CleanerFileAdapter
import com.phone.cleaner.presentation.adapters.CleanerGridAdapter
import com.phone.cleaner.presentation.adapters.DeviceInfoSectionAdapter
import com.phone.cleaner.presentation.adapters.DuplicateGroupAdapter
import com.phone.cleaner.presentation.adapters.InfoListAdapter
import com.phone.cleaner.presentation.adapters.LanguageAdapter
import com.phone.cleaner.presentation.adapters.OnboardingAdapter
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

    // ---- storage access, shared by every cleaner ----
    single<StorageAccessRepository> { StorageAccessRepositoryImp(get()) }

    // ---- first run: splash, language, onboarding ----
    viewModel { SplashViewModel(get()) }
    viewModel { LanguageViewModel(get()) }
    factory { LanguageAdapter() }
    factory { OnboardingAdapter() }

    // ---- home ----
    viewModel { HomeViewModel(get()) }

    // ---- settings ----
    viewModel { SettingsViewModel() }

    // ---- app cleaner ----
    single<AppManagerRepository> { AppManagerRepositoryImp(get(), get()) }
    viewModel { AppCleanerViewModel(get()) }
    factory { AppAdapter() }

    // ---- cleaner pages, shared by Delete Duplicates, AI Junk Cleaner and WA Cleaner ----
    factory { CleanerGridAdapter() }
    factory { CleanerFileAdapter() }

    // ---- delete duplicates ----
    single<DuplicatesRepository> { DuplicatesRepositoryImp(get(), get(), get()) }
    viewModel { DuplicatesViewModel(get()) }
    factory { DuplicateGroupAdapter() }

    // ---- ai junk cleaner, with the Smart Scan's junk files ----
    single<AiCleanerRepository> { AiCleanerRepositoryImp(get(), get(), get()) }
    single<JunkRepository> { JunkRepositoryImp(get(), get()) }
    viewModel { AiCleanerViewModel(get(), get()) }

    // ---- wa cleaner ----
    single<WhatsAppRepository> { WhatsAppRepositoryImp(get(), get()) }
    viewModel { WaCleanerViewModel(get()) }

    // ---- device info ----
    single<DeviceInfoRepository> { DeviceInfoRepositoryImp(get()) }
    viewModel { DeviceInfoViewModel(get()) }
    factory { DeviceInfoSectionAdapter() }
    factory { InfoListAdapter() }

    // ---- battery ----
    single<BatteryRepository> { BatteryRepositoryImp(get(), get()) }
    viewModel { BatteryViewModel(get()) }
}
