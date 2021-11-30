package com.izzdarki.minimalexpense.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.izzdarki.minimalexpense.R
import com.izzdarki.minimalexpense.data.Expense
import com.izzdarki.minimalexpense.databinding.AdapterExpenseBinding
import com.izzdarki.minimalexpense.util.*

class ExpenseAdapter(
    private val expenses: List<Expense>,
    private val onClickListener: (pos: Int) -> Unit
)
    : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>()
{

    lateinit var selectionTracker: SelectionTracker<String>

    inner class ViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView), MultiSelectAdapterViewHolder<String>
    {
        private val binding: AdapterExpenseBinding = AdapterExpenseBinding.bind(itemView)
        private val labelsRecyclerView = binding.labelsRecyclerView
        val nameTextView = binding.nameTextView
        val amountTextView = binding.amountTextView

        val root: View get() = itemView.findViewById(R.id.adapter_item_root)
        val context: Context get() = root.context
        val labelsAdapter: RecyclerArrayAdapter get() = labelsRecyclerView.adapter as RecyclerArrayAdapter

        init {
            root.setOnClickListener { onClickListener(adapterPosition) }
            labelsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            labelsRecyclerView.adapter = RecyclerArrayAdapter(
                R.layout.expense_labels_list_item,
                mutableListOf()
            )
        }

        override val itemDetails: ItemDetailsLookup.ItemDetails<String>
            get() = object : ItemDetailsLookup.ItemDetails<String>() {
                override fun getPosition(): Int = adapterPosition
                override fun getSelectionKey(): String = expenses[adapterPosition].id.toString()
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.adapter_expense, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        holder.nameTextView.text = expenses[pos].name
        holder.amountTextView.text = formatCurrency(expenses[pos].cents)

        holder.labelsAdapter.strings.clear()
        holder.labelsAdapter.strings.addAll(expenses[pos].labels)
        holder.labelsAdapter.notifyDataSetChanged()

        if (selectionTracker.isSelected(expenses[pos].id.toString())) {
            holder.root.setBackgroundColor(
                ContextCompat.getColor(holder.context, R.color.primary_color).withAlpha(0x40)
            )
        }
        else {
            holder.root.setBackgroundColor(
                getThemeBackgroundColor(holder.context)
            )
        }
    }

    override fun getItemCount() = expenses.size

    inner class ItemKeyProvider: androidx.recyclerview.selection.ItemKeyProvider<String>(SCOPE_CACHED) {
        override fun getKey(pos: Int) = expenses[pos].id.toString()
        override fun getPosition(key: String) = expenses.indexOfFirst { it.id.toString() == key }

    }
}