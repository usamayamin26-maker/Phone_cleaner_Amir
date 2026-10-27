package com.phone.cleaner.domain.view_models

import ai.jhw.aicleaner.model.AiScanEvent
import ai.jhw.junkcleaner.model.JunkItem
import ai.jhw.junkcleaner.model.JunkScanEvent
import ai.jhw.junkcleaner.model.JunkScanResult
import ai.jhw.junkcleaner.model.UnavailableReason
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import com.phone.cleaner.data.mapper.toFileItem
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.CleanerSection
import com.phone.cleaner.domain.entities.CleanerSectionType
import com.phone.cleaner.domain.repository.AiCleanerRepository
import com.phone.cleaner.domain.repository.JunkRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * AI Junk Cleaner (Figma 1:3 / 1:349 / 1:48 / 1:142 / 1:208): the model's junk photos and videos
 * from WhatsApp and Download, grouped by the folder they are in, the screenshots, the large files,
 * and the Smart Scan's junk files when All files access is granted.
 *
 * Only the Smart Scan's recommended junk starts selected. The model's verdicts never do, in any
 * section or group: a guess about someone's photographs is a suggestion to look at, not a
 * selection to delete, so the user ticks what goes and Delete stays disabled until they have.
 * Screenshots and large files are the user's own and start unselected for the same reason.
 */
class AiCleanerViewModel(
    private val repository: AiCleanerRepository,
    private val junkRepository: JunkRepository,
) : CleanerViewModel() {

    override val tag: String = TAG

    /** The junk behind the Junk Files section, by path - deleted through the junk repository. */
    private var junkItems: Map<String, JunkItem> = emptyMap()

    /** Junk already deleted while the system's consent for the media of the same selection is open. */
    private var junkDeleted: DeleteResult? = null

    /** Both scans run at once; Stop Scanning cancels both. */
    override suspend fun scan() = coroutineScope {
        var aiFraction = 0f
        var junkFraction = 0f
        // The slower of the two, so the ring never runs ahead of the work still to do.
        fun report() = mutableState.update { it.copy(progress = minOf(aiFraction, junkFraction)) }

        val junkScan = async {
            var result: JunkScanResult? = null
            junkRepository.scan().collect { event ->
                when (event) {
                    is JunkScanEvent.Progress -> junkFraction = event.fraction
                    is JunkScanEvent.Completed -> {
                        junkFraction = 1f
                        result = event.result
                    }
                }
                report()
            }
            result
        }

        var aiJunk: List<FileItem> = emptyList()
        repository.scan().collect { event ->
            when (event) {
                is AiScanEvent.Progress -> {
                    aiFraction = event.fraction
                    report()
                }
                is AiScanEvent.Completed -> aiJunk = event.junk
                is AiScanEvent.Classified -> Unit
            }
        }
        val screenshots = repository.getScreenshots()
        val large = repository.getLargeFiles()
        val junk = junkScan.await()

        val categories = junk?.categories.orEmpty()
        junkItems = categories.filter { it.isAvailable }.flatMap { it.items }.associateBy { it.path }
        val sections = sectionsOf(aiJunk, screenshots.data.orEmpty(), large.data.orEmpty(), junk)
        mutableState.update { state ->
            state.copy(
                isScanning = false,
                isScanComplete = true,
                progress = 1f,
                sections = sections,
                junkCategories = categories,
                selectedPaths = recommendedJunk(),
                error = screenshots.message ?: large.message,
            )
        }
    }

    /** All files access was granted from the Junk Files card: scan again, now with junk. */
    fun onJunkAccessGranted() = startScan()

    override suspend fun delete(files: List<FileItem>): ApiResult<DeleteOutcome> {
        junkDeleted = null
        val (junk, media) = files.partition { it.path in junkItems }
        val junkResult = if (junk.isEmpty()) {
            null
        } else {
            when (val result = junkRepository.delete(junk.mapNotNull { junkItems[it.path] })) {
                is ApiResult.Failure -> return ApiResult.Failure(result.message ?: "Delete failed")
                is ApiResult.Success -> result.data
            }
        }
        if (media.isEmpty()) return ApiResult.Success(DeleteOutcome.Completed(junkResult ?: DeleteResult(emptyList(), emptyList(), 0)))

        return when (val result = repository.delete(media)) {
            // The junk is gone already: report it, and the media as failed.
            is ApiResult.Failure -> if (junkResult != null) {
                ApiResult.Success(DeleteOutcome.Completed(junkResult + DeleteResult(emptyList(), media.map { it.path }, 0)))
            } else {
                result
            }
            is ApiResult.Success -> when (val outcome = result.data) {
                is DeleteOutcome.Completed -> ApiResult.Success(DeleteOutcome.Completed(junkResult + outcome.result))
                is DeleteOutcome.NeedsUserConsent -> {
                    junkDeleted = junkResult
                    result
                }
                null -> result
            }
        }
    }

    override suspend fun confirmDeleted(files: List<FileItem>): ApiResult<DeleteResult> {
        val carried = junkDeleted.also { junkDeleted = null }
        return when (val result = repository.confirmDeleted(files)) {
            is ApiResult.Failure -> carried?.let { ApiResult.Success(it) } ?: result
            is ApiResult.Success -> ApiResult.Success(carried + (result.data ?: DeleteResult(emptyList(), emptyList(), 0)))
        }
    }

    override suspend fun consentDeclined(files: List<FileItem>): ApiResult<DeleteResult> {
        val carried = junkDeleted.also { junkDeleted = null }
        return carried?.let { ApiResult.Success(it) } ?: super.consentDeclined(files)
    }

    /** The Junk Files section's safe default is the recommended junk only. */
    override fun defaultSelection(state: CleanerUiState, key: String): Set<String> =
        if (key == CleanerSectionType.JUNK_FILES.name) recommendedJunk() else super.defaultSelection(state, key)

    override fun prune(state: CleanerUiState, gone: Set<String>): CleanerUiState {
        junkItems = junkItems - gone
        return super.prune(state, gone).copy(
            junkCategories = state.junkCategories.map { category ->
                category.copy(items = category.items.filterNot { it.path in gone })
            },
        )
    }

    /** The screen is gone for good: free the model. */
    override fun onCleared() {
        repository.release()
    }

    private fun recommendedJunk(): Set<String> =
        junkItems.values.filter { it.isRecommended }.map { it.path }.toSet()

    private fun sectionsOf(
        aiJunk: List<FileItem>,
        screenshots: List<FileItem>,
        large: List<FileItem>,
        junk: JunkScanResult?,
    ) = buildList {
        val images = aiJunk.filter { it.kind == FileKind.IMAGE }
        val videos = aiJunk.filter { it.kind == FileKind.VIDEO }
        if (images.isNotEmpty()) {
            add(CleanerSection(CleanerSectionType.IMAGES, images, groups = folderGroups(CleanerSectionType.IMAGES, images)))
        }
        if (videos.isNotEmpty()) {
            add(CleanerSection(CleanerSectionType.VIDEOS, videos, groups = folderGroups(CleanerSectionType.VIDEOS, videos)))
        }
        if (screenshots.isNotEmpty()) add(CleanerSection(CleanerSectionType.SCREENSHOTS, screenshots))
        if (large.isNotEmpty()) add(CleanerSection(CleanerSectionType.LARGE_FILES, large))

        val needsAccess = junk != null && junk.categories.isNotEmpty() &&
            junk.categories.all { it.unavailableReason == UnavailableReason.NEEDS_FULL_ACCESS }
        when {
            needsAccess -> add(CleanerSection(CleanerSectionType.JUNK_FILES, emptyList(), needsAccess = true))
            junkItems.isNotEmpty() ->
                add(CleanerSection(CleanerSectionType.JUNK_FILES, junkItems.values.map { it.toFileItem() }))
        }
    }

    /**
     * The design's groups inside Images (Figma 1:48): Downloads, Gallery and WA Photos. The model
     * only looks at WhatsApp and Download now, so Gallery is left for a file in neither.
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

    private operator fun DeleteResult?.plus(other: DeleteResult): DeleteResult =
        if (this == null) {
            other
        } else {
            DeleteResult(deletedPaths + other.deletedPaths, failedPaths + other.failedPaths, freedBytes + other.freedBytes)
        }

    private companion object {
        const val TAG = "AiCleanerViewModel"
    }
}
