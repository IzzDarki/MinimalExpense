package com.izzdarki.minimalexpense.ui.home

import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.izzdarki.editlabelscomponent.EditLabelsComponent
import com.izzdarki.editlabelscomponent.generateStrikethroughChip
import com.izzdarki.editlabelscomponent.strikethroughOnCheckedChanged
import com.izzdarki.minimalexpense.R
import com.izzdarki.minimalexpense.data.ExpensePreferenceManager
import com.izzdarki.minimalexpense.data.SettingsManager
import com.izzdarki.minimalexpense.databinding.FragmentHomeBinding
import com.izzdarki.minimalexpense.ui.edit.EditExpenseActivity
import com.izzdarki.minimalexpense.util.*
import com.izzdarki.minimalexpense.debug.Timer
import java.util.*
import kotlin.math.absoluteValue

class HomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // this fragment uses the action bar menu
        //setRetainInstance(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        viewModel.init(requireContext())

        // filter card
        binding.filterCollapseButton.setOnClickListener {
            setFilterCardOpened(false)
        }
        setFilterCardOpened(
            viewModel.isFilterCardOpened.value!!
        )

        onCreateLabelFilter()
        onCreateTypeFilter()
        onCreateDateFilter()
        onCreateRecyclerView()

        viewModel.onExpensesChanged = { removeIndex ->
            if (removeIndex == null)
                adapter?.notifyDataSetChanged()
            else
                adapter?.notifyItemRemoved(removeIndex)
            updateSum()
            updateFilterLabelsComponent()
        }

        // Floating action button
        binding.addExpenseButton.setOnClickListener {
            EditExpenseActivity.startForCreate(requireContext())
        }

        binding.root.doOnPreDraw {
            updateSum() // Can't be done instantly because supportActionBar is null when creating this fragment
        }

        calledAfterOnCreateView = true
    }

    private fun onCreateLabelFilter() {
        updateLabelFilterEnabled() // make the section appear enabled or disabled
        binding.labelsEnabledButton.setOnClickListener {
            viewModel.toggleLabelFilterEnabled(requireContext())
            updateLabelFilterEnabled()
        }
        binding.inclusiveChip.isChecked = !viewModel.labelFilter.value!!.isIntersection
        binding.exclusiveChip.isChecked = viewModel.labelFilter.value!!.isIntersection
        binding.inclusiveChip.setOnClickListener {
            viewModel.setLabelFilterIntersection(requireContext(), false)
            binding.inclusiveChip.isChecked = true
        }
        binding.exclusiveChip.setOnClickListener {
            viewModel.setLabelFilterIntersection(requireContext(), true)
            binding.exclusiveChip.isChecked = true
        }
        val checkedColor = requireContext().getColor(R.color.income_color).withAlpha(70)
        val uncheckedColor = requireContext().getColor(R.color.expense_color).withAlpha(70)
        filterLabelsComponent = EditLabelsComponent(
            binding.labelsContentChipGroup,
            binding.labelsAddChip,
            allLabels = ExpensePreferenceManager(requireContext()).getSortedLabelsDropdown(),
            allowNewLabels = false,
            checkableFunctionality = true,
            generateChip = { context, label, isChecked -> generateStrikethroughChip(checkedColor, uncheckedColor, context, label, isChecked) }, // strike through unchecked labels
            onCheckedChanged = { label, isChecked, chip ->
                strikethroughOnCheckedChanged(checkedColor, uncheckedColor, label, isChecked, chip) // strike through unchecked labels
                updateLabelFilterInViewModel()
            }
        )
        filterLabelsComponent.setOnLabelChanged { _, _, _ ->
            updateLabelFilterInViewModel()
        }
        // Load initial labels
        filterLabelsComponent.displayLabelsWithCheckedStatus(
            viewModel.labelFilter.value!!.includedLabels.map { label -> Pair(label, true) }
                .union(viewModel.labelFilter.value!!.excludedLabels.map { label -> Pair(label, false) })
        )
    }

    private fun onCreateTypeFilter() {
        updateTypeFilterEnabled()

        binding.typeEnabledButton.setOnClickListener {
            viewModel.toggleAmountFilterEnabled(requireContext())
            updateTypeFilterEnabled()
        }

        binding.typeExpenseChip.isChecked = viewModel.amountFilter.value!!.expenses
        binding.typeIncomeChip.isChecked = viewModel.amountFilter.value!!.income

        binding.typeExpenseChip.setOnClickListener {
            updateTypeFilter()
        }

        binding.typeIncomeChip.setOnClickListener {
            updateTypeFilter()
        }

    }

    private fun onCreateDateFilter() {
        updateDateFilterEnabled() // make the section appear enabled or disabled
        binding.dateEnabledButton.setOnClickListener {
            viewModel.toggleDateFilterEnabled(requireContext())
            updateDateFilterEnabled()
        }

        binding.dateFromChip.text = formatDateShort(viewModel.dateFilter.value!!.from)
        binding.dateToChip.text = formatDateShort(viewModel.dateFilter.value!!.until)

        val onDatesSelected = {
            selectDateRangeDialog(
                requireContext(),
                initialStartDate = viewModel.dateFilter.value!!.from,
                initialEndDate = viewModel.dateFilter.value!!.until
            ) { from, to ->
                viewModel.setDateFilterDates(requireContext(), from, to)
                binding.dateFromChip.text = formatDateShort(from)
                binding.dateToChip.text = formatDateShort(to)
            }
        }

        binding.dateFromChip.setOnClickListener {
            onDatesSelected()
        }
        binding.dateToChip.setOnClickListener {
            onDatesSelected()
        }
    }

    private fun onCreateRecyclerView() {
        binding.recyclerView.doOnPreDraw {
            it.setPaddingBottom(binding.addExpenseButton.height + 2 * binding.addExpenseButton.paddingBottom)
            // needed to prevent the floating action button to overlap with the recyclerview
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val adapter = ExpenseAdapter(
            expenses = viewModel.expenses.value!!,
            onClickListener = { pos ->
                EditExpenseActivity.startForEdit(requireContext(), viewModel.expenses.value!![pos].id)
            }
        )
        binding.recyclerView.adapter = adapter

        val selectionTracker = SelectionTracker.Builder(
            SELECTION_ID,
            binding.recyclerView,
            adapter.ItemKeyProvider(),
            MultiSelectItemDetailsLookup(binding.recyclerView),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()
        selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {
            override fun onSelectionChanged() {
                activity?.invalidateOptionsMenu()
            }
        })
        adapter.selectionTracker = selectionTracker
    }

    override fun onResume() {
        val timer = Timer("Home Fragment on resume")

        if (!calledAfterOnCreateView) {
            viewModel.init(requireContext()) // Only do this when onCreateView was not called before
            viewModel.onExpensesChanged.invoke(null)
        }
        else
            calledAfterOnCreateView = false

        super.onResume()

        timer.end()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Gets called by MainActivity.dispatchTouchEvent
     */
    fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return if (isFilterEdit)
            filterLabelsComponent.dispatchTouchEvent(ev)
        else
            false
    }


    // region action bar menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        when {
            adapter?.selectionTracker?.isSingleSelected == true -> inflater.inflate(R.menu.home_action_bar_with_single_item_selected_menu, menu)
            adapter?.selectionTracker?.isMultipleSelected == true -> inflater.inflate(R.menu.home_action_bar_with_multiple_items_selected_menu, menu)
            else -> inflater.inflate(R.menu.home_action_bar_menu, menu)
        }

        when (viewModel.sortingType.value!!) {
            HomeViewModel.SortingType.ByCreationDate -> menu.findItem(R.id.home_action_bar_sort_by_creation_date).isChecked = true
            HomeViewModel.SortingType.ByName -> menu.findItem(R.id.home_action_bar_sort_by_name).isChecked = true
            HomeViewModel.SortingType.ByAmount -> menu.findItem(R.id.home_action_bar_sort_by_amount).isChecked = true
        }
        menu.findItem(R.id.home_action_bar_sort_reverse).isChecked = viewModel.sortingReversed.value!!

        // Search
        val searchView = menu.findItem(R.id.home_action_bar_search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_for_expense_or_label)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchTerm(requireContext(), searchTerm = query ?: "")
                return true  // Query has been handled
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return this.onQueryTextSubmit(newText)
            }

        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_action_bar_edit_selected_item -> {
                EditExpenseActivity.startForEdit(
                    requireContext(),
                    id = UUID.fromString(adapter!!.selectionTracker.selection.first())
                )
            }
            R.id.home_action_bar_delete_selected_item -> {
                if (adapter!!.selectionTracker.isSingleSelected)
                    requestDeleteSingle(id = UUID.fromString(adapter!!.selectionTracker.selection.first()))
                else
                    requestDeleteMultiple(
                        ids = adapter!!.selectionTracker.selection.map { UUID.fromString(it) }
                    )
            }
            R.id.home_action_bar_filter -> {
                toggleFilterCardVisibility()
            }
            R.id.home_action_bar_sort_by_creation_date -> {
                viewModel.sortExpenses(
                    requireContext(),
                    HomeViewModel.SortingType.ByCreationDate
                )
                adapter?.notifyDataSetChanged()
                item.isChecked = true
            }
            R.id.home_action_bar_sort_by_name -> {
                viewModel.sortExpenses(
                    requireContext(),
                    HomeViewModel.SortingType.ByName
                )
                adapter?.notifyDataSetChanged()
                item.isChecked = true
            }
            R.id.home_action_bar_sort_by_amount -> {
                viewModel.sortExpenses(
                    requireContext(),
                    HomeViewModel.SortingType.ByAmount
                )
                adapter?.notifyDataSetChanged()
                item.isChecked = true
            }
            R.id.home_action_bar_sort_reverse -> {
                val lastReversed = viewModel.sortingReversed.value!!
                viewModel.sortExpenses(
                    requireContext(),
                    reversed = !lastReversed
                )
                adapter?.notifyDataSetChanged()
                item.isChecked = !lastReversed
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
    // endregion


    private lateinit var viewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!! // This property is only valid between onCreateView and onDestroyView
    private lateinit var filterLabelsComponent: EditLabelsComponent
    private var calledAfterOnCreateView = false

    companion object {
        private const val SELECTION_ID: String = "expense_selection"
    }


    // helpers
    private fun toggleFilterCardVisibility() {
        if (isFilterEdit)
            setFilterCardOpened(false)
        else
            setFilterCardOpened(true)
    }

    private fun setFilterCardOpened(isOpened: Boolean) {
        binding.filterCard.visibility =
            if (isOpened)
                View.VISIBLE
            else
                View.GONE
        viewModel.setFilterCardOpened(requireContext(), isOpened)
    }

    /**
     * Make the section appear enabled or disabled
     */
    private fun updateLabelFilterEnabled() {
        binding.labelsEnabledButton.setImageDrawable(
            getFilterEnabledImage(viewModel.labelFilter.value!!.enabled)
        )
        if (viewModel.labelFilter.value!!.enabled) {
            binding.labelsHeadline.isEnabled = true
            binding.inclusiveChip.visibility = View.VISIBLE
            binding.exclusiveChip.visibility = View.VISIBLE
            binding.labelsContentChipGroup.visibility = View.VISIBLE
        }
        else {
            binding.labelsHeadline.isEnabled = false
            binding.inclusiveChip.visibility = View.INVISIBLE // invisible needed because date section needs this for constraints
            binding.exclusiveChip.visibility = View.INVISIBLE
            binding.labelsContentChipGroup.visibility = View.GONE
        }
    }

    private fun updateLabelFilterInViewModel() {
        viewModel.setLabelFilter(requireContext(), filterLabelsComponent.currentCheckedLabels, filterLabelsComponent.currentUncheckedLabels)
    }


    /**
     * Make the section appear enabled or disabled
     */
    private fun updateTypeFilterEnabled() {
        binding.typeEnabledButton.setImageDrawable(
            getFilterEnabledImage(viewModel.amountFilter.value!!.enabled)
        )
        if (viewModel.amountFilter.value!!.enabled) {
            binding.typeHeadline.isEnabled = true
            binding.typeChipGroup.visibility = View.VISIBLE
        }
        else {
            binding.typeHeadline.isEnabled = false
            binding.typeChipGroup.visibility = View.INVISIBLE
        }
    }


    /**
     * Make the section appear enabled or disabled
     */
    private fun updateDateFilterEnabled() {
        binding.dateEnabledButton.setImageDrawable(
            getFilterEnabledImage(viewModel.dateFilter.value!!.enabled)
        )
        if (viewModel.dateFilter.value!!.enabled) {
            binding.dateHeadline.isEnabled = true
            binding.dateFromTextView.visibility = View.VISIBLE
            binding.dateFromChip.visibility = View.VISIBLE
            binding.dateToTextView.visibility = View.VISIBLE
            binding.dateToChip.visibility = View.VISIBLE
        }
        else {
            binding.dateHeadline.isEnabled = false
            binding.dateFromTextView.visibility = View.INVISIBLE
            binding.dateFromChip.visibility = View.INVISIBLE
            binding.dateToTextView.visibility = View.GONE
            binding.dateToChip.visibility = View.GONE
        }
    }

    /**
     * Update amountFilter in viewModel according to the the type expense and income chips
     */
    private fun updateTypeFilter() {
        viewModel.setAmountFilter(requireContext(),
            expenses = binding.typeExpenseChip.isChecked,
            income = binding.typeIncomeChip.isChecked
        )
    }

    private fun getFilterEnabledImage(enabled: Boolean): Drawable? {
        return ResourcesCompat.getDrawable(resources,
            when (enabled) {
                true -> R.drawable.ic_filter_24dp
                else -> R.drawable.ic_filter_off_24dp
            },
            requireContext().theme
        )
    }

    private fun requestDeleteSingle(id: UUID) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_expense)
            .setMessage(R.string.this_cannot_be_reversed)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                deleteDirectly(id)
                afterDeletions()
            }
            .setNegativeButton(android.R.string.cancel) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.cancel()
            }
            .show()
    }

    private fun requestDeleteMultiple(ids: List<UUID>) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_x_expenses).format(ids.size))
            .setMessage(R.string.this_cannot_be_reversed)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                for (id in ids)
                    deleteDirectly(id)
                afterDeletions()
            }
            .setNegativeButton(android.R.string.cancel) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.cancel()
            }
            .show()
    }

    private fun deleteDirectly(id: UUID) {
        val pos = viewModel.deleteExpense(requireContext(), id, notifyChange = false)
        adapter?.notifyItemRemoved(pos)
    }

    private fun afterDeletions() {
        updateSum()
        updateFilterLabelsComponent()
    }

    private fun updateSum() {
        val settings = SettingsManager(requireContext())
        val amountFilter = viewModel.amountFilter.value!!
        val prefix =
            if (amountFilter.enabled && (!amountFilter.expenses || !amountFilter.income)) {
                if (amountFilter.expenses)
                    getString(R.string.expenses)
                else
                    getString(R.string.income_plural)
            } else {
                if (settings.isModeBudget)
                    getString(R.string.budget)
                else
                    getString(R.string.expenses)
            }

        val cents =
            if (!amountFilter.enabled || (amountFilter.expenses && amountFilter.income)) {
                if (settings.isModeBudget)
                    -viewModel.sumCents
                else
                    viewModel.sumCents
            }
            else
                viewModel.sumCents.absoluteValue


        val sum = formatCurrency(
            settings.currencySymbol,
            cents
        )


        activity?.supportActionBar?.title = "$prefix $sum"
    }

    private fun updateFilterLabelsComponent() {
        filterLabelsComponent.allLabels = ExpensePreferenceManager(requireContext()).getSortedLabelsDropdown()
    }

    private val adapter get() = binding.recyclerView.adapter as? ExpenseAdapter
    private val isFilterEdit get() = binding.filterCard.visibility == View.VISIBLE

}