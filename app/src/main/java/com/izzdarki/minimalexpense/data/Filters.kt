package com.izzdarki.minimalexpense.data

import java.util.*

class LabelFilter(
    var includedLabels: MutableSet<String>,
    var excludedLabels: MutableSet<String>,
    var isIntersection: Boolean,
    var enabled: Boolean
)

class AmountFilter(
    var expenses: Boolean,
    var income: Boolean,
    var enabled: Boolean,
) {
    fun isOkay(cents: Long) = (cents < 0L && income) || (cents >= 0 && expenses)
}

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