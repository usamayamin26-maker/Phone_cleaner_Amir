package com.phone.cleaner.domain.view_models

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.BatteryDetails
import com.phone.cleaner.domain.repository.BatteryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BatteryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val battery: BatteryDetails? = null,
)

sealed interface BatteryUiEvent {
    data class OpenBatterySaverSettings(val intent: Intent) : BatteryUiEvent
}

class BatteryViewModel(
    private val repository: BatteryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BatteryUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<BatteryUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var observeJob: Job? = null
    private var refreshJob: Job? = null

    /**
     * Live values while the screen is visible. The activity starts this in onStart and stops it in
     * onStop, so the battery receiver is not registered while the screen is in the back stack.
     */
    fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeBatteryInfo().collect(::apply)
        }
    }

    fun stopObserving() {
        observeJob?.cancel()
        observeJob = null
    }

    /**
     * One fresh reading. Called from onResume: the user may come back from the Battery Saver
     * settings, and the switch has to show what they chose there.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { apply(repository.getBatteryInfo()) }
    }

    /** The switch cannot change power saving itself, so a tap opens the system's settings. */
    fun onPowerSavingClicked() {
        _events.tryEmit(BatteryUiEvent.OpenBatterySaverSettings(repository.batterySaverSettingsIntent()))
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun apply(result: ApiResult<BatteryDetails>) {
        _state.update {
            it.copy(isLoading = false, error = result.message, battery = result.data ?: it.battery)
        }
    }
}
