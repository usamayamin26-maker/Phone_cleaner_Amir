package com.phone.cleaner.domain.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phone.cleaner.domain.entities.ApiResult
import com.phone.cleaner.domain.entities.DeviceInfoContent
import com.phone.cleaner.domain.entities.DeviceInfoSection
import com.phone.cleaner.domain.repository.DeviceInfoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceInfoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val section: DeviceInfoSection? = null,
    val content: DeviceInfoContent? = null,
)

/** Behind the Device Info detail screen; one instance shows one [DeviceInfoSection]. */
class DeviceInfoViewModel(
    private val repository: DeviceInfoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceInfoUiState())
    val state = _state.asStateFlow()

    private var job: Job? = null

    /** Loads [section] unless it is already on screen, so a rotation does not read it again. */
    fun load(section: DeviceInfoSection) {
        if (state.value.section == section && (state.value.content != null || state.value.isLoading)) return
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = DeviceInfoUiState(isLoading = true, section = section)
            val result = read(section)
            _state.update {
                it.copy(isLoading = false, error = result.message, content = result.data)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun read(section: DeviceInfoSection): ApiResult<out DeviceInfoContent> =
        when (section) {
            DeviceInfoSection.DEVICE -> {
                val device = repository.getDevice()
                val display = repository.getDisplay()
                val deviceData = device.data
                val displayData = display.data
                if (deviceData != null && displayData != null) {
                    ApiResult.Success(DeviceInfoContent.Device(deviceData, displayData))
                } else {
                    ApiResult.Failure(device.message ?: display.message ?: "Something went wrong")
                }
            }
            DeviceInfoSection.PROCESSOR -> repository.getProcessor().map(DeviceInfoContent::Processor)
            DeviceInfoSection.WIFI -> repository.getWifi().map(DeviceInfoContent::Wifi)
            DeviceInfoSection.DISPLAY -> repository.getDisplay().map(DeviceInfoContent::Display)
            DeviceInfoSection.RAM -> repository.getRam().map(DeviceInfoContent::Ram)
            DeviceInfoSection.ROM -> repository.getStorage().map(DeviceInfoContent::Rom)
            DeviceInfoSection.CAMERA -> repository.getCameras().map(DeviceInfoContent::Cameras)
            // Battery Information opens Battery Info instead of this screen.
            DeviceInfoSection.BATTERY -> ApiResult.Failure("Battery has its own screen")
        }

    private inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> {
        val value = data
        return if (this is ApiResult.Success && value != null) {
            ApiResult.Success(transform(value))
        } else {
            ApiResult.Failure(message ?: "Something went wrong")
        }
    }
}
