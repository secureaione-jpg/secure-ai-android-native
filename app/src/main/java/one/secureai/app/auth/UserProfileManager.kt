package one.secureai.app.auth

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val userId: String,
    val userName: String = "",
    val username: String = "",
    val userInitials: String = "",
    val profileImageURL: String? = null
)

object UserProfileManager {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _isOnboarded = MutableStateFlow(true)
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

    suspend fun load() {
        val u = uid ?: return
        try {
            val doc = db.collection("shared_users").document(u).get().await()
            val data = doc.data
            if (data != null) {
                val name = data["userName"] as? String ?: ""
                _profile.value = UserProfile(
                    userId = u,
                    userName = name,
                    username = data["username"] as? String ?: "",
                    userInitials = name.take(1).uppercase(),
                    profileImageURL = data["profileImageURL"] as? String
                )
                _isOnboarded.value = name.isNotBlank()
            } else {
                _profile.value = UserProfile(userId = u)
                _isOnboarded.value = false
            }
        } catch (_: Exception) {
            _profile.value = UserProfile(userId = u)
        }
    }

    suspend fun saveName(name: String, username: String) {
        val u = uid ?: return
        val trimmedName = name.trim()
        val trimmedUsername = username.trim().lowercase()
        val data = hashMapOf<String, Any>(
            "userName" to trimmedName,
            "username" to trimmedUsername,
            "userInitials" to trimmedName.take(1).uppercase(),
            "updatedAt" to Timestamp.now()
        )
        try {
            db.collection("shared_users").document(u).set(data, SetOptions.merge()).await()
            if (trimmedUsername.isNotEmpty()) {
                db.collection("shared_usernames").document(trimmedUsername)
                    .set(hashMapOf("uid" to u), SetOptions.merge()).await()
            }
            _profile.value = _profile.value?.copy(
                userName = trimmedName,
                username = trimmedUsername,
                userInitials = trimmedName.take(1).uppercase()
            ) ?: UserProfile(u, trimmedName, trimmedUsername, trimmedName.take(1).uppercase())
            _isOnboarded.value = true
        } catch (_: Exception) {}
    }

    suspend fun uploadProfileImage(imageBytes: ByteArray) {
        val u = uid ?: return
        try {
            val ref = storage.reference.child("shared_profileImages/$u.jpg")
            ref.putBytes(imageBytes).await()
            val url = ref.downloadUrl.await().toString()
            db.collection("shared_users").document(u)
                .update("profileImageURL", url).await()
            _profile.value = _profile.value?.copy(profileImageURL = url)
        } catch (_: Exception) {}
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        if (username.isBlank()) return false
        return try {
            val doc = db.collection("shared_usernames").document(username.lowercase()).get().await()
            !doc.exists() || doc.getString("uid") == uid
        } catch (_: Exception) { false }
    }

    suspend fun deleteAppData() {
        val u = uid ?: return
        try {
            val secureAiRef = db.collection("secure_ai").document(u)
            val convRef = db.collection("conversations").document(u)
            secureAiRef.delete().await()
            convRef.delete().await()
        } catch (_: Exception) {}
    }

    suspend fun deleteAccount() {
        val u = uid ?: return
        try {
            deleteAppData()
            db.collection("shared_users").document(u).delete().await()
            val username = _profile.value?.username
            if (!username.isNullOrBlank()) {
                db.collection("shared_usernames").document(username).delete().await()
            }
            FirebaseAuth.getInstance().currentUser?.delete()?.await()
        } catch (_: Exception) {}
        _profile.value = null
    }

    fun reset() {
        _profile.value = null
        _isOnboarded.value = true
    }
}
