package ai.jhw.junkcleaner.di

import org.koin.dsl.module

/**
 * Koin module for the junkcleaner layer: the Smart Scan - cache, residual files of uninstalled
 * apps, empty folders, APK (Android Package) files and trash.
 *
 * Registered in BaseApp next to the app's SharedModule. Repositories go here as
 * `single<Repository> { RepositoryImpl(...) }`; the app wraps them in its own repositories and
 * never calls this module from an activity.
 */
val junkCleanerModule = module {
}
