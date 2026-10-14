package ai.jhw.duplicates.di

import ai.jhw.duplicates.domain.repository.DuplicatesRepository
import ai.jhw.duplicates.repo.DuplicatesRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the duplicates layer: identical images, videos, audio and documents grouped by
 * content hash, with a keep-best-one pick. Needs `storageModule`.
 */
val duplicatesModule = module {
    single<DuplicatesRepository> { DuplicatesRepositoryImpl(androidContext(), get()) }
}
