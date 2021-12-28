package com.izzdarki.editlabelscomponent

import android.app.Activity
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.view.allViews
import com.izzdarki.editlabelscomponent.Utility.isViewHitByTouchEvent
import com.izzdarki.editlabelscomponent.Utility.showKeyboard
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.*

/**
 * Using this component enables an activity to create an UI for editing labels
 *
 * You need to do the following things
 * - override [Activity.dispatchTouchEvent] (see [EditLabelsComponent.dispatchTouchEvent] for further information)
 * - create an instance of this class in for example [Activity.onCreate]
 * - eventually call [displayLabels] to easily create and display initial labels
 *
 * @property separator Labels are not allowed to contain this String.
 *  That way it can be used as a separator when converting a list of labels to a single string
 * @property allLabels This list is used to create an auto-complete dropdown when editing labels.
 *  The dropdown contains only suggestions for labels that are not already added.
 *  When [allowNewLabels] is `false`, only labels that are already in [allLabels] are allowed.
 *  When [allowNewLabels] is `true`, new labels will **not** be added to [allLabels]
 * @property allowNewLabels See [allLabels]
 * @property onLabelAdded Called when a new label is added
 * @property onLabelRemoved Called when a label is removed
 *
 * @constructor
 * @param labelsChipGroup Used as a container for all label chips
 * @param labelsAddChip Must be inside [labelsChipGroup].
 *  Clicking this chip will display a UI for the user to create a new label
 *  @param separator See [separator]
 *  @param allLabels See [allLabels]
 *  @param allowNewLabels See [allLabels]
 *  @param onLabelAdded See [onLabelAdded]
 */
class EditLabelsComponent(
    private var labelsChipGroup: ChipGroup,
    private var labelsAddChip: Chip,
    allLabels: SortedSet<String> = sortedSetOf(),
    var allowNewLabels: Boolean = true,
    var separator: String = DEFAULT_SEPARATOR,
    var onLabelAdded: (label: String) -> Unit = {},
    var onLabelRemoved: (label: String) -> Unit = {},
) {
    private val context get() = labelsAddChip.context
    private val internalCurrentLabels = mutableSetOf<String>()
    private var currentlyEditedLabel: String? = null

    /**
     * @param labelsChipGroup Used as a container for all label chips
     * @param labelsAddChip Must be inside [labelsChipGroup].
     *  Clicking this chip will display a UI for the user to create a new label
     * @param separator See [separator]
     * @param allLabels See [allLabels]
     * @param allowNewLabels See [allLabels]
     * @param onLabelChanged Gets called when a new label is added and when a label is removed
     *  See [onLabelAdded] and [onLabelRemoved]
     */
    constructor(
        labelsChipGroup: ChipGroup,
        labelsAddChip: Chip,
        allLabels: SortedSet<String> = sortedSetOf(),
        allowNewLabels: Boolean = true,
        separator: String = DEFAULT_SEPARATOR,
        onLabelChanged: () -> Unit = {}
    ) : this(
        labelsChipGroup,
        labelsAddChip,
        allLabels,
        allowNewLabels,
        separator,
        onLabelAdded = { onLabelChanged() },
        onLabelRemoved = { onLabelChanged() }
    )

    var allLabels: SortedSet<String> = sortedSetOf()
        set(value) {
            field = value

            // Remove all chips, that are no longer in allLabels
            for (view in labelsChipGroup.allViews) {
                if (view is Chip && view != labelsAddChip && view.text !in allLabels) {
                    labelsChipGroup.removeView(view)
                    internalRemoveLabel(view.text.toString())
                }
            }
        }

    init {
        this.allLabels = allLabels
        // labels add chip
        labelsAddChip.setOnClickListener {
            addEditTextToLabels(context.getString(R.string.new_label))
        }
    }

    /**
     * Displays given labels
     * @param labels List of labels to display
     */
    fun displayLabels(labels: Set<String>) {
        for (label in labels) {
            // Add label chip at the end
            addChipToLabelsWithoutCallback(label, internalCurrentLabels.size + 1)
            internalCurrentLabels.add(label)
        }
    }

    /**
     * Get the labels that are currently added and visible to the user
     */
    val currentLabels get() = internalCurrentLabels.toSet()

    /**
     * For being able to finish editing chips when the user clicks elsewhere,
     * activities must override [Activity.dispatchTouchEvent] and call this method from there.
     * It finished editing in certain situations
     *
     * @return
     *  `false`, when touch event should be processed as usual => your overridden `dispatchTouchEvent` needs to call `super.dispatchTouchEvent` and return its value
     *  `true`, when the touch event should be consumed => your overridden `dispatchTouchEvent` also needs to return `true`,
     *
     *  You can use this code:
        ```kotlin
        override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
            // Every touch event goes through this function
            if (yourEditLabelsComponent.dispatchTouchEvent(ev))
            return true
            else
            return super.dispatchTouchEvent(ev)
        }
        ```
     */
    fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // This function finishes editing of a label in certain situations

        if (ev?.actionMasked == MotionEvent.ACTION_DOWN) {

            // Check if touch event hits the edited label => don't finish editing it (the user wants to interact with the edited label)
            val editText = getEditTextFromChipGroup()
                ?: return false // if there is no EditText, touch events can be dispatched as usual

            if (!isViewHitByTouchEvent(editText, ev)) {
                getEditTextHitByTouchEvent(ev)?.requestFocus() // request focus to EditText if the touch event hits any EditText (before the focus gets cleared by finishEditingChip)
                finishEditingChip(editText)
            }

            // Check if touch event hits one of the chips => consume the touch event
            for (view in labelsChipGroup.allViews) {
                if (view is Chip && isViewHitByTouchEvent(view, ev)) {
                    return true // consume the touch event (finishing editing while also triggering other chip related UI is too much for a single touch)
                }
            }
        }
        return false // dispatch touch events as usual
    }

    private fun addChipToLabels(label: String, index: Int = 1) {
        addChipToLabelsWithoutCallback(label, index)
        if (label !in internalCurrentLabels) {
            internalAddLabel(label)
        }
    }

    private fun addChipToLabelsWithoutCallback(text: String, index: Int = 1) {
        val chip = Chip(context)
        chip.text = text
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            labelsChipGroup.removeView(chip)
            internalRemoveLabel(text)
        }
        chip.setOnLongClickListener {
            startEditingChip(chip)
            return@setOnLongClickListener true // consumed long click
        }
        labelsChipGroup.addView(chip, index)
    }

    private fun addEditTextToLabels(text: String, index: Int = 1) {
        val editText = AutoCompleteTextView(context)
        editText.isSingleLine = true
        editText.setText(text)
        editText.setSelectAllOnFocus(true)

        editText.imeOptions = EditorInfo.IME_ACTION_DONE
        editText.setOnEditorActionListener { _, _, _ ->
            // when action (done) triggered, finish editing
            finishEditingChip(editText)
            return@setOnEditorActionListener true // consumed the action
        }

        editText.setAdapter(
            ArrayAdapter(context, R.layout.auto_complete_dropdown_item, allLabels.filter { it !in internalCurrentLabels })
        )
        editText.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
            finishEditingChip(editText)
        }
        editText.minWidth = context.resources.getDimension(R.dimen.labels_edit_text_min_width).toInt() // This looks better than changing the EditText.dropdownWidth
        editText.threshold = 1
        editText.showDropDown()

        labelsChipGroup.addView(editText, index)
        editText.requestFocus()
        showKeyboard(editText)
    }

    private fun startEditingChip(chip: Chip) {
        currentlyEditedLabel = chip.text.toString()
        val index = labelsChipGroup.indexOfChild(chip)
        labelsChipGroup.removeView(chip)
        addEditTextToLabels(chip.text.toString(), index)
    }

    private fun finishEditingChip(editText: AutoCompleteTextView) {
        // clear focus and remove editText
        editText.clearFocus()
        val index = labelsChipGroup.indexOfChild(editText)
        labelsChipGroup.removeView(editText)

        val newLabel = editText.text.toString().trim()
        if (isNewLabelOkOrShowError(newLabel))
            addChipToLabels(newLabel, index)
        else if (currentlyEditedLabel != null) {
            val labelBeforeEdit = currentlyEditedLabel!!
            currentlyEditedLabel = null
            addChipToLabelsWithoutCallback(labelBeforeEdit, index)
        }
    }

    /**
     * Adds label internally and calls [onLabelAdded],
     * but only if the label is not already added
     */
    private fun internalAddLabel(label: String) {
        if (label !in internalCurrentLabels) {
            if (currentlyEditedLabel != null) {
                val labelBeforeEdit = currentlyEditedLabel!!
                currentlyEditedLabel = null
                internalRemoveLabel(labelBeforeEdit)
            }
            internalCurrentLabels.add(label)
            onLabelAdded(label)
        }
    }

    /**
     * Removes label internally and calls [onLabelRemoved],
     * but only if the label was added before
     */
    private fun internalRemoveLabel(label: String) {
        if (label in internalCurrentLabels) {
            internalCurrentLabels.remove(label)
            onLabelRemoved(label)
        }
    }

    private fun getEditTextFromChipGroup(): AutoCompleteTextView? {
        return labelsChipGroup.allViews.firstOrNull { it is AutoCompleteTextView } as? AutoCompleteTextView
    }

    private fun getEditTextHitByTouchEvent(ev: MotionEvent): EditText? {
        return labelsChipGroup.rootView.allViews.firstOrNull {
            it is EditText && isViewHitByTouchEvent(it, ev)
        } as? EditText
    }

    private fun isNewLabelOkOrShowError(newLabel: String): Boolean {
        if (newLabel == "") {
            Toast.makeText(context, R.string.error_label_cant_be_empty, Toast.LENGTH_SHORT).show()
            return false
        } else if (newLabel.contains(separator)) {
            val errorMessage = String.format(
                context.getString(R.string.error_label_cant_contain_x),
                separator
            )
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            return false
        } else if (newLabel in internalCurrentLabels && newLabel != currentlyEditedLabel) {
            Toast.makeText(context, R.string.error_label_already_added, Toast.LENGTH_SHORT).show()
            return false
        } else if (!allowNewLabels && newLabel !in allLabels) {
            Toast.makeText(context, R.string.label_does_not_exist, Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    companion object {
        const val DEFAULT_SEPARATOR = "§]7%}$"
    }
}