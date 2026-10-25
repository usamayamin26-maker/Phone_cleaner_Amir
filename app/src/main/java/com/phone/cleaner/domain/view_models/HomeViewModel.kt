package com.phone.cleaner.domain.view_models

import ai.jhw.deviceinfo.model.RamDetails
import ai.jhw.deviceinfo.model.StorageDetails
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phone.cleaner.domain.repository.DeviceInfoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val storage: StorageDetails? = null,
    val ram: RamDetails? = null,
)

class HomeViewModel(
    private val repository: DeviceInfoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    private var job: Job? = null

    init {
        loadUsage()
    }

    /** Reads the Storage Usage and RAM Usage cards. RAM changes all the time, so Home re-reads on resume. */
    fun loadUsage() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val storage = async { repository.getStorage() }
            val ram = async { repository.getRam() }
            val storageResult = storage.await()
            val ramResult = ram.await()
            _state.update {
                it.copy(
                    isLoading = false,
                    error = storageResult.message ?: ramResult.message,
                    storage = storageResult.data ?: it.storage,
                    ram = ramResult.data ?: it.ram,
                )
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
