package one.secureai.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object AuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    val isSignedIn: Boolean get() = _user.value != null
    val uid: String? get() = _user.value?.uid
    val isAnonymous: Boolean get() = _user.value?.isAnonymous ?: true

    init {
        auth.addAuthStateListener { fa ->
            _user.value = fa.currentUser
            _isLoading.value = false
        }
    }

    fun clearError() { _authError.value = null }

    suspend fun signInAnonymouslyIfNeeded() {
        if (auth.currentUser != null) {
            _user.value = auth.currentUser
            return
        }
        try {
            val result = auth.signInAnonymously().await()
            _user.value = result.user
        } catch (e: Exception) {
            _authError.value = "Guest sign-in failed: ${e.localizedMessage}"
        }
    }

    suspend fun signInWithGoogle(context: Context) {
        val webClientId = getWebClientId(context) ?: run {
            _authError.value = "Google Sign-In not configured."
            return
        }

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)

            val googleIdToken = GoogleIdTokenCredential
                .createFrom(result.credential.data)
                .idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

            val current = auth.currentUser
            val authResult = if (current != null && current.isAnonymous) {
                try {
                    current.linkWithCredential(firebaseCredential).await()
                } catch (_: Exception) {
                    auth.signInWithCredential(firebaseCredential).await()
                }
            } else {
                auth.signInWithCredential(firebaseCredential).await()
            }

            _user.value = authResult.user
            _authError.value = null
            UserProfileManager.load()
        } catch (e: Exception) {
            if (e.message?.contains("canceled", ignoreCase = true) != true &&
                e.message?.contains("cancelled", ignoreCase = true) != true) {
                _authError.value = "Google sign-in failed. Please try again."
            }
        }
    }

    fun signOut() {
        try {
            auth.signOut()
            _user.value = null
        } catch (e: Exception) {
            _authError.value = "Sign-out failed: ${e.localizedMessage}"
        }
    }

    private fun getWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )
        return if (resId != 0) context.getString(resId) else null
    }
}
