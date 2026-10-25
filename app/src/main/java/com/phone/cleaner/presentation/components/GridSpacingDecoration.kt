package com.phone.cleaner.presentation.components

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Even gaps between the tiles of a grid: [spacingPx] between columns and [rowSpacingPx] between
 * rows, nothing at the outer edges, so every column is the same width.
 */
class GridSpacingDecoration(
    private val spanCount: Int,
    private val spacingPx: Int,
    private val rowSpacingPx: Int,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val column = position % spanCount
        val start = column * spacingPx / spanCount
        val end = spacingPx - (column + 1) * spacingPx / spanCount
        // Columns run from the start edge, so the gaps follow the layout direction.
        if (parent.layoutDirection == View.LAYOUT_DIRECTION_RTL) outRect.set(end, 0, start, 0) else outRect.set(start, 0, end, 0)
        if (position >= spanCount) outRect.top = rowSpacingPx
    }
}
