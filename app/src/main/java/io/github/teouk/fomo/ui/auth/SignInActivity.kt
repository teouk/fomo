package io.github.teouk.fomo.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.teouk.fomo.MainActivity
import io.github.teouk.fomo.R
import io.github.teouk.fomo.data.repository.AuthError
import io.github.teouk.fomo.databinding.ActivitySignInBinding
import kotlinx.coroutines.launch

/** Sign in or create an account with email and password (Firebase Authentication). */
class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private val viewModel: SignInViewModel by viewModels { SignInViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.buttonSignIn.setOnClickListener { viewModel.signIn(email(), password()) }
        binding.buttonCreateAccount.setOnClickListener { viewModel.createAccount(email(), password()) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun email() = binding.editEmail.text?.toString().orEmpty()

    private fun password() = binding.editPassword.text?.toString().orEmpty()

    private fun render(state: SignInUiState) {
        if (state.isSignedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        binding.progressSignIn.isVisible = state.isLoading
        binding.buttonSignIn.isEnabled = !state.isLoading
        binding.buttonCreateAccount.isEnabled = !state.isLoading
        binding.inputEmail.error = if (state.emailInvalid) getString(R.string.error_email_invalid) else null
        binding.inputPassword.error = if (state.passwordInvalid) {
            resources.getQuantityString(
                R.plurals.error_password_short,
                CredentialsValidator.MIN_PASSWORD_LENGTH,
                CredentialsValidator.MIN_PASSWORD_LENGTH,
            )
        } else {
            null
        }
        binding.textSignInError.isVisible = state.error != null
        binding.textSignInError.text = when (state.error) {
            AuthError.WRONG_CREDENTIALS -> getString(R.string.error_wrong_credentials)
            AuthError.ACCOUNT_EXISTS -> getString(R.string.error_account_exists)
            AuthError.OFFLINE -> getString(R.string.error_auth_offline)
            AuthError.OTHER -> getString(R.string.error_auth_generic)
            null -> null
        }
    }

    companion object {
        /** Opens sign-in as a fresh task, so Back cannot return to the signed-out app. */
        fun newIntent(context: Context): Intent =
            Intent(context, SignInActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
