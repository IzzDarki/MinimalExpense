package com.izzdarki.editlabelscomponent

import android.app.Activity
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager

object Utility {

    fun isViewHitByTouchEvent(view: View, ev: MotionEvent): Boolean {
        val rectPos = IntArray(2)
        view.getLocationOnScreen(rectPos)
        val hitRect = Rect(
            rectPos[0],
            rectPos[1],
            rectPos[0] + view.width,
            rectPos[1] + view.height
        )
        return hitRect.contains(ev.x.toInt(), ev.y.toInt())
    }

    fun showKeyboard(view: View) {
        val imm =
            view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

}