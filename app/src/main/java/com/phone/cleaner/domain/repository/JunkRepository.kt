package com.phone.cleaner.domain.repository

import ai.jhw.junkcleaner.model.JunkItem
import ai.jhw.junkcleaner.model.JunkScanEvent
import ai.jhw.storage.model.DeleteResult
import com.phone.cleaner.domain.entities.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * The Smart Scan's junk: APK (Android Package) files, temp files, thumbnails, empty folders and
 * residual files. Needs All files access; without it every category comes back unavailable.
 * Scanning never deletes.
 */
interface JunkRepository {

    /** Progress, then the categories. Cancelling the collection stops the walk. */
    fun scan(): Flow<JunkScanEvent>

    /** Deletes what the user confirmed and drops every deleted path from the AI verdict cache. */
    suspend fun delete(items: List<JunkItem>): ApiResult<DeleteResult>
}
