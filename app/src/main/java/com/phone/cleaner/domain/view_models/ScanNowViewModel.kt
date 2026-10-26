package com.phone.cleaner.domain.view_models

import ai.jhw.deviceinfo.model.SecurityStatus
import ai.jhw.junkcleaner.model.JunkScanEvent
import ai.jhw.junkcleaner.model.JunkScanResult
import ai.jhw.junkcleaner.model.UnavailableReason
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phone.cleaner.domain.repository.DeviceInfoRepository
import com.phone.cleaner.domain.repository.JunkRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the Scan Now check found about junk. */
sealed interface JunkStatus {
    /** Nothing the Smart Scan recommends deleting: "Device is fully cleaned". */
    data object Clean : JunkStatus

    /** [bytes] of recommended junk, which the AI Junk Cleaner's Junk Files section lists. */
    data class Found(val bytes: Long) : JunkStatus

    /** All files access is not granted, so junk could not be looked for. */
    data object NeedsAccess : JunkStatus
}

data class ScanNowUiState(
    val isScanning: Boolean = false,
    val isComplete: Boolean = false,
    /** 0..1 while scanning. */
    val progress: Float = 0f,
    /** Null until its check has finished; the scanning page shows a row per finished check. */
    val junk: JunkStatus? = null,
    val security: SecurityStatus? = null,
)

sealed interface ScanNowUiEvent {
    /** The AI Junk Cleaner, opened on its Junk Files section. */
    data object OpenJunkFiles : ScanNowUiEvent

    /** Ask for All files access, then look for junk again. */
    data object RequestAllFilesAccess : ScanNowUiEvent

    /** A system settings screen where the user can switch something off; the app never does. */
    data class OpenSettings(val intent: Intent) : ScanNowUiEvent
}

/**
 * Scan Now (Figma 1:1648 / 1:1705): a read-only health check. Junk comes from the Smart Scan,
 * Developer options, USB debugging and VPN from the device's settings. Nothing here deletes or
 * changes anything; each finding only leads to the screen where the user can act.
 */
class ScanNowViewModel(
    private val junkRepository: JunkRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanNowUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ScanNowUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var scanJob: Job? = null
    private var junkJob: Job? = null

    /** Whether a scan was ever started, so a rotation does not start another. */
    var hasStarted: Boolean = false
        private set

    /**
     * Debug builds only: a pause after each step of the junk walk, so the scanning page can be seen
     * and checked. Release builds never set it.
     */
    var debugStepDelayMs: Long = 0L

    /** The user went to the junk screen; what it deleted is looked for again on the way back. */
    private var isJunkStale = false

    fun startScan() {
        hasStarted = true
        scanJob?.cancel()
        junkJob?.cancel()
        scanJob = viewModelScope.launch {
            _state.value = ScanNowUiState(isScanning = true)
            try {
                // The settings are read at once; the junk walk is what takes time.
                val security = deviceInfoRepository.getSecurityStatus().data
                _state.update { it.copy(security = security) }
                val junk = scanJunk { fraction -> _state.update { it.copy(progress = fraction) } }
                _state.update { it.copy(isScanning = false, isComplete = true, progress = 1f, junk = junk) }
            } catch (e: CancellationException) {
                Log.d(TAG, "Scan stopped; nothing was changed")
                throw e
            }
        }
    }

    /** "Stop Scanning": the scan is cancelled; it never changes anything anyway. */
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    /** Back on the screen: the settings may have been changed; junk is re-checked if the user went to it. */
    fun onResume() {
        if (!_state.value.isComplete) return
        viewModelScope.launch {
            deviceInfoRepository.getSecurityStatus().data?.let { security ->
                _state.update { it.copy(security = security) }
            }
        }
        if (isJunkStale) {
            isJunkStale = false
            rescanJunk()
        }
    }

    /** All files access was granted from the junk card. */
    fun onAllFilesAccessGranted() = rescanJunk()

    fun onJunkClicked() {
        when (_state.value.junk) {
            is JunkStatus.Found -> {
                isJunkStale = true
                emit(ScanNowUiEvent.OpenJunkFiles)
            }
            JunkStatus.NeedsAccess -> emit(ScanNowUiEvent.RequestAllFilesAccess)
            JunkStatus.Clean, null -> Unit
        }
    }

    /** Both developer settings are switched off on the same system page. */
    fun onSecurityClicked() {
        val security = _state.value.security ?: return
        if (security.developerOptionsOn || security.usbDebuggingOn) {
            emit(ScanNowUiEvent.OpenSettings(deviceInfoRepository.developerSettingsIntent()))
        }
    }

    fun onVpnClicked() {
        if (_state.value.security?.vpnConnected == true) {
            emit(ScanNowUiEvent.OpenSettings(deviceInfoRepository.vpnSettingsIntent()))
        }
    }

    /** The junk card alone, quietly, without going back to the scanning page. */
    private fun rescanJunk() {
        junkJob?.cancel()
        junkJob = viewModelScope.launch {
            val junk = scanJunk {}
            _state.update { it.copy(junk = junk) }
        }
    }

    private suspend fun scanJunk(onProgress: (Float) -> Unit): JunkStatus {
        var result: JunkScanResult? = null
        junkRepository.scan().collect { event ->
            if (debugStepDelayMs > 0) delay(debugStepDelayMs)
            when (event) {
                is JunkScanEvent.Progress -> onProgress(event.fraction)
                is JunkScanEvent.Completed -> result = event.result
            }
        }
        val categories = result?.categories.orEmpty()
        if (categories.isNotEmpty() && categories.all { it.unavailableReason == UnavailableReason.NEEDS_FULL_ACCESS }) {
            return JunkStatus.NeedsAccess
        }
        val bytes = categories.filter { it.isAvailable }
            .flatMap { it.items }
            .filter { it.isRecommended }
            .sumOf { it.sizeBytes }
        val hasRecommended = categories.any { category -> category.isAvailable && category.items.any { it.isRecommended } }
        return if (hasRecommended) JunkStatus.Found(bytes) else JunkStatus.Clean
    }

    private fun emit(event: ScanNowUiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    private companion object {
        const val TAG = "ScanNowViewModel"
    }
}
