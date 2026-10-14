package ai.jhw.whatsapp.di

import ai.jhw.whatsapp.domain.repository.WhatsAppRepository
import ai.jhw.whatsapp.repo.WhatsAppRepositoryImpl
import org.koin.dsl.module

/**
 * Koin module for the whatsapp layer: WhatsApp and WhatsApp Business media by type. Needs
 * `storageModule` for [ai.jhw.storage.domain.repository.StorageRepository].
 */
val whatsAppModule = module {
    single<WhatsAppRepository> { WhatsAppRepositoryImpl(get()) }
}
