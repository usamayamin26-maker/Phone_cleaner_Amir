package com.phone.cleaner.domain.view_models

import ai.jhw.duplicates.model.DuplicateGroup
import ai.jhw.duplicates.model.DuplicateScanEvent
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.CleanerSection
import com.phone.cleaner.domain.entities.CleanerSectionType
import com.phone.cleaner.domain.repository.DuplicatesRepository
import kotlinx.coroutines.flow.update

/**
 * Delete Duplicates, shared by its three pages (scanning, results, a section's details). On top of
 * the shared cleaner flow it keeps "Keep best one": with it on, every group's extras are selected
 * and never its best copy.
 */
class DuplicatesViewModel(
    private val repository: DuplicatesRepository,
) : CleanerViewModel() {

    override val tag: String = TAG

    override suspend fun scan() {
        repository.scan().collect { event ->
            when (event) {
                is DuplicateScanEvent.Progress -> mutableState.update { it.copy(progress = event.fraction) }
                is DuplicateScanEvent.Completed -> {
                    val groups = mapOf(
                        CleanerSectionType.IMAGES to event.result.groupsOf(FileKind.IMAGE),
                        CleanerSectionType.VIDEOS to event.result.groupsOf(FileKind.VIDEO),
                    )
                    val large = repository.getLargeFiles()
                    mutableState.update { state ->
                        state.copy(
                            isScanning = false,
                            isScanComplete = true,
                            progress = 1f,
                            duplicateGroups = groups,
                            sections = sectionsOf(groups, large.data.orEmpty()),
                            // "Keep best one" starts on: every copy but the best is selected.
                            keepBest = groups.keys,
                            selectedPaths = extrasOf(groups.values.flatten()),
                            error = large.message,
                        )
                    }
                }
            }
        }
    }

    override suspend fun delete(files: List<FileItem>): ApiResult<DeleteOutcome> = repository.delete(files)

    override suspend fun confirmDeleted(files: List<FileItem>): ApiResult<DeleteResult> =
        repository.confirmDeleted(files)

    /** Turning a media section on selects its safe default: every group's extras, never a best copy. */
    override fun defaultSelection(state: CleanerUiState, key: String): Set<String> {
        val groups = state.duplicateGroups[CleanerSectionType.valueOf(key)] ?: return super.defaultSelection(state, key)
        return extrasOf(groups)
    }

    override fun onSectionCheckClicked(key: String) {
        val type = CleanerSectionType.valueOf(key)
        val turningOn = !state.value.isIncluded(key)
        super.onSectionCheckClicked(key)
        if (turningOn && type in state.value.duplicateGroups) {
            mutableState.update { it.copy(keepBest = it.keepBest + type) }
        }
    }

    /** "Select All": every file, best copies included - which is what "Keep best one" off means. */
    override fun onSelectAllClicked(key: String) {
        super.onSelectAllClicked(key)
        mutableState.update { it.copy(keepBest = it.keepBest - CleanerSectionType.valueOf(key)) }
    }

    override fun onFileClicked(key: String, file: FileItem) {
        val type = CleanerSectionType.valueOf(key)
        val selecting = file.path !in state.value.selectedPaths
        val isBest = state.value.duplicateGroups[type].orEmpty().any { it.best.path == file.path }
        super.onFileClicked(key, file)
        // Picking a best copy by hand means the user no longer keeps it.
        if (selecting && isBest) mutableState.update { it.copy(keepBest = it.keepBest - type) }
    }

    fun onKeepBestChanged(type: CleanerSectionType, keepBest: Boolean) = mutableState.update { state ->
        val groups = state.duplicateGroups[type] ?: return@update state
        if (keepBest) {
            val all = groups.flatMap { g -> g.files.map { it.path } }.toSet()
            state.copy(selectedPaths = state.selectedPaths - all + extrasOf(groups), keepBest = state.keepBest + type)
        } else {
            state.copy(keepBest = state.keepBest - type)
        }
    }

    /** Groups lose what is gone; a group left with one copy is no longer a duplicate. */
    override fun prune(state: CleanerUiState, gone: Set<String>): CleanerUiState {
        val groups = state.duplicateGroups.mapValues { (_, groups) ->
            groups.mapNotNull { group ->
                val left = group.files.filterNot { it.path in gone }
                when {
                    left.size < 2 -> null
                    left.size == group.files.size -> group
                    // The best copy may be gone too; keep pointing at a surviving one.
                    else -> group.copy(
                        files = left,
                        bestIndex = left.indexOfFirst { it.path == group.best.path }.coerceAtLeast(0),
                    )
                }
            }
        }
        val large = state.files(CleanerSectionType.LARGE_FILES.name).filterNot { it.path in gone }
        return state.copy(duplicateGroups = groups, sections = sectionsOf(groups, large))
    }

    private fun sectionsOf(groups: Map<CleanerSectionType, List<DuplicateGroup>>, large: List<FileItem>) =
        buildList {
            groups.forEach { (type, list) ->
                if (list.isNotEmpty()) {
                    add(CleanerSection(type, list.flatMap { it.files }, wastedBytes = list.sumOf { it.wastedBytes }))
                }
            }
            if (large.isNotEmpty()) add(CleanerSection(CleanerSectionType.LARGE_FILES, large))
        }

    private fun extrasOf(groups: List<DuplicateGroup>): Set<String> =
        groups.flatMap { group -> group.extras.map { it.path } }.toSet()

    private companion object {
        const val TAG = "DuplicatesViewModel"
    }
}
