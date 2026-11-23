package io.github.teouk.fomo.ui.plans

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
import com.google.android.material.tabs.TabLayout
import io.github.teouk.fomo.R
import io.github.teouk.fomo.databinding.FragmentPlansBinding
import io.github.teouk.fomo.ui.details.EventDetailsActivity
import kotlinx.coroutines.launch

/** My Plans: the Schedule (planned events by day) and Saved (bookmarked events) lists. */
class PlansFragment : Fragment() {
    private var _binding: FragmentPlansBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlansViewModel by viewModels { PlansViewModel.Factory }

    private val adapter = PlansAdapter(
        onEventClick = { startActivity(EventDetailsActivity.newIntent(requireContext(), it.id)) },
        onSaveClick = { viewModel.toggleSave(it) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerPlans.adapter = adapter
        binding.tabsPlans.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) = viewModel.selectTab(PlansTab.entries[tab.position])

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            },
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: PlansUiState) {
        val tabIndex = state.tab.ordinal
        if (binding.tabsPlans.selectedTabPosition != tabIndex) binding.tabsPlans.getTabAt(tabIndex)?.select()
        adapter.submitList(state.rows)
        binding.textPlansEmpty.isVisible = state.isEmpty
        binding.textPlansEmpty.setText(
            if (state.tab == PlansTab.SCHEDULE) R.string.plans_empty_schedule else R.string.plans_empty_saved,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
