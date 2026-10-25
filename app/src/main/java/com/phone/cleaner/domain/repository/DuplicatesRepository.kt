package com.phone.cleaner.domain.repository

import ai.jhw.duplicates.model.DuplicateScanEvent
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import com.phone.cleaner.domain.entities.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * Delete Duplicates: duplicate photos and videos, the large files shown beside them, and the
 * delete path. Scanning never deletes; deleting happens only after the screen's "Are You Sure?".
 */
interface DuplicatesRepository {

    /** Progress, then the duplicate groups. Cancelling the collection stops the scan. */
    fun scan(): Flow<DuplicateScanEvent>

    /** Files of 50 MB and more, largest first. */
    suspend fun getLargeFiles(): ApiResult<List<FileItem>>

    /**
     * Deletes [files] through the storage layer. With All files access they go at once; without
     * it Android 11+ returns [DeleteOutcome.NeedsUserConsent] and the screen asks the user.
     */
    suspend fun delete(files: List<FileItem>): ApiResult<DeleteOutcome>

    /** After the user approved the system's consent dialog: which of [files] are gone. */
    suspend fun confirmDeleted(files: List<FileItem>): ApiResult<DeleteResult>
}
