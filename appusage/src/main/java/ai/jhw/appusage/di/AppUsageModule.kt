package ai.jhw.appusage.di

import org.koin.dsl.module

/**
 * Koin module for the appusage layer: the App Usage Report (daily, weekly, rarely used) and Power
 * Usage, both read from UsageStatsManager.
 *
 * Registered in BaseApp next to the app's SharedModule. Repositories go here as
 * `single<Repository> { RepositoryImpl(...) }`; the app wraps them in its own repositories and
 * never calls this module from an activity.
 */
val appUsageModule = module {
}
