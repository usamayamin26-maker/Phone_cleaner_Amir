package ai.jhw.battery.di

import org.koin.dsl.module

/**
 * Koin module for the battery layer: battery health, level, temperature, voltage, capacity,
 * charging state and the last full charge.
 *
 * Registered in BaseApp next to the app's SharedModule. Repositories go here as
 * `single<Repository> { RepositoryImpl(...) }`; the app wraps them in its own repositories and
 * never calls this module from an activity.
 */
val batteryModule = module {
}
