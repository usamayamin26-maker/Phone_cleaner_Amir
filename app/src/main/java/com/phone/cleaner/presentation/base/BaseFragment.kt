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
import com.phone.cleaner.ads.AdsController
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

    /**
     * Opens a screen or an inner page behind its entry interstitial, the fragment counterpart of
     * `BaseActivity.withEntryAd`. [open] always runs, so a tap is never swallowed.
     *
     * It lives here because the interstitial needs an Activity and a fragment only has a host.
     * Without one [open] simply runs: navigation is never the thing that gets dropped.
     */
    protected fun withEntryAd(
        flag: String,
        counter: String = AdsController.Counter.SCREEN_BUTTONS,
        open: () -> Unit,
    ) {
        val host = activity
        if (host == null || host.isFinishing) {
            open()
            return
        }
        AdsController.interstitial(host, flag, counter) {
            // The ad outlives a quick back press: only navigate if this fragment is still attached.
            if (isAdded && view != null) open()
        }
    }

    /** Leaves an inner page behind its back interstitial; [action] always runs. */
    protected fun leaveWithAd(flag: String, action: () -> Unit) =
        AdsController.back(this, flag, action)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
