package one.secureai.app.data.store

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

data class UserPrompt(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "",
    val createdAt: Date = Date()
) {
    companion object {
        fun fromDoc(id: String, data: Map<String, Any?>): UserPrompt? {
            val title = data["title"] as? String ?: return null
            return UserPrompt(
                id = id,
                title = title,
                content = data["content"] as? String ?: "",
                category = data["category"] as? String ?: "",
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date()
            )
        }
    }
}

object PromptStore {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("prompt_library")

    private val _prompts = MutableStateFlow<List<UserPrompt>>(emptyList())
    val prompts: StateFlow<List<UserPrompt>> = _prompts.asStateFlow()

    suspend fun load() {
        try {
            val snap = col.orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            _prompts.value = snap.documents.mapNotNull { UserPrompt.fromDoc(it.id, it.data ?: emptyMap()) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun add(title: String, content: String, category: String = "") {
        val prompt = UserPrompt(title = title.trim(), content = content.trim(), category = category.trim())
        _prompts.value = listOf(prompt) + _prompts.value
        col.document(prompt.id).set(
            hashMapOf(
                "title" to prompt.title,
                "content" to prompt.content,
                "category" to prompt.category,
                "createdAt" to Timestamp.now()
            )
        )
    }

    fun delete(prompt: UserPrompt) {
        _prompts.value = _prompts.value.filter { it.id != prompt.id }
        col.document(prompt.id).delete()
    }

    fun reset() { _prompts.value = emptyList() }
}
