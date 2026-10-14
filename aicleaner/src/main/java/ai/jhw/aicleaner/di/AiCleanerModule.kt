package ai.jhw.aicleaner.di

import ai.jhw.aicleaner.domain.repository.AiCleanerRepository
import ai.jhw.aicleaner.repo.AiCleanerRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the aicleaner layer: the AI Junk Cleaner - a LiteRT (TensorFlow Lite) model in
 * src/main/assets classifies gallery images and video frames. Needs `storageModule`. The verdict
 * cache lives in the app's Room database, not here.
 */
val aiCleanerModule = module {
    single<AiCleanerRepository> { AiCleanerRepositoryImpl(androidContext(), get()) }
}
