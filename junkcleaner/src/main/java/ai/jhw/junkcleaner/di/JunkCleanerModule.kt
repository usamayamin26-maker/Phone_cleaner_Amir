package ai.jhw.junkcleaner.di

import ai.jhw.junkcleaner.domain.repository.JunkRepository
import ai.jhw.junkcleaner.repo.JunkRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the junkcleaner layer: the Smart Scan - APK (Android Package) files, temp files,
 * thumbnails, empty folders, residual folders and (Android 10 and older) app cache. Needs
 * `storageModule` for [ai.jhw.storage.domain.repository.StorageRepository].
 */
val junkCleanerModule = module {
    single<JunkRepository> { JunkRepositoryImpl(androidContext(), get()) }
}
