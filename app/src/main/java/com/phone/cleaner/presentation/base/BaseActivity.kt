package com.phone.cleaner.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.phone.cleaner.utils.applySystemBarInsets
import com.phone.cleaner.utils.toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Every activity in the app extends this: it inflates the ViewBinding, goes edge-to-edge and
 * exposes the two collectors the ViewModel pattern needs.
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB get() = requireNotNull(_binding) { "Binding accessed after onDestroy" }

    /** Pass the generated binding's `inflate` reference, e.g. `ActivityMainBinding::inflate`. */
    abstract val bindingInflater: (LayoutInflater) -> VB

    /** Set false on screens that draw their own insets (a header that bleeds behind the bars). */
    protected open val applyDefaultInsets: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        _binding = bindingInflater(layoutInflater)
        setContentView(binding.root)
        if (applyDefaultInsets) binding.root.applySystemBarInsets()
        setupViews()
        observeState()
    }

    /** Wire listeners, adapters and static text here. */
    protected open fun setupViews() = Unit

    /** Start [collectState] / [collectEvents] collectors here. */
    protected open fun observeState() = Unit

    /** Collects a [StateFlow] of UI (User Interface) state while the activity is at least STARTED. */
    protected fun <T> StateFlow<T>.collectState(block: (T) -> Unit) {
        collectWhileStarted(block)
    }

    /** Collects one-shot [SharedFlow] effects - toasts, navigation, dialogs. */
    protected fun <T> SharedFlow<T>.collectEvents(block: (T) -> Unit) {
        collectWhileStarted(block)
    }

    private fun <T> Flow<T>.collectWhileStarted(block: (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { block(it) }
            }
        }
    }

    protected fun showToast(message: String) = toast(message)

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
