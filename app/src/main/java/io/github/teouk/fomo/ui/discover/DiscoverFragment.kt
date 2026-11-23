package io.github.teouk.fomo.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.teouk.fomo.R
import io.github.teouk.fomo.databinding.FragmentDiscoverBinding
import io.github.teouk.fomo.model.Category
import io.github.teouk.fomo.model.City
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.ui.common.EventRowAdapter
import io.github.teouk.fomo.ui.common.FeaturedEventAdapter
import io.github.teouk.fomo.ui.common.messageRes
import io.github.teouk.fomo.ui.details.EventDetailsActivity
import io.github.teouk.fomo.ui.search.SearchActivity
import kotlinx.coroutines.launch

class DiscoverFragment : Fragment() {
    // The binding holds references to the fragment's views. A fragment can outlive its
    // views (for example while another tab is shown), so the reference is cleared in
    // onDestroyView() to avoid leaking them.
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiscoverViewModel by viewModels { DiscoverViewModel.Factory }

    private val chipCategories by lazy {
        mapOf(
            R.id.chip_all to null,
            R.id.chip_music to Category.MUSIC,
            R.id.chip_sport to Category.SPORT,
            R.id.chip_arts_theatre to Category.ARTS_THEATRE,
            R.id.chip_comedy to Category.COMEDY,
            R.id.chip_family to Category.FAMILY,
            R.id.chip_film to Category.FILM,
        )
    }

    private val weekendAdapter = FeaturedEventAdapter(::openDetails) { viewModel.toggleSave(it) }
    private val nearYouAdapter = EventRowAdapter(::openDetails) { viewModel.toggleSave(it) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerThisWeekend.adapter = weekendAdapter
        binding.recyclerNearYou.adapter = nearYouAdapter

        // The search bar on Discover is only a button: typing happens on the Search screen.
        binding.searchBar.setOnClickListener {
            startActivity(SearchActivity.newIntent(requireContext()))
        }
        binding.searchBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_scan_poster) {
                startActivity(SearchActivity.newIntent(requireContext(), scanPoster = true))
                true
            } else {
                false
            }
        }
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.selectCategory(checkedIds.firstOrNull()?.let { chipCategories[it] })
        }
        binding.textLocation.setOnClickListener { showCityPicker() }
        binding.buttonRetry.setOnClickListener { viewModel.retry() }

        // Collect state only while the screen is visible (STARTED). Using
        // viewLifecycleOwner stops collection when the views are destroyed.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: DiscoverUiState) {
        binding.textLocation.text = getString(R.string.location_events_near, state.city.displayName)
        binding.progressDiscover.isVisible = state.isLoading
        binding.textSampleNotice.isVisible = state.usesSampleData

        val message = when {
            state.error != null -> getString(state.error.messageRes())
            state.isEmpty -> getString(R.string.discover_empty, state.city.displayName)
            else -> null
        }
        binding.layoutDiscoverMessage.isVisible = message != null
        binding.textDiscoverMessage.text = message
        binding.buttonRetry.isVisible = state.error != null

        binding.textSectionThisWeekend.isVisible = state.thisWeekend.isNotEmpty()
        binding.recyclerThisWeekend.isVisible = state.thisWeekend.isNotEmpty()
        binding.textSectionNearYou.isVisible = state.nearYou.isNotEmpty()
        binding.recyclerNearYou.isVisible = state.nearYou.isNotEmpty()
        weekendAdapter.submitList(state.thisWeekend)
        nearYouAdapter.submitList(state.nearYou)
    }

    private fun showCityPicker() {
        val cities = City.entries
        val current = cities.indexOf(viewModel.uiState.value.city)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.location_choose_title)
            .setSingleChoiceItems(cities.map { it.displayName }.toTypedArray(), current) { dialog, which ->
                viewModel.selectCity(cities[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun openDetails(event: Event) {
        startActivity(EventDetailsActivity.newIntent(requireContext(), event.id))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
