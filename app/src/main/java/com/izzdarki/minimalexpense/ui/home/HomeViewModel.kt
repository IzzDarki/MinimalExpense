package com.izzdarki.minimalexpense.ui.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.izzdarki.minimalexpense.data.*
import com.izzdarki.minimalexpense.debug.Timer
import com.izzdarki.minimalexpense.util.containsAny
import com.izzdarki.minimalexpense.util.notBefore
import java.util.*

class HomeViewModel : ViewModel() {

    private val _expenses = MutableLiveData<MutableList<Expense>>().apply { value = mutableListOf() }
    private val _sortingType = MutableLiveData<SortingType>()
    private val _sortingReversed = MutableLiveData<Boolean>()
    private val _labelFilter = MutableLiveData<LabelFilter>()
    private val _amountFilter = MutableLiveData<AmountFilter>()
    private val _dateFilter = MutableLiveData<DateFilter>()
    private var _isFilterCardOpened = MutableLiveData<Boolean>()
    private var _searchTerm: MutableLiveData<String> = MutableLiveData("")

    val expenses: LiveData<MutableList<Expense>> = _expenses
    val sumCents: Long get() = expenses.value!!.sumOf { it.cents }
    val sortingType: LiveData<SortingType> = _sortingType
    val sortingReversed: LiveData<Boolean> = _sortingReversed
    val labelFilter: LiveData<LabelFilter> = _labelFilter
    val amountFilter: LiveData<AmountFilter> = _amountFilter
    val dateFilter: LiveData<DateFilter> = _dateFilter
    val isFilterCardOpened: LiveData<Boolean> = _isFilterCardOpened
    var onExpensesChanged: (removeIndex: Int?) -> Unit = { }

    fun init(context: Context) {
        val timer = Timer("Init HomeViewModel")

        val preferences = ExpensePreferenceManager(context)

        _expenses.value?.clear()
        _expenses.value?.addAll(preferences.readAllExpenses())
        _sortingType.value = preferences.readSortingType()
        _sortingReversed.value = preferences.readSortingReversed()
        _dateFilter.value = preferences.readDateFilter()
        _amountFilter.value = preferences.readAmountFilter()
        _labelFilter.value = preferences.readLabelFilter()
        _isFilterCardOpened.value = preferences.readFilterCardOpened()

        filterExpenses(context, notifyChanges = false) // also calls sortExpenses

        timer.end()
    }

    fun sortExpenses(context: Context, sortingType: SortingType? = null, reversed: Boolean? = null, notifyChanges: Boolean = true) {
        val timer = Timer("Sort expenses")

        val timer2 = Timer("Part")
        val preferences = ExpensePreferenceManager(context)
        timer2.end()
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
            else -> { }
        }

        if (_sortingReversed.value == true)
            _expenses.value!!.reverse()

        if (notifyChanges)
           onExpensesChanged.invoke(null)

        timer.end()
    }

    private fun filterExpenses(
        context: Context,
        dateFilterChanged: Boolean = false,
        labelFilterChanged: Boolean = false,
        amountFilterChanged: Boolean = false,
        notifyChanges: Boolean = true
    ) {
        val timer = Timer("Filter expenses")

        val preferences = ExpensePreferenceManager(context)
        val labelFilter = _labelFilter.value!!
        val amountFilter = _amountFilter.value!!
        val dateFilter = _dateFilter.value!!
        val expenses = _expenses.value!!
        val searchTerm = _searchTerm.value!!

        if (labelFilterChanged)
            preferences.writeLabelFilter(labelFilter)
        if (amountFilterChanged)
            preferences.writeAmountFilter(amountFilter)
        if (dateFilterChanged)
            preferences.writeDateFilter(dateFilter)

        // Reload expenses (for new filter)
        expenses.clear()
        expenses.addAll(preferences.readAllExpenses())
        sortExpenses(context, notifyChanges = false)

        // Filter expenses
        if (dateFilter.enabled && dateFilter.until notBefore dateFilter.from) {
            expenses.retainAll{
                it.created.after(dateFilter.from) && it.created.before(dateFilter.until)
            }
        }
        if (amountFilter.enabled) {
            expenses.retainAll { amountFilter.isOkay(it.cents) }
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
        if (searchTerm != "") {
            expenses.retainAll {
                it.name.contains(searchTerm, ignoreCase = true)
                        || it.labels.any { label -> label.contains(searchTerm, ignoreCase = true) }
            }
        }

        if (notifyChanges)
            onExpensesChanged.invoke(null)

        timer.end()
    }


    /**
     * Removes the entire expense from preferences and from [expenses]
     * @return Returns the position of the expense with the given [id] in [expenses]
     */
    fun deleteExpense(context: Context, id: UUID, notifyChange: Boolean = true): Int {
        val pos = _expenses.value!!.indexOfFirst { it.id == id }
        ExpensePreferenceManager(context).removeExpense(id)
        _expenses.value!!.removeAt(pos)

        if (notifyChange)
            onExpensesChanged.invoke(pos)
        return pos
    }


    fun toggleLabelFilterEnabled(context: Context) {
        _labelFilter.value!!.enabled = !_labelFilter.value!!.enabled
        filterExpenses(context, labelFilterChanged = true)
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

    fun setSearchTerm(context: Context, searchTerm: String) {
        _searchTerm.value = searchTerm
        filterExpenses(context)
    }


    fun toggleAmountFilterEnabled(context: Context) {
        _amountFilter.value!!.enabled = !_amountFilter.value!!.enabled
        filterExpenses(context, amountFilterChanged = true)
    }

    fun setAmountFilter(context: Context, expenses: Boolean, income: Boolean) {
        _amountFilter.value!!.expenses = expenses
        _amountFilter.value!!.income = income
        filterExpenses(context, amountFilterChanged = true)
    }


    fun toggleDateFilterEnabled(context: Context) {
        _dateFilter.value!!.enabled = !_dateFilter.value!!.enabled
        filterExpenses(context, dateFilterChanged = true)
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

}