package ai.jhw.aicleaner.di

import org.koin.dsl.module

/**
 * Koin module for the aicleaner layer: the AI Junk Cleaner - a LiteRT (TensorFlow Lite) model in
 * src/main/assets classifies gallery images.
 *
 * Registered in BaseApp next to the app's SharedModule. Repositories go here as
 * `single<Repository> { RepositoryImpl(...) }`; the app wraps them in its own repositories and
 * never calls this module from an activity.
 */
val aiCleanerModule = module {
}
