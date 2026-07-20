package one.secureai.app.data.store

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * Kotlin port of secure-ai-iOS's MemoryStore. Facts the assistant remembers
 * about the user, backed by the same Firestore path as iOS
 * (`secure_ai/{uid}/memories`) so the two clients share one memory pool.
 *
 * Note: mirrors iOS exactly, including having no dedicated "browse your
 * memories" screen — iOS only ever consumes this via [buildContextString]
 * (system-prompt injection) and [todaysNudge] (the proactive nudge card).
 * Same is true here; there's no reachable UI for listing/deleting memories
 * on either platform today.
 */
data class Memory(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val createdAt: Date = Date()
) {
    companion object {
        fun fromDoc(id: String, data: Map<String, Any?>): Memory? {
            val content = data["content"] as? String ?: return null
            val createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date()
            return Memory(id = id, content = content, createdAt = createdAt)
        }
    }

    val asMap: Map<String, Any> get() = mapOf(
        "content" to content,
        "createdAt" to Timestamp(createdAt)
    )
}

object MemoryStore {
    private val db = FirebaseFirestore.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _memories = MutableStateFlow<List<Memory>>(emptyList())
    val memories: StateFlow<List<Memory>> = _memories.asStateFlow()

    private fun col(uid: String) = db.collection("secure_ai").document(uid).collection("memories")

    suspend fun load() {
        val u = uid ?: return
        try {
            val snap = col(u).orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            _memories.value = snap.documents.mapNotNull { Memory.fromDoc(it.id, it.data ?: emptyMap()) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun save(memory: Memory) {
        val u = uid ?: return
        try {
            col(u).document(memory.id).set(memory.asMap).await()
            val current = _memories.value
            val idx = current.indexOfFirst { it.id == memory.id }
            _memories.value = if (idx >= 0) current.toMutableList().apply { set(idx, memory) }
                               else listOf(memory) + current
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun delete(memory: Memory) {
        _memories.value = _memories.value.filterNot { it.id == memory.id }
        val u = uid ?: return
        col(u).document(memory.id).delete()
    }

    fun reset() { _memories.value = emptyList() }

    /** Joined into the chat request's system prompt so the assistant has context. */
    fun buildContextString(): String {
        val current = _memories.value
        if (current.isEmpty()) return ""
        val items = current.take(50).joinToString("\n") { "- ${it.content}" }
        return "User memories:\n$items"
    }

    /**
     * Today's proactive nudge, if any — a memory reframed as something the
     * assistant brings up unprompted. Stable for the day (same memory keeps
     * showing until dismissed or the day rolls over) so it doesn't flicker
     * between app launches.
     */
    fun todaysNudge(isDismissedToday: (String) -> Boolean): Memory? {
        val current = _memories.value
        if (current.isEmpty()) return null
        val key = dayKey(Date())
        if (isDismissedToday(key)) return null
        val index = abs(key.hashValue()) % current.size
        return current[index]
    }

    fun dayKey(date: Date): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    // Kotlin String.hashCode() differs from Swift's String.hashValue, but
    // both just need a stable-per-day, evenly-distributed index — any
    // deterministic hash works here.
    private fun String.hashValue(): Int = hashCode()
}
