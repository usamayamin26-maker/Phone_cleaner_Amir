package com.phone.cleaner.di

import androidx.room.Room
import com.phone.cleaner.data.local.AppDatabase
import com.phone.cleaner.utils.Constants
import com.phone.cleaner.utils.SharedPref
import org.koin.android.ext.koin.androidContext
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
}
