package com.phone.cleaner.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.phone.cleaner.utils.toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The fragment counterpart of [BaseActivity]: it inflates the ViewBinding, releases it in
 * [onDestroyView] and exposes the same two collectors, tied to the view lifecycle.
 *
 * A fragment outlives its view, so everything here goes through `viewLifecycleOwner` - a collector
 * started on the fragment's own lifecycle would keep writing into views that no longer exist.
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) { "Binding accessed outside the view lifecycle" }

    /** Pass the generated binding's `inflate` reference, e.g. `FragmentHomeBinding::inflate`. */
    abstract val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = bindingInflater(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeState()
    }

    /** Wire listeners, adapters and static text here. */
    protected open fun setupViews() = Unit

    /** Start [collectState] / [collectEvents] collectors here. */
    protected open fun observeState() = Unit

    /** Collects a [StateFlow] of UI (User Interface) state while the view is at least STARTED. */
    protected fun <T> StateFlow<T>.collectState(block: (T) -> Unit) {
        collectWhileStarted(block)
    }

    /** Collects one-shot [SharedFlow] effects - toasts, navigation, dialogs. */
    protected fun <T> SharedFlow<T>.collectEvents(block: (T) -> Unit) {
        collectWhileStarted(block)
    }

    private fun <T> Flow<T>.collectWhileStarted(block: (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { block(it) }
            }
        }
    }

    protected fun showToast(message: String) = requireContext().toast(message)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
