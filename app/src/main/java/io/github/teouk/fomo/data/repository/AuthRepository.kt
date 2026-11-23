package io.github.teouk.fomo.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** The signed-in user, as the app sees it. */
data class Account(val email: String, val displayName: String?)

enum class AuthError { WRONG_CREDENTIALS, ACCOUNT_EXISTS, OFFLINE, OTHER }

/**
 * EXTERNAL SERVICE: Firebase Authentication (email + password).
 * Docs: https://firebase.google.com/docs/auth/android/password-auth
 *
 * Firebase stores and checks passwords securely on Google's servers; the app never
 * stores a password itself. Firebase also remembers the session, so users stay
 * signed in after closing the app.
 */
class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val currentAccount: Account? get() = auth.currentUser?.let { Account(it.email.orEmpty(), it.displayName) }

    /** Emits the account whenever someone signs in or out. */
    val account: Flow<Account?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(currentAccount) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): AuthError? =
        attempt { auth.signInWithEmailAndPassword(email, password).await() }

    suspend fun createAccount(email: String, password: String): AuthError? =
        attempt { auth.createUserWithEmailAndPassword(email, password).await() }

    suspend fun updateDisplayName(name: String): AuthError? = attempt {
        val user = checkNotNull(auth.currentUser)
        user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        user.reload().await()
    }

    fun signOut() = auth.signOut()

    /** Runs a Firebase call and turns its exceptions into an [AuthError], or null on success. */
    private suspend fun attempt(block: suspend () -> Unit): AuthError? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        AuthError.WRONG_CREDENTIALS
    } catch (e: FirebaseAuthInvalidUserException) {
        AuthError.WRONG_CREDENTIALS
    } catch (e: FirebaseAuthUserCollisionException) {
        AuthError.ACCOUNT_EXISTS
    } catch (e: FirebaseNetworkException) {
        AuthError.OFFLINE
    } catch (e: Exception) {
        AuthError.OTHER
    }
}
