package com.izzdarki.minimalexpense.util

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView

interface MultiSelectAdapterViewHolder<T> {
    val itemDetails: ItemDetailsLookup.ItemDetails<T>
}

class MultiSelectItemDetailsLookup<T>(private val recyclerView: RecyclerView)
    : ItemDetailsLookup<T>()
{
    override fun getItemDetails(e: MotionEvent): ItemDetails<T>? {
        val view = recyclerView.findChildViewUnder(e.x, e.y)
        view?.let {
            return (recyclerView.getChildViewHolder(view) as MultiSelectAdapterViewHolder<T>).itemDetails
        }
        return null
    }
}

val SelectionTracker<*>.isNothingSelected get() = this.selection.size() == 0
val SelectionTracker<*>.isSingleSelected get() = this.selection.size() == 1
val SelectionTracker<*>.isMultipleSelected get() = this.selection.size() > 1
