package com.phone.cleaner.utils

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps every scanning page on screen for at least [minDurationMs], so a scan that finishes at
 * once (the AI (Artificial Intelligence) cleaner's cached results, a small WhatsApp folder) still
 * shows its ring filling instead of jumping straight to the results.
 *
 * The ring shows the smaller of the real progress and the time gone by against [minDurationMs]:
 * a fast scan fills it evenly over that time, a slow one moves at its own pace as before.
 *
 * Started from inside the scan's own job, so stopping the scan cancels the ticker with it. Used
 * on the main thread only, like the ViewModels that own it.
 */
class ScanPacer(
    private val minDurationMs: Long = MIN_SCAN_MS,
    /** Writes the progress to show into the screen's state. */
    private val publish: (Float) -> Unit,
) {

    private var startedAt = 0L
    private var progress = 0f
    private var ticker: Job? = null

    private val elapsed: Long get() = SystemClock.elapsedRealtime() - startedAt

    /** A new scan: the clock starts and the ring is redrawn every [TICK_MS] while it runs. */
    fun start(scope: CoroutineScope) {
        stop()
        startedAt = SystemClock.elapsedRealtime()
        progress = 0f
        ticker = scope.launch {
            while (isActive) {
                publish(shown())
                delay(TICK_MS)
            }
        }
    }

    /** The scan's real progress, 0..1. */
    fun report(fraction: Float) {
        progress = fraction.coerceIn(0f, 1f)
        publish(shown())
    }

    /** The scan is done: waits out what is left of [minDurationMs] while the ring fills, then stops. */
    suspend fun finish() {
        progress = 1f
        val remaining = minDurationMs - elapsed
        if (remaining > 0) delay(remaining)
        stop()
    }

    fun stop() {
        ticker?.cancel()
        ticker = null
    }

    private fun shown(): Float = minOf(progress, elapsed.toFloat() / minDurationMs)

    companion object {
        /** QA: every scanner shows its scanning page for at least this long. */
        const val MIN_SCAN_MS = 3_000L

        private const val TICK_MS = 50L
    }
}
