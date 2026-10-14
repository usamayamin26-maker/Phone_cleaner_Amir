package ai.jhw.whatsapp.di

import org.koin.dsl.module

/**
 * Koin module for the whatsapp layer: WhatsApp and WhatsApp Business media - images, videos,
 * audio, documents, voice notes and statuses.
 *
 * Registered in BaseApp next to the app's SharedModule. Repositories go here as
 * `single<Repository> { RepositoryImpl(...) }`; the app wraps them in its own repositories and
 * never calls this module from an activity.
 */
val whatsAppModule = module {
}
