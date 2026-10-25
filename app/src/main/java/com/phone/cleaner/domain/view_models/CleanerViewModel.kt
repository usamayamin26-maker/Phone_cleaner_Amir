package com.phone.cleaner.domain.view_models

import ai.jhw.duplicates.model.DuplicateGroup
import ai.jhw.junkcleaner.model.JunkCategory
import ai.jhw.storage.model.DeleteOutcome
import ai.jhw.storage.model.DeleteResult
import ai.jhw.storage.model.FileItem
import android.content.IntentSender
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.CleanerSection
import com.phone.cleaner.domain.entities.CleanerSectionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What every cleaner screen (Delete Duplicates, AI Junk Cleaner, WA Cleaner) shows: the scan's
 * progress, then its sections and the selection the Delete buttons act on.
 */
data class CleanerUiState(
    val isScanning: Boolean = false,
    /** The scan has finished and the results are in. */
    val isScanComplete: Boolean = false,
    /** 0..1 while scanning. */
    val progress: Float = 0f,
    val sections: List<CleanerSection> = emptyList(),
    /** What the Delete buttons delete, by path. A file in two sections is selected once. */
    val selectedPaths: Set<String> = emptySet(),
    val isDeleting: Boolean = false,
    val error: String? = null,
    /** Delete Duplicates: each media section's groups of identical copies. */
    val duplicateGroups: Map<CleanerSectionType, List<DuplicateGroup>> = emptyMap(),
    /** Delete Duplicates: sections where "Keep best one" is on. */
    val keepBest: Set<CleanerSectionType> = emptySet(),
    /** AI Junk Cleaner: the Smart Scan's categories behind the Junk Files section. */
    val junkCategories: List<JunkCategory> = emptyList(),
) {
    fun section(key: String): CleanerSection? = sections.firstNotNullOfOrNull { it.find(key) }

    /** Every file the section (or group) shows. */
    fun files(key: String): List<FileItem> = section(key)?.files.orEmpty()

    /** The selected files of one section, or of every section when [key] is null. */
    fun selected(key: String? = null): List<FileItem> {
        val files = if (key != null) files(key) else sections.flatMap { it.files }
        return files.filter { it.path in selectedPaths }.distinctBy { it.path }
    }

    /** The results screen's section check: on while anything in the section is selected. */
    fun isIncluded(key: String): Boolean = files(key).any { it.path in selectedPaths }

    fun isAllSelected(key: String): Boolean {
        val files = files(key)
        return files.isNotEmpty() && files.all { it.path in selectedPaths }
    }

    /** Nothing was found - a section that only asks for access does not count. */
    val isEmpty: Boolean get() = sections.all { it.files.isEmpty() }
}

sealed interface CleanerUiEvent {
    /** Show "Are You Sure?" naming what is about to go. */
    data class ConfirmDelete(val count: Int, val bytes: Long) : CleanerUiEvent

    /** Android 11+ without All files access: launch the system's own delete confirmation. */
    data class RequestConsent(val intentSender: IntentSender) : CleanerUiEvent

    data class Deleted(val freedBytes: Long, val failedCount: Int) : CleanerUiEvent
}

/**
 * The scan, selection and delete flow every cleaner screen shares; each cleaner supplies its own
 * scan and delete calls.
 *
 * Nothing is deleted without [onDeleteConfirmed], which only the "Are You Sure?" dialog calls. A
 * stopped scan is a cancelled job and deletes nothing.
 */
abstract class CleanerViewModel : ViewModel() {

    protected val mutableState = MutableStateFlow(CleanerUiState())
    val state = mutableState.asStateFlow()

    private val _events = MutableSharedFlow<CleanerUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var scanJob: Job? = null
    private var deleteJob: Job? = null

    /** Whether a scan was ever started, so access granted twice (a rotation) scans once. */
    var hasStarted: Boolean = false
        private set

    /** Confirmed in the dialog, waiting for the delete (and maybe the system's consent). */
    private var pendingDelete: List<FileItem> = emptyList()

    /** Runs the scan, reporting through [mutableState]; ends with [CleanerUiState.isScanComplete]. */
    protected abstract suspend fun scan()

    protected abstract suspend fun delete(files: List<FileItem>): ApiResult<DeleteOutcome>

    /** After the system's consent dialog was approved: which of [files] are gone. */
    protected abstract suspend fun confirmDeleted(files: List<FileItem>): ApiResult<DeleteResult>

    /** The log tag of the screen's own ViewModel. */
    protected abstract val tag: String

    fun startScan() {
        hasStarted = true
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            mutableState.value = CleanerUiState(isScanning = true)
            try {
                scan()
            } catch (e: CancellationException) {
                Log.d(tag, "Scan stopped; nothing was deleted")
                throw e
            } catch (e: Exception) {
                Log.e(tag, "Scan failed", e)
                mutableState.update { it.copy(isScanning = false, error = e.message ?: "Scan failed") }
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
     * "Delete". Turning it off clears the section; turning it on selects its safe default.
     */
    open fun onSectionCheckClicked(key: String) = mutableState.update { state ->
        val paths = state.files(key).map { it.path }.toSet()
        if (state.isIncluded(key)) {
            state.copy(selectedPaths = state.selectedPaths - paths)
        } else {
            state.copy(selectedPaths = state.selectedPaths + defaultSelection(state, key))
        }
    }

    /** "Select All" on a section's page: every file of it, or none. */
    open fun onSelectAllClicked(key: String) = mutableState.update { state ->
        val paths = state.files(key).map { it.path }.toSet()
        if (state.isAllSelected(key)) {
            state.copy(selectedPaths = state.selectedPaths - paths)
        } else {
            state.copy(selectedPaths = state.selectedPaths + paths)
        }
    }

    open fun onFileClicked(key: String, file: FileItem) = mutableState.update { state ->
        val selecting = file.path !in state.selectedPaths
        state.copy(selectedPaths = if (selecting) state.selectedPaths + file.path else state.selectedPaths - file.path)
    }

    /** A Delete button: one section's selection, or everything selected when [key] is null. */
    fun onDeleteClicked(key: String?) {
        val files = mutableState.value.selected(key)
        if (files.isEmpty() || mutableState.value.isDeleting) return
        pendingDelete = files
        emit(CleanerUiEvent.ConfirmDelete(files.size, files.sumOf { it.sizeBytes }))
    }

    /** The "Are You Sure?" dialog's Delete - the only way to a delete. */
    fun onDeleteConfirmed() {
        val files = pendingDelete
        if (files.isEmpty()) return
        deleteJob?.cancel()
        deleteJob = viewModelScope.launch {
            mutableState.update { it.copy(isDeleting = true) }
            when (val result = delete(files)) {
                is ApiResult.Failure -> finishWithError(result.message)
                is ApiResult.Success -> when (val outcome = result.data) {
                    is DeleteOutcome.Completed -> applyDeleted(outcome.result)
                    is DeleteOutcome.NeedsUserConsent -> {
                        pendingDelete = outcome.items
                        emit(CleanerUiEvent.RequestConsent(outcome.intentSender))
                    }
                    null -> finishWithError(null)
                }
            }
        }
    }

    fun onDeleteCancelled() {
        pendingDelete = emptyList()
    }

    /** The system's consent dialog answered. Declined: nothing more was deleted. */
    fun onConsentResult(approved: Boolean) {
        val files = pendingDelete
        deleteJob?.cancel()
        deleteJob = viewModelScope.launch {
            if (!approved) Log.d(tag, "Delete consent declined; nothing was deleted")
            when (val result = if (approved) confirmDeleted(files) else consentDeclined(files)) {
                is ApiResult.Failure -> finishWithError(result.message)
                is ApiResult.Success -> result.data?.let { applyDeleted(it) } ?: finishWithError(null)
            }
        }
    }

    fun clearError() {
        mutableState.update { it.copy(error = null) }
    }

    /** What turning a section's check on selects. Every file, unless the screen knows better. */
    protected open fun defaultSelection(state: CleanerUiState, key: String): Set<String> =
        state.files(key).map { it.path }.toSet()

    /**
     * What a declined consent leaves deleted. Nothing, unless the screen already deleted part of
     * the selection itself before asking (the AI cleaner's junk files).
     */
    protected open suspend fun consentDeclined(files: List<FileItem>): ApiResult<DeleteResult> =
        ApiResult.Success(DeleteResult(emptyList(), emptyList(), 0))

    /** Drops [gone] from the state's lists. Sections and groups left empty disappear. */
    protected open fun prune(state: CleanerUiState, gone: Set<String>): CleanerUiState =
        state.copy(sections = state.sections.mapNotNull { it.without(gone) })

    protected fun CleanerSection.without(gone: Set<String>): CleanerSection? {
        val files = files.filterNot { it.path in gone }
        if (files.isEmpty() && !needsAccess) return null
        return copy(files = files, groups = groups.mapNotNull { it.without(gone) })
    }

    /** Drops what is gone from every list and the selection, then reports what was freed. */
    private fun applyDeleted(result: DeleteResult) {
        val gone = result.deletedPaths.toSet()
        pendingDelete = emptyList()
        mutableState.update { state ->
            prune(state, gone).copy(selectedPaths = state.selectedPaths - gone, isDeleting = false)
        }
        // Nothing went and nothing failed: the user declined; there is nothing to report.
        if (result.deletedPaths.isNotEmpty() || result.failedPaths.isNotEmpty()) {
            emit(CleanerUiEvent.Deleted(result.freedBytes, result.failedPaths.size))
        }
    }

    private fun finishWithError(message: String?) {
        pendingDelete = emptyList()
        mutableState.update { it.copy(isDeleting = false, error = message ?: "Delete failed") }
    }

    private fun emit(event: CleanerUiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }
}
