package one.secureai.app.data.store

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

data class LibraryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mimeType: String = "application/octet-stream",
    val tags: List<String> = emptyList(),
    val downloadURL: String? = null,
    val isImage: Boolean = false,
    val createdAt: Date = Date()
) {
    companion object {
        fun fromDoc(id: String, data: Map<String, Any?>): LibraryItem? {
            val name = data["name"] as? String ?: return null
            val mime = data["mimeType"] as? String ?: "application/octet-stream"
            return LibraryItem(
                id = id,
                name = name,
                mimeType = mime,
                tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                downloadURL = data["downloadURL"] as? String,
                isImage = mime.startsWith("image/"),
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date()
            )
        }
    }
}

object LibraryStore {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _items = MutableStateFlow<List<LibraryItem>>(emptyList())
    val items: StateFlow<List<LibraryItem>> = _items.asStateFlow()

    // Distinguishes "confirmed empty" from "haven't fetched yet" so screens
    // don't flash an empty state (with a distracting Create/Upload CTA)
    // before the first Firestore round-trip completes.
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded: StateFlow<Boolean> = _hasLoaded.asStateFlow()

    private fun col(uid: String) = db.collection("secure_ai").document(uid).collection("library")

    suspend fun load() {
        val u = uid ?: return
        try {
            val snap = col(u).orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            _items.value = snap.documents.mapNotNull { LibraryItem.fromDoc(it.id, it.data ?: emptyMap()) }
        } catch (e: Exception) { e.printStackTrace() }
        finally { _hasLoaded.value = true }
    }

    suspend fun add(name: String, mimeType: String, data: ByteArray, tags: List<String> = emptyList()) {
        val u = uid ?: return
        val id = UUID.randomUUID().toString()
        val storagePath = "secure_ai_library/$u/$id"
        try {
            val ref = storage.reference.child(storagePath)
            ref.putBytes(data).await()
            val downloadURL = ref.downloadUrl.await().toString()
            val doc = hashMapOf<String, Any>(
                "name" to name,
                "mimeType" to mimeType,
                "tags" to tags,
                "downloadURL" to downloadURL,
                "storagePath" to storagePath,
                "createdAt" to Timestamp.now()
            )
            col(u).document(id).set(doc).await()
            val item = LibraryItem(
                id = id, name = name, mimeType = mimeType, tags = tags,
                downloadURL = downloadURL, isImage = mimeType.startsWith("image/")
            )
            _items.value = listOf(item) + _items.value
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun delete(item: LibraryItem) {
        val u = uid ?: return
        _items.value = _items.value.filter { it.id != item.id }
        try {
            col(u).document(item.id).delete().await()
            storage.reference.child("secure_ai_library/$u/${item.id}").delete().await()
        } catch (_: Exception) {}
    }

    fun reset() { _items.value = emptyList() }
}
