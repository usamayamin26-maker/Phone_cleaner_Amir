package ai.jhw.whatsapp.domain.repository

import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.whatsapp.model.WhatsAppScanEvent
import kotlinx.coroutines.flow.Flow

/**
 * WA (WhatsApp) Cleaner. Reads WhatsApp's and WhatsApp Business's media folders; never touches
 * the chats database or backups. Needs All files access. Cancelling the collector stops the scan.
 */
interface WhatsAppRepository {

    fun scan(): Flow<WhatsAppScanEvent>

    suspend fun delete(files: List<FileItem>): DeleteResult
}
