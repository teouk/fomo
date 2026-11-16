package io.github.teouk.fomo.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.teouk.fomo.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    // The binding holds references to the fragment's views. A fragment can outlive its
    // views (for example while another tab is shown), so the reference is cleared in
    // onDestroyView() to avoid leaking them.
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
