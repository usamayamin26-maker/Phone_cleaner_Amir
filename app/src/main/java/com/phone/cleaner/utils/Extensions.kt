package com.phone.cleaner.utils

import android.content.Context
import android.graphics.Rect
import android.text.format.Formatter
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.phone.cleaner.presentation.components.CompositeTouchDelegate

// ---- String ----

fun String?.orNotAvailable(): String = if (isNullOrBlank()) "-" else this

// ---- View ----

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/** Debounced click listener - stops a double tap from opening a screen twice. */
fun View.onClick(intervalMs: Long = 500L, action: (View) -> Unit) {
    var lastClick = 0L
    setOnClickListener {
        val now = System.currentTimeMillis()
        if (now - lastClick >= intervalMs) {
            lastClick = now
            action(it)
        }
    }
}

/** Pads a root view by the system bars so edge-to-edge content is not drawn under them. */
fun View.applySystemBarInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
}

/**
 * Moves the view below the status bar, keeping the top margin its layout already declares as the
 * gap the design draws underneath the bar. For screens whose header background bleeds behind the
 * status bar, which therefore cannot pad their root with [applySystemBarInsets].
 *
 * Call this once per inflated view, never from a rebind: the declared margin is read when the
 * listener is attached and would otherwise be re-read from an already shifted view.
 */
fun View.applyStatusBarTopMargin() {
    val designGap = (layoutParams as ViewGroup.MarginLayoutParams).topMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = designGap + statusBar }
        insets
    }
    doOnAttach { ViewCompat.requestApplyInsets(it) }
}

/** The bottom counterpart of [applyStatusBarTopMargin], for a call to action above the gesture bar. */
fun View.applyNavigationBarBottomMargin() {
    val designGap = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val navigationBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = designGap + navigationBar
            // A ConstraintLayout child whose bottom points at a GONE view uses goneBottomMargin
            // instead of bottomMargin, so the clearance has to be set on both.
            (this as? ConstraintLayout.LayoutParams)?.goneBottomMargin = designGap + navigationBar
        }
        insets
    }
    doOnAttach { ViewCompat.requestApplyInsets(it) }
}

/**
 * Grows a control's touch area to the platform's 48dp minimum without moving it, for controls the
 * design draws smaller than that. Several small controls under one parent each keep their own
 * enlarged area through [CompositeTouchDelegate].
 */
fun View.expandTouchTarget(minimumDp: Int = 48) {
    val host = parent as? View ?: return
    if (visibility != View.VISIBLE) return
    host.doOnLayout {
        val minimum = (minimumDp * resources.displayMetrics.density).toInt()
        val hit = Rect().also(::getHitRect)
        val growX = ((minimum - hit.width()).coerceAtLeast(0)) / 2
        val growY = ((minimum - hit.height()).coerceAtLeast(0)) / 2
        if (growX == 0 && growY == 0) return@doOnLayout
        hit.inset(-growX, -growY)

        val composite = host.touchDelegate as? CompositeTouchDelegate
            ?: CompositeTouchDelegate(hit, host).also { host.touchDelegate = it }
        composite.put(this, TouchDelegate(hit, this))
    }
}

// ---- Long ----

/** A byte count as the platform writes it, in the app's language: "357 MB", "3.75 GB". */
fun Long.toReadableSize(context: Context): String = Formatter.formatFileSize(context, this)

// ---- Context ----

fun Context.toast(message: String, long: Boolean = false) {
    Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}
