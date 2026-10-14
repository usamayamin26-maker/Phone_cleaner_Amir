package ai.jhw.appusage.di

import ai.jhw.appusage.domain.repository.AppUsageRepository
import ai.jhw.appusage.repo.AppUsageRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the appusage layer: the App Usage Report (today, last 7 days, rarely used) and
 * the Power Usage estimate, all read from UsageStatsManager. Registered in BaseApp; the app wraps
 * [AppUsageRepository] in its own repository.
 */
val appUsageModule = module {
    single<AppUsageRepository> { AppUsageRepositoryImpl(androidContext()) }
}
