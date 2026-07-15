package one.secureai.app.data.store

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val projectId: String? = null,
    val updatedAt: Date = Date(),
    val createdAt: Date = Date()
) {
    val preview: String
        get() {
            val text = body.trim()
            return if (text.isEmpty()) "No content" else text.take(80)
        }

    fun toMap(): Map<String, Any?> = buildMap {
        put("title", title)
        put("body", body)
        put("updatedAt", Timestamp.now())
        put("createdAt", Timestamp(createdAt))
        if (projectId != null) put("projectId", projectId)
    }

    companion object {
        fun fromDoc(id: String, data: Map<String, Any?>): Note = Note(
            id = id,
            title = data["title"] as? String ?: "",
            body = data["body"] as? String ?: "",
            projectId = data["projectId"] as? String,
            updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date(),
            createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date()
        )
    }
}

object NoteStore {
    private val db = FirebaseFirestore.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    // Persistence writes are launched here rather than on a caller-supplied
    // scope — a note is often saved in the same action that navigates away
    // (e.g. tapping Done), which would cancel a Compose-scoped coroutine
    // before the Firestore write completes.
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun col(uid: String) = db.collection("secure_ai").document(uid).collection("notes")

    fun reset() { _notes.value = emptyList() }

    suspend fun load() {
        val u = uid ?: return
        _isLoading.value = true
        try {
            val snap = col(u).orderBy("updatedAt", Query.Direction.DESCENDING).get().await()
            _notes.value = snap.documents.map { Note.fromDoc(it.id, it.data ?: emptyMap()) }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    fun save(note: Note) {
        val u = uid ?: return
        val updated = note.copy(updatedAt = Date())
        _notes.value = if (_notes.value.any { it.id == updated.id }) {
            _notes.value.map { if (it.id == updated.id) updated else it }
        } else {
            listOf(updated) + _notes.value
        }
        storeScope.launch {
            try {
                col(u).document(updated.id).set(updated.toMap()).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun delete(note: Note) {
        _notes.value = _notes.value.filter { it.id != note.id }
        val u = uid ?: return
        col(u).document(note.id).delete()
    }
}
