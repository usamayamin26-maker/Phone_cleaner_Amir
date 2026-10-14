package ai.jhw.storage.di

import org.koin.dsl.module

/**
 * Koin module for the storage layer: MediaStore queries, file sizes, the delete flow and large
 * files. The other cleaner modules build on it.
 *
 * Registered in BaseApp next to the app's SharedModule. Repositories go here as
 * `single<Repository> { RepositoryImpl(...) }`; the app wraps them in its own repositories and
 * never calls this module from an activity.
 */
val storageModule = module {
}
