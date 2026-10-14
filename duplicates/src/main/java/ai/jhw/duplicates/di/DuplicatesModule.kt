package ai.jhw.duplicates.di

import org.koin.dsl.module

/**
 * Koin module for the duplicates layer: duplicate images, videos and files, grouped by content
 * hash, with a keep-best-one pick.
 *
 * Registered in BaseApp next to the app's SharedModule. Repositories go here as
 * `single<Repository> { RepositoryImpl(...) }`; the app wraps them in its own repositories and
 * never calls this module from an activity.
 */
val duplicatesModule = module {
}
