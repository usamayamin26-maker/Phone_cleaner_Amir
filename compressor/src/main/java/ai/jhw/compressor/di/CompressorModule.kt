package ai.jhw.compressor.di

import org.koin.dsl.module

/**
 * Koin module for the compressor layer: photo compression by quality, resolution, size and format,
 * with replace-original.
 *
 * Registered in BaseApp next to the app's SharedModule. Repositories go here as
 * `single<Repository> { RepositoryImpl(...) }`; the app wraps them in its own repositories and
 * never calls this module from an activity.
 */
val compressorModule = module {
}
