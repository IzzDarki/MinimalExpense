package com.izzdarki.minimalexpense.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException


fun openEncryptedPreferences(context: Context, preferencesName: String): SharedPreferences {
    try {
        val mainKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            preferencesName,
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: GeneralSecurityException) {
        throw RuntimeException(e)
    } catch (e: IOException) {
        throw RuntimeException(e)
    }
}


@ColorInt
fun getThemeBackgroundColor(context: Context): Int {
    val value = TypedValue()
    context.theme.resolveAttribute(
        android.R.attr.colorBackground,
        value,
        true
    ) // kind of works
    return value.data
}

fun View.setPaddingBottom(value: Int) {
    this.setPadding(
        this.paddingLeft,
        this.paddingTop,
        this.paddingRight,
        value,
    )
}

val FragmentActivity?.supportActionBar: ActionBar? get() = (this as? AppCompatActivity)?.supportActionBar

fun View.hideKeyboard() {
    // from https://stackoverflow.com/questions/1109022/how-do-you-close-hide-the-android-soft-keyboard-using-java
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}

/**
 * Hex opacity values
 *
 * 100% - FF
 * 95% - F2
 * 90% - E6
 * 85% - D9
 * 80% - CC
 * 75% - BF
 * 70% - B3
 * 65% - A6
 * 60% - 99
 * 55% - 8C
 * 50% - 80
 * 45% - 73
 * 40% - 66
 * 35% - 59
 * 30% - 4D
 * 25% - 40
 * 20% - 33
 * 15% - 26
 * 10% - 1A
 * 5% - 0D
 * 0% - 00
 */
@ColorInt
fun @receiver:ColorInt Int.withAlpha(alpha: Int): Int = Color.argb(alpha, this.red, this.green, this.blue)

fun<T> Iterable<T>.containsAny(other: Collection<T>) = this.any { it in other }

infix fun Int.mathMod(b: Int) = (this % b + b) % b

// region very specific utility



fun formatCurrency(cents: Long)
    = "%.2f €".format(cents.toFloat() / 100).replace(",", ".")

fun getDecimalPlaces(floatString: String, decimalSeparator: Char = '.'): Int {
    val commaIndex = floatString.indexOf(decimalSeparator)
    return if (commaIndex == -1)
        0
    else
        floatString.length - commaIndex - 1
}

// endregion