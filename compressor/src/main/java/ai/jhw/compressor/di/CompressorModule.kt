package ai.jhw.compressor.di

import ai.jhw.compressor.domain.repository.CompressorRepository
import ai.jhw.compressor.repo.CompressorRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the compressor layer: photo compression by quality, resolution, size and
 * format, then save-a-copy or replace-original. Needs `storageModule`.
 */
val compressorModule = module {
    single<CompressorRepository> { CompressorRepositoryImpl(androidContext(), get()) }
}
