package io.github.teouk.fomo.ui.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialsValidatorTest {
    @Test
    fun isValidEmail_normalAddress_isValid() {
        assertTrue(CredentialsValidator.isValidEmail("student@swansea.ac.uk"))
    }

    @Test
    fun isValidEmail_surroundingSpaces_isValid() {
        assertTrue(CredentialsValidator.isValidEmail("  student@swansea.ac.uk "))
    }

    @Test
    fun isValidEmail_missingAtOrDomain_isInvalid() {
        assertFalse(CredentialsValidator.isValidEmail("student.swansea.ac.uk"))
        assertFalse(CredentialsValidator.isValidEmail("student@swansea"))
        assertFalse(CredentialsValidator.isValidEmail(""))
    }

    @Test
    fun isValidPassword_belowMinimum_isInvalid() {
        assertFalse(CredentialsValidator.isValidPassword("12345"))
        assertTrue(CredentialsValidator.isValidPassword("123456"))
    }
}
