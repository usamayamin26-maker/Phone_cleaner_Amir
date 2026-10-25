package com.phone.cleaner.domain.view_models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phone.cleaner.utils.SharedPref
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the launch goes once the splash has been shown. */
enum class SplashDestination { LANGUAGE, HOME }

data class SplashUiState(
    /** Null while the splash holds; then the screen to open. */
    val destination: SplashDestination? = null,
)

/**
 * Holds the splash for [HOLD_MS], then picks the next screen. The destination is state rather than
 * a one-shot event, so a splash that was in the background when the hold ended still moves on
 * when it comes back.
 *
 * Until Home has been opened once ([SharedPref.hasOpenedMainScreen]) the launch is a first run and
 * starts with the language; the Language screen then goes on to onboarding unless it is done.
 */
class SplashViewModel(private val sharedPref: SharedPref) : ViewModel() {

    private val _state = MutableStateFlow(SplashUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            delay(HOLD_MS)
            val destination = if (sharedPref.hasOpenedMainScreen) SplashDestination.HOME else SplashDestination.LANGUAGE
            Log.d(TAG, "Routing to $destination")
            _state.update { it.copy(destination = destination) }
        }
    }

    companion object {
        /** About two seconds, as asked; there are no ads to wait for yet. */
        const val HOLD_MS = 2_000L

        private const val TAG = "SplashViewModel"
    }
}
