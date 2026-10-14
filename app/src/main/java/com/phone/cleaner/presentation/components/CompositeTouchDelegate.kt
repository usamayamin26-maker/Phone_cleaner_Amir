package com.phone.cleaner.presentation.components

import android.graphics.Rect
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View

/**
 * A [TouchDelegate] that holds several delegates at once.
 *
 * A view carries exactly one `touchDelegate`, so two small controls under the same parent would
 * overwrite each other's enlarged hit area and only the last one would grow. This keeps one
 * delegate per target view and offers a touch to each of them in turn.
 *
 * [MotionEvent.ACTION_DOWN] decides which delegate owns the gesture. Asking a delegate moves the
 * event's coordinates into the target view's space, so the down event is offered as a copy and the
 * original is left untouched for the delegates that are asked after it; every later event of the
 * same gesture goes straight to the delegate that claimed it.
 */
class CompositeTouchDelegate(bounds: Rect, host: View) : TouchDelegate(bounds, host) {

    /** One entry per target view, so re-binding a recycled row replaces rather than piles up. */
    private val delegates = linkedMapOf<View, TouchDelegate>()

    private var handlingDelegate: TouchDelegate? = null

    fun put(target: View, delegate: TouchDelegate) {
        delegates[target] = delegate
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) {
            return handlingDelegate?.onTouchEvent(event) == true
        }

        handlingDelegate = null
        val copy = MotionEvent.obtain(event)
        try {
            for ((target, delegate) in delegates) {
                // A hidden control has no laid-out bounds, and an enlarged empty rect would
                // swallow taps meant for the row.
                if (target.visibility != View.VISIBLE) continue
                copy.setLocation(event.x, event.y)
                if (delegate.onTouchEvent(copy)) {
                    handlingDelegate = delegate
                    return true
                }
            }
        } finally {
            copy.recycle()
        }
        return false
    }
}
