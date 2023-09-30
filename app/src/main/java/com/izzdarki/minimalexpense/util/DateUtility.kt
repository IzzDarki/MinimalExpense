package com.izzdarki.minimalexpense.util

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.allViews
import androidx.core.widget.doAfterTextChanged
import com.izzdarki.minimalexpense.R
import com.izzdarki.minimalexpense.databinding.DialogSelectDateRangeBinding
import com.izzdarki.minimalexpense.databinding.DialogEditTextInputBinding
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.lang.Exception
import java.util.*

val dateNow: Date get() = Calendar.getInstance().time

infix fun Date.notBefore(other: Date) = !this.before(other)

infix fun Date.equalDay(other: Date): Boolean {
    val thisCalendar = Calendar.getInstance()
    thisCalendar.time = this
    val otherCalendar = Calendar.getInstance()
    otherCalendar.time = other

    return thisCalendar.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR)
            && thisCalendar.get(Calendar.DAY_OF_YEAR) == otherCalendar.get(Calendar.DAY_OF_YEAR)
}

fun isLeapYear(year: Int) = ((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0)

fun getFieldFromDate(date: Date, field: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.time = date
    return calendar.get(field)
}

fun getYearFromDate(date: Date) = getFieldFromDate(date, Calendar.YEAR)
/**
 * @return Month of the date (January is 1)
 */
fun getMonthFromDate(date: Date) = getFieldFromDate(date, Calendar.MONTH) + 1

/**
 * Create a date with given year, month and day
 * @param month First month is 1
 * @param day Day of the month, first day is 1
 */
fun dateWith(
    year: Int,
    month: Int,
    day: Int
): Date {
    val calendar = Calendar.getInstance()
    calendar.time = Date(0)
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    calendar.set(Calendar.DAY_OF_MONTH, day)
    return calendar.time
}

/**
 * Create a date with year, month, day, hour, minute, second, millisecond
 * @param month January is 1
 * @param day Day of the month, first day is 1
 * @param hour 24-hour format
 */
fun dateWith(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int,
    millisecond: Int,
): Date {
    val calendar = Calendar.getInstance()
    calendar.time = Date(0)
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    calendar.set(Calendar.DAY_OF_MONTH, day)
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, second)
    calendar.set(Calendar.MILLISECOND, millisecond)
    return calendar.time
}

/**
 * Format date as for example `02. August 2043 (13:59)`
 * Info on format: https://dzone.com/articles/java-string-format-examples
 */
fun formatDateLong(date: Date) = "%1\$td. %1\$tB %1\$tY (%1\$tR)".format(date)

/**
 * Format date as for example `29.08.2043`
 * Info on format: https://dzone.com/articles/java-string-format-examples
 */
fun formatDateShort(date: Date) = "%1\$td.%1\$tm.%1\$tY".format(date)

/**
 * Converts a string of format "dd.mm.yyyy" to date
 */
fun parseDateShort(string: String): Date? {
    return try {
        val dayMonthYear = string.split(".").map { it.toInt() }
        if(dayMonthYear[1] > 12 || dayMonthYear [0] > 31)
            return null
        dateWith(
            year = dayMonthYear[2],
            month = dayMonthYear[1],
            day = dayMonthYear[0]
        )
    } catch (e: Exception) {
        null
    }
}


// region select date dialogs
/**
 * Shows a date range selection dialog
 * @param initialStartDate Initial start date
 * @param initialEndDate Initial end date
 * @param onDateRangeSelected Callback when user has selected a date range, not called when dialog gets cancelled
 */
fun selectDateRangeDialog(
    context: Context,
    initialStartDate: Date,
    initialEndDate: Date,
    onDateRangeSelected: (startDate: Date, endDate: Date) -> Unit
) {
    val monthList = getMonthList(context)
    val rangeMonth = dateRangeToMonth(initialStartDate, initialEndDate)

    // Setup the views for the dialog
    val layoutInflater = LayoutInflater.from(context)
    val dialogViewBinding = DialogSelectDateRangeBinding.inflate(layoutInflater)

    // Helper function and variable to disable it (when the dates are edited programmatically)
    var disableUpdateChipsAfterDatesEdited = false
    fun updateChipsAfterDatesEdited() {
        if (!disableUpdateChipsAfterDatesEdited) {
            val startDate =
                parseDateShort(dialogViewBinding.startDateEditText.text.toString().trim())
            val endDate = parseDateShort(dialogViewBinding.endDateEditText.text.toString().trim())
            val month =
                if (startDate != null && endDate != null)
                    dateRangeToMonth(startDate, endDate)
                else
                    -1

            for (chip in dialogViewBinding.monthChipGroup.allViews
                .filter { it is Chip }
                .map { it as Chip }
            ) {
                val chipDate = monthList.first { it.second == chip.text.toString() }
                chip.isChecked = chipDate.first == month
                        && startDate != null
                        && chipDate.third == getYearFromDate(startDate)
            }
        }
    }

    // Setup month chips
    for (month in monthList) {
        val chip: Chip = layoutInflater.inflate(R.layout.choice_chip, dialogViewBinding.monthChipGroup, false) as Chip
        chip.text = month.second
        chip.isChecked = month.first == rangeMonth
        chip.setOnClickListener {
            val (startDate, endDate) = monthToDateRange(month.first, month.third)
            // Update date text fields (disable the callbacks)
            disableUpdateChipsAfterDatesEdited = true
            dialogViewBinding.startDateEditText.setText(formatDateShort(startDate))
            dialogViewBinding.endDateEditText.setText(formatDateShort(endDate))
            disableUpdateChipsAfterDatesEdited = false
        }

        dialogViewBinding.monthChipGroup.addView(chip)
    }

    // Setup text fields for editing dates manually
    dialogViewBinding.startDateLayout.hint = context.getString(R.string.from)
    dialogViewBinding.startDateEditText.setText(formatDateShort(initialStartDate))
    dialogViewBinding.startDateEditText.doAfterTextChanged {
        checkDateInput(
            dialogViewBinding.startDateLayout,
            dialogViewBinding.startDateEditText
        )
        updateChipsAfterDatesEdited() // Check/uncheck corresponding chip
    }
    dialogViewBinding.endDateLayout.hint = context.getString(R.string.until)
    dialogViewBinding.endDateEditText.setText(formatDateShort(initialEndDate))
    dialogViewBinding.endDateEditText.doAfterTextChanged {
        checkDateInput(
            dialogViewBinding.endDateLayout,
            dialogViewBinding.endDateEditText,
            mustBeNotBefore = parseDateShort(
                dialogViewBinding.startDateEditText.text.toString().trim()
            )
        )
        updateChipsAfterDatesEdited() // Check/uncheck corresponding chip
    }

    // Show the dialog
    AlertDialog.Builder(context)
        .setView(dialogViewBinding.root)
        .setCancelable(true)
        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
            val startDate = parseDateShort(
                dialogViewBinding.startDateEditText.text.toString().trim()
            )
            val endDate = parseDateShort(
                dialogViewBinding.endDateEditText.text.toString().trim()
            )
            // Check for errors
            if (startDate == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.s_is_not_a_valid_date).format(
                        dialogViewBinding.startDateEditText.text.toString().trim()
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            else if (endDate == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.s_is_not_a_valid_date).format(
                        dialogViewBinding.endDateEditText.text.toString().trim()
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            else if (endDate.before(startDate)) {
                Toast.makeText(
                    context,
                    R.string.end_date_cant_be_before_start_date,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else {
                // No errors => Invoke callback
                onDateRangeSelected(startDate, endDate)
            }
        }
        .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
            dialog.cancel()
        }
        .show()
}

/**
 * Displays a dialog for the user to enter a date (dd.mm.yyyy)
 * @param initialDate The date to be shown in the beginning
 * @param startDate (Null by default) The user gets an error message, if the entered date is before [startDate]
 * @param onDateSelected Callback when user has selected a date, not called when dialog gets cancelled
 */
fun selectDateDialog(
    context: Context,
    initialDate: Date,
    startDate: Date? = null,
    onDateSelected: (Date) -> Unit,
) {
    val dialogViewBinding = DialogEditTextInputBinding.inflate(LayoutInflater.from(context))

    dialogViewBinding.titleTextview.text = context.getString(R.string.enter_date)
    dialogViewBinding.textInputLayout.hint = context.getString(R.string.enter_date)
    dialogViewBinding.editText.setText(formatDateShort(initialDate))
    dialogViewBinding.editText.requestFocus()
    dialogViewBinding.editText.doAfterTextChanged {
        checkDateInput(
            dialogViewBinding.textInputLayout,
            dialogViewBinding.editText,
            startDate
        )
    }
    // Initial check
    checkDateInput(
        dialogViewBinding.textInputLayout,
        dialogViewBinding.editText,
        startDate
    )

    val dialog = AlertDialog.Builder(context)
        .setView(dialogViewBinding.root)
        .setCancelable(true)
        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
            val date = parseDateShort(
                dialogViewBinding.editText.text.toString().trim()
            )
            if (date != null) {
                onDateSelected(date)
            }
        }
        .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
            dialog.cancel()
        }
        .show()

    dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE) // Without this keyboard closes when alert dialog opens
}


fun checkDateInput(
    dateInputLayout: TextInputLayout,
    dateEditText: TextInputEditText,
    mustBeNotBefore: Date? = null
) {
    val context = dateInputLayout.context
    val dateString = dateEditText.text.toString().trim()
    val date = parseDateShort(dateString)
    if (date == null)
        dateInputLayout.error = context.getString(R.string.this_is_not_a_valid_date)
    else if (mustBeNotBefore != null && date.before(mustBeNotBefore))
        dateInputLayout.error = context.getString(R.string.end_date_cant_be_before_start_date)
    else
        dateInputLayout.error = null
}
// endregion


fun dateRangeToMonth(startDate: Date, endDate: Date): Int? {
    val month = getMonthFromDate(startDate)
    val year = getYearFromDate(startDate)

    if (month == getMonthFromDate(endDate) && year == getYearFromDate(endDate)) {
        // Check if startDate and endDate are beginning end end of a month (don't care about the time of the day)
        val (expectedStart, expectedEnd) = monthToDateRange(month, getYearFromDate(startDate))
        if (startDate equalDay expectedStart && endDate equalDay expectedEnd)
            return month
    }
    return null
}

/**
 * Convert a month (with a year) to a start and end date
 * @param month Month (January is 1)
 */
fun monthToDateRange(month: Int, year: Int): Pair<Date, Date> {
    val endHour = 23
    val endMinute = 59
    val endSecond = 59
    val endMillis = 999

    val startDate = dateWith(year, month, 1)
    val endDate = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> dateWith(year, month, 31, endHour, endMinute, endSecond,endMillis)
        4, 6, 9, 11 -> dateWith(year, month, 30, endHour, endMinute, endSecond,endMillis)
        2 -> dateWith(
            year, month,
            if (isLeapYear(year)) 29 else 28,
            endHour, endMinute, endSecond,endMillis
        )
        else -> throw IllegalArgumentException("Given month is not valid: $month")
    }
    return Pair(
        startDate,
        endDate
    )
}

/**
 * Get a list the current month and the 11 previous months
 * @return
 *  [Triple.first]: Month number (January is 1)
 *  [Triple.second]: Month name
 *  [Triple.third]: Year number
 */
fun getMonthList(context: Context): List<Triple<Int, String, Int>> {
    val list = mutableListOf<Triple<Int, String, Int>>()
    val currentMonth = getMonthFromDate(dateNow) - 1 // Calculations are done with January as 0
    val currentYear = getYearFromDate(dateNow)

    for (i in 0 until 12) {
        val isPreviousYear = currentMonth < i
        val monthNum = ((currentMonth - i) mathMod 12) + 1 // mathMod works for negative inputs too
        var monthName = context.getString(
            when (monthNum) {
                1 -> R.string.january
                2 -> R.string.february
                3 -> R.string.march
                4 -> R.string.april
                5 -> R.string.may
                6 -> R.string.june
                7 -> R.string.july
                8 -> R.string.august
                9 -> R.string.september
                10 -> R.string.october
                11 -> R.string.november
                else -> R.string.december
            }
        )
        var year = currentYear

        if (isPreviousYear) {
            val previousYear = (currentYear - 1).toString()
            monthName = monthName + " " + previousYear.substring(previousYear.length - 2)
            year = currentYear - 1
        }
        list.add(
            Triple(monthNum, monthName, year)
        )
    }
    return list
}