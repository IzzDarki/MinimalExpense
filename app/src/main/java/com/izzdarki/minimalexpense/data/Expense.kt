package com.izzdarki.minimalexpense.data

import com.izzdarki.minimalexpense.util.dateNow
import java.util.*

data class Expense(
    val id: UUID,
    var name: String,
    var cents: Long,
    var labels: MutableSet<String> = mutableSetOf(),
    var notes: String? = null,
    var created: Date = dateNow,
    var altered: Date = dateNow, // this is not visible to the user, but gets saved to preferences anyway (maybe needed in the future)
)