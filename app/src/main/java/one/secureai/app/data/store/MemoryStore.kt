package one.secureai.app.data.store

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object MemoryStore {
    private val db = FirebaseFirestore.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    private fun memoryRef(uid: String) =
        db.collection("secure_ai").document(uid).collection("memory").document("entries")

    suspend fun load() {
        val u = uid ?: return
        try {
            val doc = memoryRef(u).get().await()
            val saved = doc.get("entries") as? List<*>
            _entries.value = saved?.filterIsInstance<String>() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun save() {
        val u = uid ?: return
        memoryRef(u).set(
            mapOf("entries" to _entries.value, "updatedAt" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }

    fun add(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        _entries.value = _entries.value + t
        save()
    }

    fun update(index: Int, text: String) {
        val list = _entries.value.toMutableList()
        if (index !in list.indices) return
        val t = text.trim()
        if (t.isEmpty()) list.removeAt(index) else list[index] = t
        _entries.value = list
        save()
    }

    fun delete(index: Int) {
        val list = _entries.value.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        _entries.value = list
        save()
    }

    fun clear() {
        _entries.value = emptyList()
        save()
    }

    fun reset() { _entries.value = emptyList() }
}
