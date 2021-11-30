package com.izzdarki.minimalexpense.ui.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.izzdarki.minimalexpense.data.Expense
import com.izzdarki.minimalexpense.data.ExpensePreferenceManager
import com.izzdarki.minimalexpense.data.ExpensePreferenceManager.LabelFilter
import com.izzdarki.minimalexpense.data.ExpensePreferenceManager.DateFilter
import com.izzdarki.minimalexpense.util.containsAny
import com.izzdarki.minimalexpense.util.notBefore
import java.util.*

class HomeViewModel : ViewModel() {

    fun init(context: Context) {
        val preferences = ExpensePreferenceManager(context)

        _expenses.value?.clear()
        _expenses.value?.addAll(preferences.readAllExpenses())
        _sortingType.value = preferences.readSortingType()
        _sortingReversed.value = preferences.readSortingReversed()
        _dateFilter.value = preferences.readDateFilter()
        _labelFilter.value = preferences.readLabelFilter()
        _isFilterCardOpened.value = preferences.readFilterCardOpened()

        sortExpenses(context)
        filterExpenses(context)
    }

    fun sortExpenses(context: Context, sortingType: SortingType? = null, reversed: Boolean? = null) {
        val preferences = ExpensePreferenceManager(context)
        if (sortingType != null) {
            _sortingType.value = sortingType!!
            preferences.writeSortingType(sortingType)
        }
        if (reversed != null) {
            _sortingReversed.value = reversed!!
            preferences.writeSortingReversed(reversed)
        }

        when (_sortingType.value) {
            SortingType.ByCreationDate -> _expenses.value!!.sortByDescending { it.created }
            SortingType.ByAmount -> _expenses.value!!.sortByDescending { it.cents }
            SortingType.ByName -> _expenses.value!!.sortBy { it.name }
        }

        if (_sortingReversed.value == true)
            _expenses.value!!.reverse()
    }

    private fun filterExpenses(context: Context, dateFilterChanged: Boolean = false, labelFilterChanged: Boolean = false) {
        val preferences = ExpensePreferenceManager(context)
        val dateFilter = _dateFilter.value!!
        val labelFilter = _labelFilter.value!!
        val expenses = _expenses.value!!

        if (dateFilterChanged)  {
            preferences.writeDateFilter(dateFilter)
        }
        if (labelFilterChanged) {
            preferences.writeLabelFilter(labelFilter)
        }

        // Reload expenses (for new filter)
        expenses.clear()
        expenses.addAll(preferences.readAllExpenses())
        sortExpenses(context)

        // Filter expenses
        if (dateFilter.enabled && dateFilter.until notBefore dateFilter.from) {
            expenses.retainAll{
                it.created.after(dateFilter.from) && it.created.before(dateFilter.until)
            }
        }
        if (labelFilter.enabled && labelFilter.labels.isNotEmpty()) {
            when (labelFilter.exclusive) {
                false -> {
                    expenses.retainAll {
                        it.labels.containsAny(labelFilter.labels)
                    }
                }
                true -> {
                    expenses.removeAll {
                        !it.labels.containsAll(labelFilter.labels)
                    }
                }
            }
        }
        if ((dateFilter.enabled && dateFilter.until notBefore dateFilter.from)
            || (labelFilter.enabled && labelFilter.labels.isNotEmpty())
        )
            onExpensesFilteredOrSorted.invoke()
    }

    /**
     * Removes the entire expense from preferences and from [expenses]
     * @return Returns the position of the expense with the given [id] in [expenses]
     */
    fun deleteExpense(context: Context, id: UUID): Int {
        val pos = _expenses.value!!.indexOfFirst { it.id == id }
        ExpensePreferenceManager(context).removeExpense(id)
        _expenses.value!!.removeAt(pos)
        return pos
    }

    fun toggleLabelFilterEnabled(context: Context) {
        _labelFilter.value!!.enabled = !_labelFilter.value!!.enabled
        filterExpenses(context,
            labelFilterChanged = true
        )
    }

    fun setLabelFilterExclusive(context: Context, exclusive: Boolean) {
        _labelFilter.value!!.exclusive = exclusive
        filterExpenses(context,
            labelFilterChanged = true
        )
    }

    fun setLabelFilter(context: Context, labels: Set<String>) {
        _labelFilter.value!!.labels = labels.toMutableSet()
        filterExpenses(context,
            labelFilterChanged = true
        )
    }

    fun toggleDateFilterEnabled(context: Context) {
        _dateFilter.value!!.enabled = !_dateFilter.value!!.enabled
        filterExpenses(context,
            dateFilterChanged = true
        )
    }

    fun setDateFilterDates(context: Context, from: Date, to: Date) {
        _dateFilter.value!!.from = from
        _dateFilter.value!!.until = to
        filterExpenses(context,
            dateFilterChanged = true
        )
    }

    fun setFilterCardOpened(context: Context, isOpened: Boolean) {
        _isFilterCardOpened.value = isOpened
        ExpensePreferenceManager(context).writeFilterCardOpened(isOpened)
    }

    enum class SortingType {
        ByCreationDate,
        ByAmount,
        ByName
    }

    private val _expenses = MutableLiveData<MutableList<Expense>>().apply { value = mutableListOf() }
    private val _sortingType = MutableLiveData<SortingType>()
    private val _sortingReversed = MutableLiveData<Boolean>()
    private val _dateFilter = MutableLiveData<DateFilter>()
    private val _labelFilter = MutableLiveData<LabelFilter>()
    private var _isFilterCardOpened = MutableLiveData<Boolean>()

    val expenses: LiveData<MutableList<Expense>> = _expenses
    val sumCents: Long get() = expenses.value!!.sumOf { it.cents }
    val sortingType: LiveData<SortingType> = _sortingType
    val sortingReversed: LiveData<Boolean> = _sortingReversed
    val dateFilter: LiveData<DateFilter> = _dateFilter
    val labelFilter: LiveData<LabelFilter> = _labelFilter
    val isFilterCardOpened: LiveData<Boolean> = _isFilterCardOpened
    var onExpensesFilteredOrSorted: () -> Unit = { }
}