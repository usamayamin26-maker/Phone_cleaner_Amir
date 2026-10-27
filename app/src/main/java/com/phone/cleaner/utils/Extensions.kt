package com.phone.cleaner.utils

import ai.jhw.storage.model.FileItem
import ai.jhw.storage.model.FileKind
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.format.Formatter
import android.util.TypedValue
import android.view.Gravity
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import coil3.dispose
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import com.phone.cleaner.R
import com.phone.cleaner.databinding.ViewSwitchBinding
import com.phone.cleaner.presentation.components.CompositeTouchDelegate
import java.text.BreakIterator
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

// ---- String ----

/** The value, or the design's "-" from strings.xml when the device did not report one. */
fun String?.orNotAvailable(context: Context): String =
    if (isNullOrBlank()) context.getString(R.string.value_not_available) else this

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
    // Apply the window's current insets right away when they are known (a fragment swapped into an
    // activity that is already on screen). Waiting for the listener drew the first frame with the
    // header under the status bar and then moved it down: the header "flicker" on scan complete.
    knownWindowInsets()?.let { insets ->
        val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = designGap + statusBar }
    }
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
    knownWindowInsets()?.let { insets ->
        val navigationBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = designGap + navigationBar
            (this as? ConstraintLayout.LayoutParams)?.goneBottomMargin = designGap + navigationBar
        }
    }
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

/**
 * Sets the design's switch (layout/view_switch.xml): the thumb turns blue and moves to the end
 * when on, and back to the start when off.
 */
fun ViewSwitchBinding.setOn(on: Boolean) {
    switchThumb.isActivated = on
    switchThumb.updateLayoutParams<FrameLayout.LayoutParams> {
        gravity = (if (on) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
    }
}

/** Clips the view to a rounded rectangle - an app icon or a thumbnail with the design's radius. */
fun View.clipToRoundRect(radiusPx: Float) {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
        }
    }
    clipToOutline = true
}

/**
 * The largest text size, no larger than the current one, at which the text fits [width] without
 * breaking inside a word and, laid out at that width, is no taller than [maxHeight]. A word is a
 * run between two line-break opportunities, so a Japanese or Chinese label, which may break
 * between characters, is not shrunk for having no spaces. Used where a label sits in a box the
 * design fixes, so a large font size or a long word cannot break a word in two.
 */
fun TextView.fittingTextSize(width: Int, maxHeight: Int = Int.MAX_VALUE): Float {
    if (width <= 0 || text.isEmpty()) return textSize
    val probe = TextPaint(paint)
    val lineHeightPerSize = lineHeight / textSize
    val smallest = textSize / 4
    var size = textSize
    while (size > smallest) {
        probe.textSize = size
        val lines = StaticLayout.Builder.obtain(text, 0, text.length, probe, width)
            .setBreakStrategy(breakStrategy)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()
            .lineCount
        val fits = probe.widestWord(text) <= width && lines * size * lineHeightPerSize <= maxHeight
        if (fits) break
        size *= 0.95f
    }
    return size
}

/** Sets a size from [fittingTextSize], shrinking the line height in step. */
fun TextView.applyFittingTextSize(sizePx: Float) {
    if (sizePx >= textSize) return
    val lineHeightPx = lineHeight * sizePx / textSize
    setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
    TextViewCompat.setLineHeight(this, lineHeightPx.roundToInt())
}

private fun TextPaint.widestWord(text: CharSequence): Float {
    val string = text.toString()
    val breaks = BreakIterator.getLineInstance(textLocale).apply { setText(string) }
    var widest = 0f
    var start = breaks.first()
    var end = breaks.next()
    while (end != BreakIterator.DONE) {
        widest = maxOf(widest, measureText(string.substring(start, end).trimEnd()))
        start = end
        end = breaks.next()
    }
    return widest
}

/**
 * A file's preview: a Coil thumbnail for photos and video frames, the PDF icon for PDF documents
 * and the file icon for anything else. Recycled views are reset, so a tile never keeps the wrong image.
 * [inset] is the room around an icon inside a tile; a list row's icon fills its view.
 */
fun ImageView.loadFilePreview(
    file: FileItem,
    inset: Int = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp),
) {
    when (file.kind) {
        FileKind.IMAGE, FileKind.VIDEO -> {
            setPadding(0, 0, 0, 0)
            scaleType = ImageView.ScaleType.CENTER_CROP
            imageTintList = null
            load(file.uri) {
                placeholder(ContextCompat.getColor(context, R.color.thumbnail_placeholder).toDrawable())
                error(ContextCompat.getColor(context, R.color.thumbnail_placeholder).toDrawable())
            }
        }
        FileKind.DOCUMENT if file.name.endsWith(".pdf", ignoreCase = true) -> {
            dispose()
            setPadding(inset, inset, inset, inset)
            scaleType = ImageView.ScaleType.FIT_CENTER
            imageTintList = null
            setImageResource(R.drawable.ic_pdf)
        }
        else -> {
            dispose()
            setPadding(inset, inset, inset, inset)
            scaleType = ImageView.ScaleType.FIT_CENTER
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.icon_accent))
            setImageResource(R.drawable.ic_file)
        }
    }
}

// ---- Long ----

/** A byte count as the platform writes it, in the app's language: "357 MB", "3.75 GB". */
fun Long.toReadableSize(context: Context): String = Formatter.formatFileSize(context, this)

/**
 * Joins a value and its unit with no-break spaces ("256 GB", "26,57 %"), so a line that wraps
 * breaks between words of the sentence and never between a number and its unit.
 */
fun String.keepTogether(): String = replace(' ', '\u00A0')

// ---- Context ----

/** The language the app is shown in, which is what numbers are written in. */
val Context.appLocale: Locale
    get() = ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.getDefault()

/**
 * A number in the app's language and digits: "4.07", "٤٫٠٧". No thousands separator: the design
 * writes "1080 x 2210", "4265 mV" and "5000 mah".
 */
fun Context.formatNumber(value: Number, maxFractionDigits: Int = 0, minFractionDigits: Int = 0): String =
    NumberFormat.getInstance(appLocale).apply {
        isGroupingUsed = false
        maximumFractionDigits = maxFractionDigits
        minimumFractionDigits = minFractionDigits
    }.format(value)

/** A 0..100 value as a percentage in the app's language: 56.31 -> "56.31%". */
fun Context.formatPercent(value: Float, maxFractionDigits: Int = 2): String =
    NumberFormat.getPercentInstance(appLocale).apply {
        maximumFractionDigits = maxFractionDigits
    }.format(value / 100.0)

fun Context.toast(message: String, long: Boolean = false) {
    Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

/**
 * The window insets as they are now, before this view is attached: from the view itself if it is
 * attached, otherwise from its activity's window. Null on an activity's first layout, where the
 * listener's first dispatch arrives before anything is drawn anyway.
 */
private fun View.knownWindowInsets(): WindowInsetsCompat? {
    ViewCompat.getRootWindowInsets(this)?.let { return it }
    var c: android.content.Context? = context
    while (c is android.content.ContextWrapper) {
        if (c is android.app.Activity) {
            return ViewCompat.getRootWindowInsets(c.window.decorView)
        }
        c = c.baseContext
    }
    return null
}
