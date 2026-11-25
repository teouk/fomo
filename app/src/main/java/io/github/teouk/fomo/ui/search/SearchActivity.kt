package io.github.teouk.fomo.ui.search

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
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
import java.io.File
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

    /** The lines currently shown as poster chips, so chips are only rebuilt when they change. */
    private var shownPosterLines: List<String>? = null

    /**
     * Poster Scanner (bonus feature). TakePicture hands the photo to the phone's camera
     * app, so FOMO needs no CAMERA permission (and must not declare one, or the camera
     * Intent would require it). The photo goes to a FileProvider URI in FOMO's cache.
     */
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = viewModel.pendingPhotoUri
        if (saved && uri != null) viewModel.readPoster(uri)
        viewModel.pendingPhotoUri = null
    }

    /** The system Photo Picker: no storage permission needed, and only the chosen photo is shared. */
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.readPoster(it.toString()) }
    }

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
        binding.toolbarSearch.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_scan_poster) showPosterSourceDialog()
            item.itemId == R.id.action_scan_poster
        }
        binding.recyclerResults.adapter = adapter

        restoreKeyword()
        setUpListeners()
        // Opened from Discover's camera button: go straight to the scanner (once, not after rotation).
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_SCAN_POSTER, false)) {
            showPosterSourceDialog()
        }

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
        binding.chipOnlyFree.setOnCheckedChangeListener { _, checked -> viewModel.setOnlyWhenFree(checked) }
        binding.sliderDistance.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.setRadius(value.toInt())
        }
        binding.textDistance.setOnClickListener { showCityPicker() }
        binding.buttonClearAll.setOnClickListener { clearAll() }
        binding.buttonEmptyClearAll.setOnClickListener { clearAll() }
    }

    private fun render(state: SearchUiState) {
        renderPoster(state.poster)
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
        // "Only when I'm free" is part of Calendar Clash Check, so it only shows when that is on.
        binding.chipOnlyFree.isVisible = state.clashCheckEnabled
        if (binding.chipOnlyFree.isChecked != filters.onlyWhenFree) binding.chipOnlyFree.isChecked = filters.onlyWhenFree
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

    private fun renderPoster(poster: PosterState) {
        binding.layoutPoster.isVisible = poster != PosterState.Hidden
        binding.progressPoster.isVisible = poster == PosterState.Reading
        binding.textPosterStatus.setText(
            when (poster) {
                PosterState.Reading -> R.string.poster_reading
                PosterState.NoText -> R.string.poster_no_text
                PosterState.Failed -> R.string.poster_error
                else -> R.string.poster_hint
            },
        )
        val lines = (poster as? PosterState.Lines)?.lines.orEmpty()
        if (lines == shownPosterLines) return
        shownPosterLines = lines
        binding.chipGroupPosterLines.removeAllViews()
        lines.forEach { line ->
            val chip = Chip(this).apply {
                text = line
                setOnClickListener { searchFor(line) }
            }
            binding.chipGroupPosterLines.addView(chip)
        }
    }

    /** A tapped poster line becomes the keyword; the text field's listener does the rest. */
    private fun searchFor(line: String) {
        binding.editSearch.setText(line)
        binding.editSearch.setSelection(line.length)
    }

    private fun showPosterSourceDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_scan_poster)
            .setItems(arrayOf(getString(R.string.poster_take_photo), getString(R.string.poster_choose_photo))) { _, which ->
                if (which == 0) openCamera() else openPhotoPicker()
            }
            .show()
    }

    private fun openCamera() {
        // Only the latest poster is kept; older ones are deleted before taking a new one.
        val folder = File(cacheDir, POSTER_FOLDER).apply {
            deleteRecursively()
            mkdirs()
        }
        val photo = File(folder, "poster_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName$FILE_PROVIDER_SUFFIX", photo)
        viewModel.pendingPhotoUri = uri.toString()
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            viewModel.pendingPhotoUri = null
            Toast.makeText(this, R.string.error_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPhotoPicker() {
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
        private const val POSTER_FOLDER = "posters"
        private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

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
