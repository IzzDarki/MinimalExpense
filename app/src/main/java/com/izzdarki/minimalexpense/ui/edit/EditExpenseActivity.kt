package com.izzdarki.minimalexpense.ui.edit

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import com.izzdarki.minimalexpense.R
import com.izzdarki.minimalexpense.data.ExpensePreferenceManager
import com.izzdarki.minimalexpense.data.Expense
import com.izzdarki.minimalexpense.databinding.ActivityEditExpenseBinding
import com.izzdarki.editlabelscomponent.EditLabelsComponent
import com.izzdarki.minimalexpense.data.SettingsManager
import com.izzdarki.minimalexpense.util.*
import java.util.*
import kotlin.math.abs
import kotlin.math.absoluteValue

class EditExpenseActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CREATE_NEW: String = "extra_create_new" // Boolean
        const val EXTRA_ID: String = "extra_id" // String (UUID)

        fun startForEdit(context: Context, id: UUID) {
            val intent = Intent(context, EditExpenseActivity::class.java)
            intent.putExtra(EXTRA_ID, id)
            context.startActivity(intent)
        }

        fun startForCreate(context: Context) {
            val intent = Intent(context, EditExpenseActivity::class.java)
            intent.putExtra(EXTRA_CREATE_NEW, true)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        expense = if (isCreateNewIntent) {
            Expense(
                id = UUID.randomUUID(),
                name = getString(R.string.expense_default_name),
                cents = 0,
            )
        } else {
            expensePreferences.readExpense(intent.getSerializableExtra(EXTRA_ID) as UUID)
        }

        // Toolbar
        binding.toolbar.title = expense.name
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Name input
        binding.nameInputEditText.setText(expense.name)
        binding.nameInputEditText.doAfterTextChanged {
            val name = binding.nameInputEditText.text.toString()
            if (name.isEmpty())
                binding.nameInput.error = getString(R.string.name_cant_be_empty)
            else
                binding.nameInput.error = null
        }
        if (isCreateNewIntent) {
            // When creating a new expense start with name selected
            binding.nameInputEditText.requestFocus()
            binding.nameInputEditText.selectAll()
        }
        binding.nameInputEditText.setOnEditorActionListener { v, actionId, event ->
            // Custom handling of action next
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.amountInputEditText.selectAll()
                binding.amountInputEditText.requestFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        // Amount input
        binding.amountInput.suffixText = SettingsManager(this).currencySymbol
        val amountText = formatCurrencyWithoutSymbol(expense.cents.absoluteValue)
        binding.amountInputEditText.setText(amountText)
        binding.amountInputEditText.doAfterTextChanged {
            val amountString = binding.amountInputEditText.text.toString()
            val amount = amountString.toDoubleOrNull()
            binding.amountInput.error = when {
                amount == null -> getString(R.string.this_is_not_a_valid_amount)
                getDecimalPlaces(amountString) > 2 -> getString(R.string.only_two_decimal_places_are_allowed)
                else -> null
            }
        }
        // Format number when focus lost
        binding.amountInputEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.amountInput.error == null) {
                // Toggle expense/income when user has entered a negative sign '-'
                if (binding.amountInputEditText.text.toString().trim().contains('-')) {
                    if (!binding.expenseChip.isChecked)
                        binding.expenseChip.isChecked = true
                    else
                        binding.incomeChip.isChecked = true
                }
                // Update amount text (also removes negative sign)
                binding.amountInputEditText.setText(formatCurrencyWithoutSymbol(readCents().absoluteValue))
            }
        }
        // Format number on IME Action (Done)
        binding.amountInputEditText.setOnEditorActionListener { _, _, _ ->
            if (binding.amountInput.error == null) {
                binding.amountInputEditText.setText(formatCurrencyWithoutSymbol(readCents()))
                binding.amountInputEditText.setSelection(binding.amountInputEditText.length())
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        // Expense/income toggle
        if (expense.cents >= 0)
            binding.expenseChip.isChecked = true
        else
            binding.incomeChip.isChecked = true

        // Labels component
        editLabelsComponent = EditLabelsComponent(
            binding.labelsChipGroup,
            binding.labelsAddChip,
            allLabels = expensePreferences.getSortedLabelsDropdown(),
        )
        editLabelsComponent.displayLabels(expense.labels)

        // Creation date
        updateCreationDateText()
        binding.createdTextView.setOnLongClickListener {
            selectDateDialog(
                context = this,
                initialDate = expense.created
            ) { date ->
                expense.created = date
                updateCreationDateText()
            }
            true // Long click always consumed
        }
    }

    /**
     * @return true if activity was finished
     */
    private fun requestCancel(): Boolean {
        expense.name = readName()
        expense.cents = readCents()
        expense.labels = readLabels()

        if (isCreateNewIntent || hasBeenModified) {
            val title = getString(
                if (isCreateNewIntent)
                    R.string.discard_new_expense
                else
                    R.string.discard_changes
            )
            val message = getString(
                if (isCreateNewIntent)
                    R.string.nothing_will_be_saved
                else
                    R.string.changes_are_not_saved
            )
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                    finish()
                }
                .setNegativeButton(android.R.string.cancel) { dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.cancel()
                }
                .show()
            return false
        }
        else {
            finish()
            return true
        }
    }
    
    private fun saveAndFinish() {
        if (!isError) {
            expense.altered = dateNow
            expense.name = readName()
            expense.cents = readCents()
            expense.labels = readLabels()
            expensePreferences.writeExpense(expense)
            finish()
        }
        else {
            Toast.makeText(this,
                R.string.there_are_still_errors,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun requestDelete() {
        val title = getString(
            if (isCreateNewIntent)
                R.string.discard_new_expense
            else
                R.string.delete_expense
        )
        val message = getString(
            if (isCreateNewIntent)
                R.string.nothing_will_be_saved
            else
                R.string.this_cannot_be_reversed
        )
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                expensePreferences.removeExpense(expense.id)
                finish()
            }
            .setNegativeButton(android.R.string.cancel) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.cancel()
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_expense, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_delete -> requestDelete()
            R.id.menu_done -> saveAndFinish()
            else -> return false
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return requestCancel()
    }

    override fun onBackPressed() {
        requestCancel()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // Every touch event goes through this function
        return if (editLabelsComponent.dispatchTouchEvent(ev))
            true
        else
            super.dispatchTouchEvent(ev)
    }

    private lateinit var binding: ActivityEditExpenseBinding
    private val expensePreferences by lazy { ExpensePreferenceManager(this) }
    private lateinit var expense: Expense
    private lateinit var editLabelsComponent: EditLabelsComponent

    private val hasBeenModified get() = expense != expensePreferences.readExpense(expense.id)
    private val isCreateNewIntent get() = intent.getBooleanExtra(EXTRA_CREATE_NEW, false)
    private val isError get() = binding.nameInput.error != null || binding.amountInput.error != null

    private fun readName(): String = binding.nameInputEditText.text.toString().trim()

    private fun readCents(): Long {
        val cents = binding.amountInputEditText.text
            .toString()
            .split('.').run {
                val beforeComma = getOrNull(0)?.toLongOrNull() ?: 0
                val afterComma = getOrNull(1)?.toLongOrNull() ?: 0

                beforeComma * 100 + afterComma
            }

        return if (binding.incomeChip.isChecked)
            -cents
        else
            cents
    }

    private fun readLabels() = editLabelsComponent.currentLabels.toMutableSet()

    private fun updateCreationDateText() {
        binding.createdTextView.text = getString(R.string.created_x).format(formatDateLong(expense.created))
    }
}