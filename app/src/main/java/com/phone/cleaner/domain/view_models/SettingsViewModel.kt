package com.phone.cleaner.domain.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** What a Settings row asks the screen to open; each one leaves the app or opens a screen. */
sealed interface SettingsUiEvent {
    data object OpenLanguage : SettingsUiEvent
    data object ShareApp : SettingsUiEvent
    data object RateApp : SettingsUiEvent
    data object OpenPrivacyPolicy : SettingsUiEvent
}

/**
 * Settings has no state of its own: four rows, each a one-shot action. The ViewModel turns the
 * taps into events, emitted only from a click, never from start-up.
 */
class SettingsViewModel : ViewModel() {

    private val _events = MutableSharedFlow<SettingsUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun onLanguageClicked() = emit(SettingsUiEvent.OpenLanguage)

    fun onShareClicked() = emit(SettingsUiEvent.ShareApp)

    fun onRateClicked() = emit(SettingsUiEvent.RateApp)

    fun onPrivacyPolicyClicked() = emit(SettingsUiEvent.OpenPrivacyPolicy)

    private fun emit(event: SettingsUiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }
}
