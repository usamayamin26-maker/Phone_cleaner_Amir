package com.phone.cleaner.domain.view_models

import ai.jhw.aicleaner.model.AiScanEvent
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.CleanerSection
import com.phone.cleaner.domain.entities.CleanerSectionType
import com.phone.cleaner.domain.repository.AiCleanerRepository
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * AI Junk Cleaner (Figma 1:3 / 1:349 / 1:48 / 1:142 / 1:208): the model's junk photos and videos,
 * grouped by the folder they are in, and the large files. The model's verdicts start selected;
 * large files are the user's own and start unselected.
 */
class AiCleanerViewModel(
    private val repository: AiCleanerRepository,
) : CleanerViewModel() {

    override val tag: String = TAG

    override suspend fun scan() {
        var aiJunk: List<FileItem> = emptyList()
        repository.scan().collect { event ->
            when (event) {
                is AiScanEvent.Progress -> mutableState.update { it.copy(progress = event.fraction) }
                is AiScanEvent.Completed -> aiJunk = event.junk
                is AiScanEvent.Classified -> Unit
            }
        }
        val large = repository.getLargeFiles()
        mutableState.update { state ->
            state.copy(
                isScanning = false,
                isScanComplete = true,
                progress = 1f,
                sections = sectionsOf(aiJunk, large.data.orEmpty()),
                selectedPaths = aiJunk.map { it.path }.toSet(),
                error = large.message,
            )
        }
    }

    override suspend fun delete(files: List<FileItem>): ApiResult<DeleteOutcome> = repository.delete(files)

    override suspend fun confirmDeleted(files: List<FileItem>): ApiResult<DeleteResult> =
        repository.confirmDeleted(files)

    /** The screen is gone for good: free the model. */
    override fun onCleared() {
        repository.release()
    }

    private fun sectionsOf(aiJunk: List<FileItem>, large: List<FileItem>) = buildList {
        val images = aiJunk.filter { it.kind == FileKind.IMAGE }
        val videos = aiJunk.filter { it.kind == FileKind.VIDEO }
        if (images.isNotEmpty()) {
            add(CleanerSection(CleanerSectionType.IMAGES, images, groups = folderGroups(CleanerSectionType.IMAGES, images)))
        }
        if (videos.isNotEmpty()) {
            add(CleanerSection(CleanerSectionType.VIDEOS, videos, groups = folderGroups(CleanerSectionType.VIDEOS, videos)))
        }
        if (large.isNotEmpty()) add(CleanerSection(CleanerSectionType.LARGE_FILES, large))
    }

    /**
     * The design's groups inside Images (Figma 1:48): Downloads, Gallery and WA Photos. Anything
     * that is neither downloaded nor from WhatsApp is the gallery.
     */
    private fun folderGroups(parent: CleanerSectionType, files: List<FileItem>): List<CleanerSection> {
        val whatsApp = if (parent == CleanerSectionType.IMAGES) CleanerSectionType.WA_PHOTOS else CleanerSectionType.WA_VIDEOS
        val byGroup = files.groupBy { file ->
            val path = file.path.lowercase(Locale.ROOT)
            when {
                "/whatsapp" in path -> whatsApp
                "/download/" in path -> CleanerSectionType.DOWNLOADS
                else -> CleanerSectionType.GALLERY
            }
        }
        return listOf(CleanerSectionType.DOWNLOADS, CleanerSectionType.GALLERY, whatsApp).mapNotNull { type ->
            byGroup[type]?.let { CleanerSection(type, it, key = CleanerSection.groupKey(parent, type)) }
        }
    }

    private companion object {
        const val TAG = "AiCleanerViewModel"
    }
}
