package com.phone.cleaner.domain.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplashUiState(
    /** False while the splash holds, true once the design's loading bar has filled. */
    val isHoldDone: Boolean = false,
)

/**
 * Holds the splash for [HOLD_MS], the time the design's loading bar takes to fill.
 *
 * The hold is state rather than a one-shot event, so a splash that was in the background when it
 * ended still moves on when it comes back. **Where the launch then goes is not decided here** - it
 * is the `is_show_language_screen` / `is_show_onboarding_screen` pair in Remote Config, read by
 * [com.phone.cleaner.presentation.onboarding.SplashActivity] once the ads config is in hand.
 */
class SplashViewModel : ViewModel() {

    private val _state = MutableStateFlow(SplashUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            delay(HOLD_MS)
            _state.update { it.copy(isHoldDone = true) }
        }
    }

    companion object {
        /** About two seconds, as asked, and what the loading bar's animation is timed against. */
        const val HOLD_MS = 2_000L
    }
}
