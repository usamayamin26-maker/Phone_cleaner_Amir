package com.phone.cleaner.domain.repository

import ai.jhw.aicleaner.model.AiScanEvent
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import com.phone.cleaner.domain.entities.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * AI (Artificial Intelligence) Junk Cleaner: the on-device model's verdicts on forwarded photos
 * and videos, the screenshots and large files shown beside them, and the delete path. Scanning
 * never deletes.
 */
interface AiCleanerRepository {

    /**
     * Progress, each classification, then everything judged junk. Verdicts already in the cache
     * are reused while the file is unchanged; every new verdict is stored, and files no longer on
     * the phone are dropped from the cache. Cancelling the collection stops the scan.
     */
    fun scan(): Flow<AiScanEvent>

    /** The photos and videos in the Screenshots folders, newest first - no AI verdict. */
    suspend fun getScreenshots(): ApiResult<List<FileItem>>

    /** Files of 50 MB and more, largest first. */
    suspend fun getLargeFiles(): ApiResult<List<FileItem>>

    /**
     * Deletes [files] through the storage layer and drops them from the verdict cache. Without All
     * files access Android 11+ returns [DeleteOutcome.NeedsUserConsent] and the screen asks the user.
     */
    suspend fun delete(files: List<FileItem>): ApiResult<DeleteOutcome>

    /** After the user approved the system's consent dialog: which of [files] are gone. */
    suspend fun confirmDeleted(files: List<FileItem>): ApiResult<DeleteResult>

    /** Frees the model - called when the screen is left for good. */
    fun release()
}
