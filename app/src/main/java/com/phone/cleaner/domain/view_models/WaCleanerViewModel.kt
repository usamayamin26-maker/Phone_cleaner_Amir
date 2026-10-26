package com.phone.cleaner.domain.view_models

import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.whatsapp.model.WhatsAppMediaType
import ai.jhw.whatsapp.model.WhatsAppScanEvent
import ai.jhw.whatsapp.model.WhatsAppScanResult
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.CleanerSection
import com.phone.cleaner.domain.entities.CleanerSectionType
import com.phone.cleaner.domain.repository.WhatsAppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update

/**
 * WA (WhatsApp) Cleaner (Figma 1:997 / 1:1042 / 1:1160 / 1:1526): the design's Images, Videos and
 * Large Files, then documents ("Files") and the other media types WhatsApp keeps, each only when
 * it has files. Everything here is the user's own chat media, so nothing starts selected.
 */
class WaCleanerViewModel(
    private val repository: WhatsAppRepository,
) : CleanerViewModel() {

    override val tag: String = TAG

    /**
     * Debug builds only: a pause after each step of the scan, so the scanning page can be seen
     * and checked (a real scan finishes in under a second). Release builds never set it.
     */
    var debugStepDelayMs: Long = 0L

    override suspend fun scan() {
        repository.scan().collect { event ->
            if (debugStepDelayMs > 0) delay(debugStepDelayMs)
            when (event) {
                is WhatsAppScanEvent.Progress -> mutableState.update { it.copy(progress = event.fraction) }
                is WhatsAppScanEvent.Completed -> mutableState.update { state ->
                    state.copy(
                        isScanning = false,
                        isScanComplete = true,
                        progress = 1f,
                        sections = sectionsOf(event.result),
                        error = if (event.result.needsFullAccess) "All files access is missing" else null,
                    )
                }
            }
        }
    }

    override suspend fun delete(files: List<FileItem>): ApiResult<DeleteOutcome> =
        when (val result = repository.delete(files)) {
            is ApiResult.Failure -> ApiResult.Failure(result.message ?: "Delete failed")
            is ApiResult.Success -> result.data?.let { ApiResult.Success(DeleteOutcome.Completed(it)) }
                ?: ApiResult.Failure("Delete failed")
        }

    /** WhatsApp media is deleted by path with All files access; Android never asks for consent. */
    override suspend fun confirmDeleted(files: List<FileItem>): ApiResult<DeleteResult> =
        ApiResult.Success(DeleteResult(emptyList(), emptyList(), 0))

    private fun sectionsOf(result: WhatsAppScanResult): List<CleanerSection> {
        fun files(type: WhatsAppMediaType) = result.categories.firstOrNull { it.type == type }?.files.orEmpty()
        return listOf(
            CleanerSectionType.IMAGES to files(WhatsAppMediaType.IMAGES),
            CleanerSectionType.VIDEOS to files(WhatsAppMediaType.VIDEOS),
            CleanerSectionType.LARGE_FILES to result.largeFiles(),
            CleanerSectionType.DOCUMENTS to files(WhatsAppMediaType.DOCUMENTS),
            CleanerSectionType.VOICE_NOTES to files(WhatsAppMediaType.VOICE_NOTES),
            CleanerSectionType.AUDIO to files(WhatsAppMediaType.AUDIO),
            CleanerSectionType.GIFS to files(WhatsAppMediaType.GIFS),
            CleanerSectionType.STICKERS to files(WhatsAppMediaType.STICKERS),
            CleanerSectionType.STATUSES to files(WhatsAppMediaType.STATUSES),
        ).filter { (_, files) -> files.isNotEmpty() }
            .map { (type, files) -> CleanerSection(type, files) }
    }

    private companion object {
        const val TAG = "WaCleanerViewModel"
    }
}
