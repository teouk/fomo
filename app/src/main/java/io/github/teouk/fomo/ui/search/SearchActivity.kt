package io.github.teouk.fomo.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.teouk.fomo.R
import io.github.teouk.fomo.databinding.ActivitySearchBinding
import io.github.teouk.fomo.model.Category
import io.github.teouk.fomo.model.City
import io.github.teouk.fomo.ui.common.EventRowAdapter
import io.github.teouk.fomo.ui.common.messageRes
import io.github.teouk.fomo.ui.details.EventDetailsActivity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Search & Filter screen. Opened from the search bar on Discover. */
class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels { SearchViewModel.Factory }

    private val adapter = EventRowAdapter(
        onEventClick = { startActivity(EventDetailsActivity.newIntent(this, it.id)) },
        onSaveClick = { viewModel.toggleSave(it) },
    )

    private val chipCategories by lazy {
        mapOf(
            binding.chipMusic to Category.MUSIC,
            binding.chipSport to Category.SPORT,
            binding.chipArtsTheatre to Category.ARTS_THEATRE,
            binding.chipComedy to Category.COMEDY,
            binding.chipFamily to Category.FAMILY,
            binding.chipFilm to Category.FILM,
        )
    }

    private val dateButtons by lazy {
        mapOf(
            R.id.button_date_today to DatePreset.TODAY,
            R.id.button_date_weekend to DatePreset.WEEKEND,
            R.id.button_date_7_days to DatePreset.NEXT_7_DAYS,
            R.id.button_date_pick to DatePreset.CUSTOM,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.toolbarSearch.setNavigationOnClickListener { finish() }
        binding.recyclerResults.adapter = adapter
        // "Only when I'm free" belongs to the Calendar Clash Check bonus feature.
        binding.chipOnlyFree.isVisible = false

        restoreKeyword()
        setUpListeners()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun restoreKeyword() {
        val keyword = viewModel.uiState.value.filters.keyword
        if (keyword.isNotEmpty()) binding.editSearch.setText(keyword)
        if (keyword.isEmpty()) binding.editSearch.requestFocus()
    }

    private fun setUpListeners() {
        binding.editSearch.doAfterTextChanged { viewModel.setKeyword(it?.toString().orEmpty()) }
        binding.editSearch.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(view.windowToken, 0)
                true
            } else {
                false
            }
        }
        chipCategories.forEach { (chip, category) ->
            chip.setOnCheckedChangeListener { _, checked -> viewModel.setCategoryChecked(category, checked) }
        }
        binding.toggleGroupDates.addOnButtonCheckedListener { group, checkedId, isChecked ->
            when {
                isChecked && checkedId != R.id.button_date_pick -> viewModel.setDatePreset(dateButtons.getValue(checkedId))
                !isChecked && group.checkedButtonId == android.view.View.NO_ID -> viewModel.setDatePreset(DatePreset.ANY)
            }
        }
        // "Dates" always opens the picker, even when it is already selected.
        binding.buttonDatePick.setOnClickListener { showDateRangePicker() }
        binding.sliderDistance.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.setRadius(value.toInt())
        }
        binding.textDistance.setOnClickListener { showCityPicker() }
        binding.buttonClearAll.setOnClickListener { clearAll() }
        binding.buttonEmptyClearAll.setOnClickListener { clearAll() }
    }

    private fun render(state: SearchUiState) {
        val filters = state.filters
        // Only touch a control when it differs from the state, so redrawing never
        // re-triggers its listener or moves the text cursor.
        chipCategories.forEach { (chip, category) ->
            val checked = category in filters.categories
            if (chip.isChecked != checked) chip.isChecked = checked
        }
        val dateButtonId = dateButtons.entries.firstOrNull { it.value == filters.datePreset }?.key
        if (dateButtonId == null) {
            if (binding.toggleGroupDates.checkedButtonId != android.view.View.NO_ID) binding.toggleGroupDates.clearChecked()
        } else if (binding.toggleGroupDates.checkedButtonId != dateButtonId) {
            binding.toggleGroupDates.check(dateButtonId)
        }
        val radius = filters.radiusMiles.toFloat()
        if (binding.sliderDistance.value != radius) binding.sliderDistance.value = radius
        binding.textDistance.text = getString(R.string.search_distance, filters.radiusMiles, state.city.displayName)

        binding.progressSearch.isVisible = state.isLoading
        binding.textResultCount.isVisible = !state.isLoading && state.error == null
        binding.textResultCount.text =
            resources.getQuantityString(R.plurals.search_result_count, state.results.size, state.results.size)
        binding.buttonClearAll.isEnabled = !filters.isDefault

        adapter.submitList(state.results)
        binding.recyclerResults.isVisible = state.results.isNotEmpty()

        val showMessage = state.error != null || state.isEmpty
        binding.layoutEmpty.isVisible = showMessage
        if (state.error != null) {
            binding.textEmptyTitle.setText(R.string.search_error_title)
            binding.textEmptyBody.setText(state.error.messageRes())
            binding.buttonEmptyClearAll.setText(R.string.action_retry)
            binding.buttonEmptyClearAll.setOnClickListener { viewModel.retry() }
        } else {
            binding.textEmptyTitle.setText(R.string.empty_results_title)
            binding.textEmptyBody.setText(R.string.empty_results_body)
            binding.buttonEmptyClearAll.setText(R.string.action_clear_all)
            binding.buttonEmptyClearAll.setOnClickListener { clearAll() }
        }
    }

    private fun clearAll() {
        binding.editSearch.text?.clear()
        viewModel.clearAll()
    }

    private fun showDateRangePicker() {
        val filters = viewModel.uiState.value.filters
        val constraints = CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build()
        val builder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.date_range_title)
            .setCalendarConstraints(constraints)
        if (filters.customStart != null && filters.customEnd != null) {
            builder.setSelection(Pair(filters.customStart.toUtcMillis(), filters.customEnd.toUtcMillis()))
        }
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first
            val end = selection.second
            if (start != null && end != null) viewModel.setCustomRange(start.toUtcDate(), end.toUtcDate())
        }
        // Cancelling without an earlier range leaves "Dates" unselected again.
        picker.addOnDismissListener { render(viewModel.uiState.value) }
        picker.show(supportFragmentManager, DATE_PICKER_TAG)
    }

    private fun showCityPicker() {
        val cities = City.entries
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.location_choose_title)
            .setSingleChoiceItems(
                cities.map { it.displayName }.toTypedArray(),
                cities.indexOf(viewModel.uiState.value.city),
            ) { dialog, which ->
                viewModel.setCity(cities[which])
                dialog.dismiss()
            }
            .show()
    }

    companion object {
        private const val EXTRA_SCAN_POSTER = "io.github.teouk.fomo.extra.SCAN_POSTER"
        private const val DATE_PICKER_TAG = "date_range_picker"

        /**
         * Builds the Intent that opens this screen, so callers never construct it by hand.
         * @param scanPoster open the camera straight away (Poster Scanner).
         */
        fun newIntent(context: Context, scanPoster: Boolean = false): Intent =
            Intent(context, SearchActivity::class.java).putExtra(EXTRA_SCAN_POSTER, scanPoster)
    }
}

/** The Material date picker works in UTC milliseconds at midnight. */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
