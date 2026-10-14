package ai.jhw.deviceinfo.di

import org.koin.dsl.module

/**
 * Koin module for the deviceinfo layer: device, processor, WiFi, display, RAM (Random Access
 * Memory), ROM (Read-Only Memory, internal storage) and camera information.
 *
 * Registered in BaseApp next to the app's SharedModule. Repositories go here as
 * `single<Repository> { RepositoryImpl(...) }`; the app wraps them in its own repositories and
 * never calls this module from an activity.
 */
val deviceInfoModule = module {
}
