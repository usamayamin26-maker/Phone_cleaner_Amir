package ai.jhw.appmanager.di

import org.koin.dsl.module

/**
 * Koin module for the appmanager layer: installed apps with their sizes, and the uninstall flow.
 *
 * Registered in BaseApp next to the app's SharedModule. Repositories go here as
 * `single<Repository> { RepositoryImpl(...) }`; the app wraps them in its own repositories and
 * never calls this module from an activity.
 */
val appManagerModule = module {
}
