package io.github.teouk.fomo.ui.auth

/** Checks the form before anything is sent to Firebase. Pure Kotlin, so it is unit-tested. */
object CredentialsValidator {
    /** Firebase's own minimum. */
    const val MIN_PASSWORD_LENGTH = 6

    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidEmail(email: String): Boolean = emailPattern.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH
}
