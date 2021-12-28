package com.izzdarki.minimalexpense.debug

import android.util.Log

class Timer(
    private val name: String
) {
    private val startMillis = System.currentTimeMillis()

    init {
        level += 1
    }

    fun end() {
        val millis = System.currentTimeMillis() - startMillis
        level -= 1
        val indentation = "    ".repeat(level)
        Log.d("timing", "$indentation$name: $millis ms")
    }

    companion object {
        private var level: Int = 0
    }

}