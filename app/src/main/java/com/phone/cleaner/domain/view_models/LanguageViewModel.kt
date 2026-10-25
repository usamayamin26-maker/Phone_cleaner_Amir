package com.phone.cleaner.domain.view_models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phone.cleaner.domain.entities.AppLanguage
import com.phone.cleaner.utils.SharedPref
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LanguageUiState(
    val languages: List<AppLanguage> = AppLanguage.ALL,
    /** The row with the tick: the stored language until the user taps another one. */
    val selectedTag: String = "en",
)

sealed interface LanguageUiEvent {
    /** The choice is stored; the activity applies it, which recreates the screen. */
    data class LanguageStored(val tag: String) : LanguageUiEvent
}

class LanguageViewModel(private val sharedPref: SharedPref) : ViewModel() {

    private val _state = MutableStateFlow(LanguageUiState(selectedTag = sharedPref.appLanguage))
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<LanguageUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var job: Job? = null

    fun onLanguageSelected(tag: String) {
        _state.update { it.copy(selectedTag = tag) }
    }

    /** Stores the chosen language; it takes effect when the activity applies it. */
    fun onContinueClicked() {
        job?.cancel()
        job = viewModelScope.launch {
            val tag = _state.value.selectedTag
            sharedPref.appLanguage = tag
            Log.d(TAG, "Language stored: $tag")
            _events.emit(LanguageUiEvent.LanguageStored(tag))
        }
    }

    private companion object {
        const val TAG = "LanguageViewModel"
    }
}
