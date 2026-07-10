package one.secureai.app.data.store

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

data class StoredConversation(
    val id: String,
    val title: String,
    val lastMessage: String = "",
    val favorited: Boolean = false,
    val updatedAt: Date = Date()
) {
    companion object {
        fun fromDoc(id: String, data: Map<String, Any?>): StoredConversation? {
            val title = data["title"] as? String ?: return null
            return StoredConversation(
                id = id,
                title = title,
                lastMessage = data["lastMessage"] as? String ?: "",
                favorited = data["favorited"] as? Boolean ?: false,
                updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
            )
        }
    }
}

object ConversationStore {
    private val db = FirebaseFirestore.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _conversations = MutableStateFlow<List<StoredConversation>>(emptyList())
    val conversations: StateFlow<List<StoredConversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun col(uid: String) =
        db.collection("secure_ai").document(uid).collection("conversations")

    suspend fun load() {
        val u = uid ?: return
        _isLoading.value = true
        try {
            val snap = col(u)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            _conversations.value = snap.documents.mapNotNull {
                StoredConversation.fromDoc(it.id, it.data ?: emptyMap())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun deleteConversation(id: String) {
        val u = uid ?: return
        _conversations.value = _conversations.value.filter { it.id != id }
        try { col(u).document(id).delete().await() } catch (_: Exception) {}
    }

    suspend fun toggleFavorite(id: String) {
        val u = uid ?: return
        _conversations.value = _conversations.value.map {
            if (it.id == id) it.copy(favorited = !it.favorited) else it
        }
        val newValue = _conversations.value.find { it.id == id }?.favorited ?: return
        try { col(u).document(id).update("favorited", newValue).await() } catch (_: Exception) {}
    }

    suspend fun renameConversation(id: String, title: String) {
        val u = uid ?: return
        _conversations.value = _conversations.value.map {
            if (it.id == id) it.copy(title = title) else it
        }
        try { col(u).document(id).update("title", title).await() } catch (_: Exception) {}
    }

    fun reset() { _conversations.value = emptyList() }
}
