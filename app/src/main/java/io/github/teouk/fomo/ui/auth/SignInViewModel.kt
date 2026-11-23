package io.github.teouk.fomo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.teouk.fomo.FomoApplication
import io.github.teouk.fomo.data.repository.AuthError
import io.github.teouk.fomo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val isLoading: Boolean = false,
    val emailInvalid: Boolean = false,
    val passwordInvalid: Boolean = false,
    val error: AuthError? = null,
    val isSignedIn: Boolean = false,
)

class SignInViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState(isSignedIn = authRepository.currentAccount != null))
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) = submit(email, password) { e, p -> authRepository.signIn(e, p) }

    fun createAccount(email: String, password: String) =
        submit(email, password) { e, p -> authRepository.createAccount(e, p) }

    private fun submit(email: String, password: String, action: suspend (String, String) -> AuthError?) {
        val emailInvalid = !CredentialsValidator.isValidEmail(email)
        val passwordInvalid = !CredentialsValidator.isValidPassword(password)
        _uiState.update { it.copy(emailInvalid = emailInvalid, passwordInvalid = passwordInvalid, error = null) }
        if (emailInvalid || passwordInvalid || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val error = action(email.trim(), password)
            _uiState.update { it.copy(isLoading = false, error = error, isSignedIn = error == null) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SignInViewModel((this[APPLICATION_KEY] as FomoApplication).container.authRepository)
            }
        }
    }
}
