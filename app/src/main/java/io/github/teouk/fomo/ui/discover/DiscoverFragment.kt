package io.github.teouk.fomo.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.teouk.fomo.databinding.FragmentDiscoverBinding
import io.github.teouk.fomo.ui.search.SearchActivity

class DiscoverFragment : Fragment() {
    // The binding holds references to the fragment's views. A fragment can outlive its
    // views (for example while another tab is shown), so the reference is cleared in
    // onDestroyView() to avoid leaking them.
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

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
        // The search bar on Discover is only a button: typing happens on the Search screen.
        binding.searchBar.setOnClickListener {
            startActivity(SearchActivity.newIntent(requireContext()))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
