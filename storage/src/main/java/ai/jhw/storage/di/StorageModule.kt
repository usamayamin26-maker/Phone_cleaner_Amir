package ai.jhw.storage.di

import ai.jhw.storage.domain.repository.StorageRepository
import ai.jhw.storage.repo.StorageRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the storage layer: access checks, MediaStore queries, large files and the only
 * delete path in the app. The cleaner modules get [StorageRepository] from here; the app wraps it
 * in its own repositories.
 */
val storageModule = module {
    single<StorageRepository> { StorageRepositoryImpl(androidContext()) }
}
