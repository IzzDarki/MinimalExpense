package com.izzdarki.minimalexpense.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

class RecyclerArrayAdapter(
    @param:LayoutRes private val layoutRes: Int,
    var strings: MutableList<String>
)
    : RecyclerView.Adapter<RecyclerArrayAdapter.ViewHolder>()
{

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(layoutRes, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        holder.textView.text = strings[pos]
    }

    override fun getItemCount() = strings.size

}