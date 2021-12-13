package com.bennet.preferenceutility

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.res.TypedArrayUtils
import androidx.core.view.doOnPreDraw
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlin.math.max


/**
 * @attr name options
 */
class ChoicePreference : Preference {

    private val options: MutableList<String> = mutableListOf()

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        widgetLayoutResource = R.layout.preference_choice
        if (attrs == null)
            throw IllegalArgumentException("Expected attrs to be not null")

        val optionsString = getAttributeStringValue(attrs, null, ATTR_OPTIONS)
            ?: throw IllegalArgumentException("Missing attribute '$ATTR_OPTIONS'")

        options.addAll(
            optionsString.split("|")
                .filter { it.isNotEmpty() }
        )
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs,
        TypedArrayUtils.getAttr(
            context!!,
            R.attr.preferenceStyle,
            android.R.attr.preferenceStyle
        ))

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        if (holder == null)
            return

        val layoutInflater = LayoutInflater.from(context)

        val chipGroup = holder.findViewById(R.id.chip_group) as ChipGroup
        val maxWith = context.resources.getDimension(R.dimen.max_width_chip_group).toInt()
        chipGroup.doOnPreDraw {
            if (chipGroup.width > maxWith)
                chipGroup.layoutParams = LinearLayout.LayoutParams(maxWith, chipGroup.height)
        }

        val currentOption = getPersistedInt(0)
        for ((index, option) in options.withIndex()) {
            val chip = layoutInflater.inflate(R.layout.choice_chip, null) as Chip
            chip.text = option
            if (index == currentOption)
                chip.isChecked = true

            chip.setOnClickListener {
                persistInt(index)
            }

            chipGroup.addView(chip)
        }

    }

    private fun getAttributeStringValue(
        attrs: AttributeSet,
        namespace: String?,
        name: String,
        defaultValue: String? = null
    ): String? {
        val resId = attrs.getAttributeResourceValue(namespace, name, 0)
        return if (resId == 0) {
            attrs.getAttributeValue(namespace, name) ?: defaultValue
        } else {
            context.resources.getString(resId)
        }
    }

    companion object {
        const val ATTR_OPTIONS = "options"
    }

}