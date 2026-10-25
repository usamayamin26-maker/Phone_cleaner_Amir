package com.phone.cleaner.domain.repository

import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.whatsapp.model.WhatsAppScanEvent
import com.phone.cleaner.domain.entities.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * WA (WhatsApp) Cleaner: the media folders of WhatsApp and WhatsApp Business, by type. Needs All
 * files access. Scanning never deletes; the chats database and backups are never touched.
 */
interface WhatsAppRepository {

    /** Progress, then the media by type. Cancelling the collection stops the scan. */
    fun scan(): Flow<WhatsAppScanEvent>

    /** Deletes [files] and drops them from the AI verdict cache, which may have named them too. */
    suspend fun delete(files: List<FileItem>): ApiResult<DeleteResult>
}
