package com.phone.cleaner.domain.view_models

import ai.jhw.duplicates.model.DuplicateGroup
import ai.jhw.duplicates.model.DuplicateScanEvent
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import android.content.IntentSender
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.DuplicatesSection
import com.phone.cleaner.domain.repository.DuplicatesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DuplicatesUiState(
    val isScanning: Boolean = false,
    /** The scan has finished and the results are in. */
    val isScanComplete: Boolean = false,
    /** 0..1 while scanning. */
    val progress: Float = 0f,
    val imageGroups: List<DuplicateGroup> = emptyList(),
    val videoGroups: List<DuplicateGroup> = emptyList(),
    val largeFiles: List<FileItem> = emptyList(),
    /** What the Delete buttons delete, by path. A file in two sections is selected once. */
    val selectedPaths: Set<String> = emptySet(),
    /** Sections where "Keep best one" is on: every group's extras selected, never its best. */
    val keepBest: Set<DuplicatesSection> = setOf(DuplicatesSection.IMAGES, DuplicatesSection.VIDEOS),
    val isDeleting: Boolean = false,
    val error: String? = null,
) {
    fun groups(section: DuplicatesSection): List<DuplicateGroup> = when (section) {
        DuplicatesSection.IMAGES -> imageGroups
        DuplicatesSection.VIDEOS -> videoGroups
        DuplicatesSection.LARGE_FILES -> emptyList()
    }

    /** Every file the section shows. */
    fun files(section: DuplicatesSection): List<FileItem> = when (section) {
        DuplicatesSection.LARGE_FILES -> largeFiles
        else -> groups(section).flatMap { it.files }
    }

    /** The selected files of one section, or of every section when [section] is null. */
    fun selected(section: DuplicatesSection? = null): List<FileItem> {
        val sections = section?.let(::listOf) ?: DuplicatesSection.entries
        return sections.flatMap(::files).filter { it.path in selectedPaths }.distinctBy { it.path }
    }

    /** The results screen's section check: on while anything in the section is selected. */
    fun isIncluded(section: DuplicatesSection): Boolean = files(section).any { it.path in selectedPaths }

    fun isAllSelected(section: DuplicatesSection): Boolean {
        val files = files(section)
        return files.isNotEmpty() && files.all { it.path in selectedPaths }
    }

    /** What the section can free: the extras of its groups, or the large files themselves. */
    fun reclaimableBytes(section: DuplicatesSection): Long = when (section) {
        DuplicatesSection.LARGE_FILES -> largeFiles.sumOf { it.sizeBytes }
        else -> groups(section).sumOf { it.wastedBytes }
    }

    val isEmpty: Boolean get() = DuplicatesSection.entries.all { files(it).isEmpty() }
}

sealed interface DuplicatesUiEvent {
    /** Show "Are You Sure?" naming what is about to go. */
    data class ConfirmDelete(val count: Int, val bytes: Long) : DuplicatesUiEvent

    /** Android 11+ without All files access: launch the system's own delete confirmation. */
    data class RequestConsent(val intentSender: IntentSender) : DuplicatesUiEvent

    data class Deleted(val freedBytes: Long, val failedCount: Int) : DuplicatesUiEvent
}

/**
 * Delete Duplicates, shared by its three pages (scanning, results, a section's details): the scan,
 * the selection and the delete flow.
 *
 * Nothing is deleted without [onDeleteConfirmed], which only the "Are You Sure?" dialog calls. A
 * stopped scan is a cancelled job and deletes nothing.
 */
class DuplicatesViewModel(
    private val repository: DuplicatesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DuplicatesUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<DuplicatesUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var scanJob: Job? = null
    private var deleteJob: Job? = null

    /** Whether a scan was ever started, so access granted twice (a rotation) scans once. */
    var hasStarted: Boolean = false
        private set

    /** Confirmed in the dialog, waiting for the delete (and maybe the system's consent). */
    private var pendingDelete: List<FileItem> = emptyList()

    fun startScan() {
        hasStarted = true
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _state.value = DuplicatesUiState(isScanning = true)
            try {
                repository.scan().collect { event ->
                    when (event) {
                        is DuplicateScanEvent.Progress -> _state.update { it.copy(progress = event.fraction) }
                        is DuplicateScanEvent.Completed -> {
                            val groups = event.result.groups
                            val large = repository.getLargeFiles()
                            _state.update { state ->
                                val withResults = state.copy(
                                    imageGroups = groups.filter { it.kind == FileKind.IMAGE },
                                    videoGroups = groups.filter { it.kind == FileKind.VIDEO },
                                    largeFiles = large.data.orEmpty(),
                                    error = large.message,
                                )
                                // "Keep best one" starts on: every copy but the best is selected.
                                withResults.copy(
                                    isScanning = false,
                                    isScanComplete = true,
                                    progress = 1f,
                                    selectedPaths = extrasOf(withResults.imageGroups + withResults.videoGroups),
                                )
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Scan stopped; nothing was deleted")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Scan failed", e)
                _state.update { it.copy(isScanning = false, error = e.message ?: "Scan failed") }
            }
        }
    }

    /** "Stop Scanning": the scan is cancelled and nothing is deleted. */
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    /**
     * The section's check on the results screen (Figma 1:579): on means the section takes part in
     * "Delete". Turning it off clears the section; turning it on selects its safe default - every
     * group's extras, never a best copy - and large files all.
     */
    fun onSectionCheckClicked(section: DuplicatesSection) = _state.update { state ->
        val paths = state.files(section).map { it.path }.toSet()
        when {
            state.isIncluded(section) ->
                state.copy(selectedPaths = state.selectedPaths - paths)
            section.hasGroups -> state.copy(
                selectedPaths = state.selectedPaths + extrasOf(state.groups(section)),
                keepBest = state.keepBest + section,
            )
            else -> state.copy(selectedPaths = state.selectedPaths + paths)
        }
    }

    /** "Select All" on a section's page: every file, best copies included, or none. */
    fun onSelectAllClicked(section: DuplicatesSection) = _state.update { state ->
        val paths = state.files(section).map { it.path }.toSet()
        if (state.isAllSelected(section)) {
            state.copy(selectedPaths = state.selectedPaths - paths, keepBest = state.keepBest - section)
        } else {
            // Everything, best copies included - which is what "Keep best one" being off means.
            state.copy(selectedPaths = state.selectedPaths + paths, keepBest = state.keepBest - section)
        }
    }

    fun onKeepBestChanged(section: DuplicatesSection, keepBest: Boolean) = _state.update { state ->
        if (!section.hasGroups) return@update state
        if (keepBest) {
            val groups = state.groups(section)
            val all = groups.flatMap { g -> g.files.map { it.path } }.toSet()
            state.copy(
                selectedPaths = state.selectedPaths - all + extrasOf(groups),
                keepBest = state.keepBest + section,
            )
        } else {
            state.copy(keepBest = state.keepBest - section)
        }
    }

    fun onFileClicked(section: DuplicatesSection, file: FileItem) = _state.update { state ->
        val selecting = file.path !in state.selectedPaths
        val isBest = state.groups(section).any { it.best.path == file.path }
        state.copy(
            selectedPaths = if (selecting) state.selectedPaths + file.path else state.selectedPaths - file.path,
            // Picking a best copy by hand means the user no longer keeps it.
            keepBest = if (selecting && isBest) state.keepBest - section else state.keepBest,
        )
    }

    /** A Delete button: one section's selection, or everything selected when [section] is null. */
    fun onDeleteClicked(section: DuplicatesSection?) {
        val files = _state.value.selected(section)
        if (files.isEmpty() || _state.value.isDeleting) return
        pendingDelete = files
        emit(DuplicatesUiEvent.ConfirmDelete(files.size, files.sumOf { it.sizeBytes }))
    }

    /** The "Are You Sure?" dialog's Delete - the only way to a delete. */
    fun onDeleteConfirmed() {
        val files = pendingDelete
        if (files.isEmpty()) return
        deleteJob?.cancel()
        deleteJob = viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (val result = repository.delete(files)) {
                is ApiResult.Failure -> finishWithError(result.message)
                is ApiResult.Success -> when (val outcome = result.data) {
                    is DeleteOutcome.Completed -> applyDeleted(outcome.result)
                    is DeleteOutcome.NeedsUserConsent -> {
                        pendingDelete = outcome.items
                        emit(DuplicatesUiEvent.RequestConsent(outcome.intentSender))
                    }
                    null -> finishWithError(null)
                }
            }
        }
    }

    fun onDeleteCancelled() {
        pendingDelete = emptyList()
    }

    /** The system's consent dialog answered. Declined: nothing was deleted. */
    fun onConsentResult(approved: Boolean) {
        val files = pendingDelete
        if (!approved) {
            Log.d(TAG, "Delete consent declined; nothing was deleted")
            pendingDelete = emptyList()
            _state.update { it.copy(isDeleting = false) }
            return
        }
        deleteJob?.cancel()
        deleteJob = viewModelScope.launch {
            when (val result = repository.confirmDeleted(files)) {
                is ApiResult.Failure -> finishWithError(result.message)
                is ApiResult.Success -> result.data?.let { applyDeleted(it) } ?: finishWithError(null)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /** Drops what is gone from every list and the selection, then reports what was freed. */
    private fun applyDeleted(result: DeleteResult) {
        val gone = result.deletedPaths.toSet()
        pendingDelete = emptyList()
        _state.update { state ->
            fun prune(groups: List<DuplicateGroup>) = groups.mapNotNull { group ->
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
            state.copy(
                imageGroups = prune(state.imageGroups),
                videoGroups = prune(state.videoGroups),
                largeFiles = state.largeFiles.filterNot { it.path in gone },
                selectedPaths = state.selectedPaths - gone,
                isDeleting = false,
            )
        }
        emit(DuplicatesUiEvent.Deleted(result.freedBytes, result.failedPaths.size))
    }

    private fun finishWithError(message: String?) {
        pendingDelete = emptyList()
        _state.update { it.copy(isDeleting = false, error = message ?: "Delete failed") }
    }

    private fun extrasOf(groups: List<DuplicateGroup>): Set<String> =
        groups.flatMap { group -> group.extras.map { it.path } }.toSet()

    private fun emit(event: DuplicatesUiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    private companion object {
        const val TAG = "DuplicatesViewModel"
    }
}
