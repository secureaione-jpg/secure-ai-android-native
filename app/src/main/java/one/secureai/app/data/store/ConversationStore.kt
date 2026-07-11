package one.secureai.app.data.store

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import one.secureai.app.chat.ChatMessage
import one.secureai.app.chat.ChatRole
import java.util.Date
import java.util.UUID

data class StoredConversation(
    val id: String,
    val title: String,
    val lastMessage: String = "",
    val favorited: Boolean = false,
    val messageCount: Int = 0,
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
                messageCount = (data["messageCount"] as? Long)?.toInt() ?: 0,
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

    private fun chatsCol(uid: String) =
        db.collection("conversations").document(uid).collection("chats")

    suspend fun load() {
        val u = uid ?: return
        _isLoading.value = true
        try {
            val snap = chatsCol(u)
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

    suspend fun saveConversation(
        conversationId: String?,
        title: String,
        lastMessage: String,
        newMessages: List<ChatMessage>,
        messageCount: Int
    ): String {
        val u = uid ?: return conversationId ?: UUID.randomUUID().toString()
        val id = conversationId ?: UUID.randomUUID().toString()

        try {
            val docRef = chatsCol(u).document(id)
            val meta = hashMapOf<String, Any>(
                "title" to title,
                "lastMessage" to lastMessage,
                "messageCount" to messageCount,
                "updatedAt" to Timestamp.now()
            )
            docRef.set(meta, SetOptions.merge()).await()

            val msgCol = docRef.collection("messages")
            val batch = db.batch()
            for (msg in newMessages) {
                val encryptedContent = withContext(Dispatchers.Default) {
                    ConversationEncryption.encrypt(msg.content, u)
                }
                val msgData = hashMapOf<String, Any>(
                    "role" to msg.role.wireValue,
                    "content" to encryptedContent,
                    "encrypted" to true,
                    "timestamp" to Timestamp.now()
                )
                msg.model?.let { msgData["model"] = it }
                msg.exchangeId?.let { msgData["exchangeId"] = it }
                batch.set(msgCol.document(msg.id), msgData)
            }
            batch.commit().await()

            val conv = StoredConversation(
                id = id, title = title, lastMessage = lastMessage,
                messageCount = messageCount, updatedAt = Date()
            )
            _conversations.value = listOf(conv) +
                _conversations.value.filter { it.id != id }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    suspend fun loadMessages(conversationId: String): List<ChatMessage> {
        val u = uid ?: return emptyList()
        return try {
            val snap = chatsCol(u).document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            withContext(Dispatchers.Default) {
                snap.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val roleStr = data["role"] as? String ?: return@mapNotNull null
                    val role = ChatRole.entries.find { it.wireValue == roleStr } ?: return@mapNotNull null
                    val rawContent = data["content"] as? String ?: ""
                    val isEncrypted = data["encrypted"] as? Boolean ?: false
                    val content = if (isEncrypted) {
                        try { ConversationEncryption.decrypt(rawContent, u) } catch (_: Exception) { rawContent }
                    } else rawContent
                    ChatMessage(
                        id = doc.id,
                        role = role,
                        content = content,
                        model = data["model"] as? String,
                        exchangeId = data["exchangeId"] as? String
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun deleteConversation(id: String) {
        val u = uid ?: return
        _conversations.value = _conversations.value.filter { it.id != id }
        try { chatsCol(u).document(id).delete().await() } catch (_: Exception) {}
    }

    suspend fun toggleFavorite(id: String) {
        val u = uid ?: return
        _conversations.value = _conversations.value.map {
            if (it.id == id) it.copy(favorited = !it.favorited) else it
        }
        val newValue = _conversations.value.find { it.id == id }?.favorited ?: return
        try { chatsCol(u).document(id).update("favorited", newValue).await() } catch (_: Exception) {}
    }

    suspend fun renameConversation(id: String, title: String) {
        val u = uid ?: return
        _conversations.value = _conversations.value.map {
            if (it.id == id) it.copy(title = title) else it
        }
        try { chatsCol(u).document(id).update("title", title).await() } catch (_: Exception) {}
    }

    fun reset() {
        _conversations.value = emptyList()
    }
}
