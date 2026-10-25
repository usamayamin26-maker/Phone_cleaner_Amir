package com.phone.cleaner.domain.view_models

import ai.jhw.appmanager.model.InstalledApp
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phone.cleaner.domain.repository.AppManagerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppCleanerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val apps: List<InstalledApp> = emptyList(),
    /** Usage access is off, so sizes are the APK only: show the prompt that asks for it. */
    val showUsageAccessPrompt: Boolean = false,
)

sealed interface AppCleanerUiEvent {
    /** The design's dialog for one app: Uninstall or Clear cache. */
    data class ShowAppDialog(val app: InstalledApp) : AppCleanerUiEvent

    /** A system screen: the uninstall dialog, the App info page or the Usage access settings. */
    data class OpenSystemScreen(val intent: Intent) : AppCleanerUiEvent
}

/**
 * App Cleaner. Uninstalling and clearing a cache happen in system screens, so the list re-checks
 * when the screen comes back ([onScreenResumed]): an uninstalled app's row goes, and newly granted
 * Usage access reloads the exact sizes.
 */
class AppCleanerViewModel(
    private val repository: AppManagerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppCleanerUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<AppCleanerUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var job: Job? = null

    /** The app whose uninstall dialog was opened; checked when the screen resumes. */
    private var pendingUninstall: String? = null

    /** Whether the list on screen was read with Usage access. */
    private var loadedWithUsageAccess = false

    init {
        load()
    }

    fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val usageAccess = repository.hasUsageAccess()
            val result = repository.getInstalledApps()
            loadedWithUsageAccess = usageAccess
            _state.update {
                it.copy(
                    isLoading = false,
                    error = result.message,
                    apps = result.data ?: it.apps,
                    showUsageAccessPrompt = !usageAccess,
                )
            }
        }
    }

    fun onAppClicked(app: InstalledApp) = emit(AppCleanerUiEvent.ShowAppDialog(app))

    fun onUninstallClicked(app: InstalledApp) {
        pendingUninstall = app.packageName
        emit(AppCleanerUiEvent.OpenSystemScreen(repository.uninstallIntent(app.packageName)))
    }

    /** Android lets no app clear another app's cache; its App info page has the button. */
    fun onClearCacheClicked(app: InstalledApp) =
        emit(AppCleanerUiEvent.OpenSystemScreen(repository.appDetailsIntent(app.packageName)))

    fun onUsageAccessPromptClicked() =
        emit(AppCleanerUiEvent.OpenSystemScreen(repository.usageAccessSettingsIntent()))

    /** Back from a system screen: drop an app that is gone; reload if Usage access changed. */
    fun onScreenResumed() {
        pendingUninstall?.let { packageName ->
            pendingUninstall = null
            if (!repository.isInstalled(packageName)) {
                Log.d(TAG, "$packageName was uninstalled")
                _state.update { state -> state.copy(apps = state.apps.filterNot { it.packageName == packageName }) }
            }
        }
        if (!_state.value.isLoading && repository.hasUsageAccess() != loadedWithUsageAccess) load()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun emit(event: AppCleanerUiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    private companion object {
        const val TAG = "AppCleanerViewModel"
    }
}
