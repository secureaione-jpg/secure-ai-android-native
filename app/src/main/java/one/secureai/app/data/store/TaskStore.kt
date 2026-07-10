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
import java.util.UUID

enum class TaskRecurrence(val wireValue: String, val label: String) {
    NONE("none", "One-time"),
    DAILY("daily", "Daily"),
    WEEKLY("weekly", "Weekly"),
    CUSTOM("custom", "Custom");

    companion object {
        fun fromWire(value: String?) = entries.find { it.wireValue == value } ?: NONE
    }
}

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val recurrence: TaskRecurrence = TaskRecurrence.NONE,
    val done: Boolean = false,
    val reminderAt: Date? = null,
    val createdFromMessageId: String? = null,
    val createdAt: Date = Date()
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("title", title)
        put("recurrence", recurrence.wireValue)
        put("done", done)
        put("createdAt", Timestamp(createdAt))
        reminderAt?.let { put("reminderAt", Timestamp(it)) }
        createdFromMessageId?.let { put("createdFromMessageId", it) }
    }

    companion object {
        fun fromDoc(id: String, data: Map<String, Any?>): TaskItem? {
            val title = data["title"] as? String ?: return null
            return TaskItem(
                id = id,
                title = title,
                recurrence = TaskRecurrence.fromWire(data["recurrence"] as? String),
                done = data["done"] as? Boolean ?: false,
                reminderAt = (data["reminderAt"] as? Timestamp)?.toDate(),
                createdFromMessageId = data["createdFromMessageId"] as? String,
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date()
            )
        }
    }
}

object TaskStore {
    private val db = FirebaseFirestore.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private fun col(uid: String) = db.collection("secure_ai").document(uid).collection("tasks")

    suspend fun load() {
        val u = uid ?: return
        try {
            val snap = col(u).orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            _tasks.value = snap.documents.mapNotNull { TaskItem.fromDoc(it.id, it.data ?: emptyMap()) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun add(title: String, recurrence: TaskRecurrence = TaskRecurrence.NONE, reminderAt: Date? = null) {
        val t = title.trim()
        val u = uid
        if (t.isEmpty() || u == null) return
        val item = TaskItem(title = t, recurrence = recurrence, reminderAt = reminderAt)
        _tasks.value = listOf(item) + _tasks.value
        col(u).document(item.id).set(item.toMap())
    }

    fun toggleDone(item: TaskItem) {
        val u = uid ?: return
        _tasks.value = _tasks.value.map { if (it.id == item.id) it.copy(done = !it.done) else it }
        col(u).document(item.id).update("done", !item.done)
    }

    fun update(item: TaskItem) {
        val u = uid ?: return
        _tasks.value = _tasks.value.map { if (it.id == item.id) item else it }
        col(u).document(item.id).set(item.toMap(), com.google.firebase.firestore.SetOptions.merge())
    }

    fun delete(item: TaskItem) {
        val u = uid ?: return
        _tasks.value = _tasks.value.filter { it.id != item.id }
        col(u).document(item.id).delete()
    }

    fun reset() { _tasks.value = emptyList() }
}
