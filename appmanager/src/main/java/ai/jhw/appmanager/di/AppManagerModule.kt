package ai.jhw.appmanager.di

import ai.jhw.appmanager.domain.repository.AppManagerRepository
import ai.jhw.appmanager.repo.AppManagerRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the appmanager layer: installed apps with their sizes, and the uninstall flow.
 * Registered in BaseApp; the app wraps [AppManagerRepository] in its own repository.
 */
val appManagerModule = module {
    single<AppManagerRepository> { AppManagerRepositoryImpl(androidContext()) }
}
