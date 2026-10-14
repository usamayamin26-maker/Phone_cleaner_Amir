package ai.jhw.junkcleaner.domain.repository

import ai.jhw.junkcleaner.model.JunkItem
import ai.jhw.junkcleaner.model.JunkScanEvent
import ai.jhw.storage.model.DeleteResult
import kotlinx.coroutines.flow.Flow

/**
 * The Smart Scan. Scanning never deletes anything; cancelling the collecting coroutine ("Stop
 * Scanning") stops the walk at once. Everything needs All files access.
 */
interface JunkRepository {

    fun scan(): Flow<JunkScanEvent>

    /**
     * Deletes what the user confirmed. Folder items are emptied from the inside out; a residual
     * folder is re-checked first and skipped if its app has been installed since the scan.
     */
    suspend fun delete(items: List<JunkItem>): DeleteResult
}
