package com.izzdarki.minimalexpense.data

import android.content.Context
import android.content.SharedPreferences
import com.izzdarki.editlabelscomponent.EditLabelsComponent
import com.izzdarki.minimalexpense.ui.home.HomeViewModel.SortingType
import com.izzdarki.minimalexpense.util.openEncryptedPreferences
import java.util.*

class ExpensePreferenceManager(context: Context) {

    class LabelFilter(
        var labels: MutableSet<String>,
        var exclusive: Boolean,
        var enabled: Boolean
    )

    class DateFilter(
        from: Date,
        until: Date,
        var enabled: Boolean
    ) {
        /** Only year, month and day are important, time is always 00:00:00 and 0000ms */
        var from: Date = Date(0)
            set(value) {
                val calendar = Calendar.getInstance()
                calendar.time = value
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                field = calendar.time
            }

        /** Only year, month and day are important, time is always 23:59:59 and 999ms **/
        var until: Date = Date(0)
            set(value) {
                val calendar = Calendar.getInstance()
                calendar.time = value
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                field = calendar.time
            }

        init {
            this.from = from
            this.until = until
        }
    }

    fun readExpense(id: UUID) = Expense(
        id,
        name = preferences.getString(NAME.format(id), null)!!,
        cents = preferences.getLong(CENTS.format(id), -1),
        labels = readExpenseLabels(id),
        created = Date(preferences.getLong(CREATED.format(id), 0)),
        altered = Date(preferences.getLong(ALTERED.format(id), 0)),
    )

    fun writeExpense(expense: Expense) {
        val id = expense.id
        preferences.edit()
            .putString(NAME.format(id), expense.name)
            .putLong(CENTS.format(id), expense.cents)
            .putString(LABELS.format(id), expense.labels.joinToString(EditLabelsComponent.DEFAULT_SEPARATOR))
            .putLong(CREATED.format(id), expense.created.time)
            .putLong(ALTERED.format(id), expense.altered.time)
            .apply()
        addToAllExpenseIds(expense.id)
    }

    fun removeExpense(id: UUID) {
        preferences.edit()
            .remove(NAME.format(id))
            .remove(CENTS.format(id))
            .remove(LABELS.format(id))
            .remove(CREATED.format(id))
            .remove(ALTERED.format(id))
            .apply()
        removeFromAllExpenseIds(id)
    }


    fun readAllExpenses(): List<Expense>
        = readAllExpenseIds()
        .map { readExpense(it) }

    private fun collectAllExpenseLabels(): Set<String> {
        val set = mutableSetOf<String>()
        for (id in readAllExpenseIds()) {
            set.addAll(
                readExpenseLabels(id)
            )
        }
        return set.toSet()
    }

    fun getSortedLabelsDropdown(): SortedSet<String> {
        val allLabelsList = collectAllExpenseLabels()
        val alLLabelsSet: SortedSet<String> = TreeSet { label1, label2 ->
            val lengthCompare = label1.length.compareTo(label2.length)
            if (lengthCompare != 0)
                lengthCompare
            else
                label1.compareTo(label2)
        }
        alLLabelsSet.addAll(allLabelsList)
        return alLLabelsSet
    }

    fun readSortingType() = intToSortingType(preferences.getInt(SORTING_TYPE, 0))

    fun writeSortingType(sortingType: SortingType) {
        preferences.edit()
            .putInt(SORTING_TYPE, sortingTypeToInt(sortingType))
            .apply()
    }

    fun readSortingReversed() = preferences.getBoolean(SORTING_REVERSED, false)

    fun writeSortingReversed(sortingReversed: Boolean) {
        preferences.edit()
            .putBoolean(SORTING_REVERSED, sortingReversed)
            .apply()
    }

    fun readDateFilter() = DateFilter(
        from = Date(preferences.getLong(FILTER_START_DATE, 0L)),
        until = Date(preferences.getLong(FILTER_END_DATE, 0L)),
        enabled = preferences.getBoolean(FILTER_DATE_ENABLED, true)
    )

    fun writeDateFilter(dateFilter: DateFilter) {
        preferences.edit()
            .putLong(FILTER_START_DATE, dateFilter.from.time)
            .putLong(FILTER_END_DATE, dateFilter.until.time)
            .putBoolean(FILTER_DATE_ENABLED, dateFilter.enabled)
            .apply()
    }

    fun readLabelFilter() = LabelFilter(
        labels = preferences.getString(FILTER_LABELS, "")!!
            .split(EditLabelsComponent.DEFAULT_SEPARATOR)
            .filter { it.isNotEmpty() }
            .toMutableSet(),
        exclusive = preferences.getBoolean(FILTER_LABELS_EXCLUSIVE, false),
        enabled = preferences.getBoolean(FILTER_LABELS_ENABLED, true)
    )

    fun writeLabelFilter(labelFilter: LabelFilter) {
        preferences.edit().putString(
            FILTER_LABELS,
            labelFilter.labels.joinToString(EditLabelsComponent.DEFAULT_SEPARATOR)
        ).putBoolean(
            FILTER_LABELS_EXCLUSIVE,
            labelFilter.exclusive
        ).putBoolean(
            FILTER_LABELS_ENABLED,
            labelFilter.enabled
        ).apply()
    }

    fun writeFilterCardOpened(isOpened: Boolean) {
        preferences.edit()
            .putBoolean(FILTER_CARD_OPENED, isOpened)
            .apply()
    }

    fun readFilterCardOpened() = preferences.getBoolean(FILTER_CARD_OPENED, false)

    private fun readExpenseLabels(id: UUID) = preferences.getString(LABELS.format(id), "")!!
        .split(EditLabelsComponent.DEFAULT_SEPARATOR)
        .filter { it.isNotEmpty() }
        .toMutableSet()

    private fun addToAllExpenseIds(id: UUID) {
        val allIds = readAllExpenseIds().toMutableSet()
        allIds.add(id)
        writeAllExpenseIds(allIds)
    }

    private fun removeFromAllExpenseIds(id: UUID) {
        val allIds = readAllExpenseIds().toMutableSet()
        allIds.remove(id)
        writeAllExpenseIds(allIds)
    }

    private fun readAllExpenseIds(): Set<UUID> {
        val string = preferences.getString(ALL_EXPENSES, "")!!
        return if (string.isNotEmpty()) {
            string.split(LIST_DELIMITER)
                .map { UUID.fromString(it) }
                .toSet()
        } else
            setOf()
    }

    private fun writeAllExpenseIds(ids: Set<UUID>) {
        preferences.edit()
            .putString(ALL_EXPENSES, ids.joinToString(LIST_DELIMITER) { it.toString() })
            .apply()
    }

    private val preferences: SharedPreferences = openEncryptedPreferences(context, "expenses")

    companion object {
        private const val NAME: String = "%s.name" // String
        private const val CENTS: String = "%s.cents" // Long
        private const val LABELS: String = "%s.labels" // String (List)
        private const val CREATED: String = "%s.created" // Long (Date)
        private const val ALTERED: String = "%s.altered" // Long (Date)
        private const val ALL_EXPENSES: String = "all_expenses" // String (List)
        private const val SORTING_TYPE: String = "sorting_type" // Int (SortingType)
        private const val SORTING_REVERSED: String = "sorting_reversed" // Boolean
        private const val FILTER_START_DATE: String = "filter_start_date" // Long (Date)
        private const val FILTER_END_DATE: String = "filter_end_date" // Long (Date)
        private const val FILTER_DATE_ENABLED: String = "filter_date_enabled" // Boolean
        private const val FILTER_LABELS: String = "filter_labels" // String (List)
        private const val FILTER_LABELS_EXCLUSIVE: String = "filter_labels_exclusive" // Boolean
        private const val FILTER_LABELS_ENABLED: String = "filter_labels_enabled" // Boolean
        private const val FILTER_CARD_OPENED: String = "filter_card_opened" // Boolean
        private const val LIST_DELIMITER: String = ","

        private fun sortingTypeToInt(sortingType: SortingType) =
            when (sortingType) {
                SortingType.ByCreationDate -> 0
                SortingType.ByName -> 1
                SortingType.ByAmount -> 2
            }

        private fun intToSortingType(sortingInt: Int) =
            when (sortingInt) {
                0 -> SortingType.ByCreationDate
                1 -> SortingType.ByName
                else -> SortingType.ByAmount
            }
    }

}