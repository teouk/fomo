package io.github.teouk.fomo.ui.profile

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.teouk.fomo.R
import io.github.teouk.fomo.data.repository.PreferencesRepository
import io.github.teouk.fomo.databinding.FragmentProfileBinding
import io.github.teouk.fomo.model.City
import io.github.teouk.fomo.ui.auth.SignInActivity
import kotlinx.coroutines.launch

/** Profile: the account (name, email, sign out) and the user's settings. */
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels { ProfileViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutProfileName.setOnClickListener { showNameDialog() }
        binding.layoutHomeTown.setOnClickListener { showCityDialog() }
        binding.layoutReminderLead.setOnClickListener { showReminderDialog() }
        binding.buttonSignOut.setOnClickListener {
            viewModel.signOut()
            startActivity(SignInActivity.newIntent(requireContext()))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ProfileUiState) {
        binding.textProfileName.text = state.account?.displayName?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_name_not_set)
        binding.textProfileEmail.text = state.account?.let { getString(R.string.profile_signed_in_as, it.email) }
        binding.textHomeTown.text = state.homeCity.displayName
        binding.textReminderLead.text = reminderLabel(state.reminderLeadHours)
    }

    private fun reminderLabel(hours: Int): String = when (hours) {
        1 -> getString(R.string.reminder_one_hour)
        HOURS_IN_DAY -> getString(R.string.reminder_one_day)
        else -> getString(R.string.reminder_hours, hours)
    }

    private fun showNameDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(viewModel.uiState.value.account?.displayName)
            hint = getString(R.string.label_display_name)
        }
        val padding = resources.getDimensionPixelSize(R.dimen.dialog_input_padding)
        val container = FrameLayout(requireContext()).apply {
            setPadding(padding, 0, padding, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_display_name)
            .setView(container)
            .setPositiveButton(R.string.action_save) { _, _ -> viewModel.setDisplayName(input.text.toString()) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showCityDialog() {
        val cities = City.entries
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_home_town)
            .setSingleChoiceItems(
                cities.map { it.displayName }.toTypedArray(),
                cities.indexOf(viewModel.uiState.value.homeCity),
            ) { dialog, which ->
                viewModel.setHomeCity(cities[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showReminderDialog() {
        val options = PreferencesRepository.REMINDER_LEAD_OPTIONS
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_reminders)
            .setSingleChoiceItems(
                options.map(::reminderLabel).toTypedArray(),
                options.indexOf(viewModel.uiState.value.reminderLeadHours),
            ) { dialog, which ->
                viewModel.setReminderLeadHours(options[which])
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val HOURS_IN_DAY = 24
    }
}
