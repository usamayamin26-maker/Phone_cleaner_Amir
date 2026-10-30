package com.phone.cleaner.presentation.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.phone.cleaner.ads.AdsController
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

    /**
     * True on a screen whose top is dark (the Home header), so the status bar icons are drawn
     * light. The app is light only, so this never follows the phone's dark mode.
     */
    protected open val darkStatusBarContent: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // The bar styles are fixed: the default follows the phone's dark mode, which would draw
        // white status icons over this light-only app's light screens.
        enableEdgeToEdge(
            statusBarStyle = if (darkStatusBarContent) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            },
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
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

    /**
     * Opens a screen behind its entry interstitial.
     *
     * [flag] is the destination's own `is_show_*_btn_inter` and [counter] the group of buttons
     * it belongs to. [open] always runs - straight away when the flag is off, the counter says no,
     * the unit id is blank or the ad fails - so a tap can never be swallowed.
     */
    protected fun withEntryAd(
        flag: String,
        counter: String = AdsController.Counter.SCREEN_BUTTONS,
        open: () -> Unit,
    ) = AdsController.interstitial(this, flag, counter) {
        // The ad outlives a quick finish: only navigate while this activity is still here.
        if (!isFinishing && !isDestroyed) open()
    }

    /**
     * Leaves an inner screen behind its back interstitial. [flag] is the screen's own
     * `is_show_*_back_inter`; the whole group shares `counter_back_btn`. [action] always runs.
     */
    protected fun leaveWithAd(flag: String, action: () -> Unit = { finish() }) =
        AdsController.back(this, flag, action)

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
