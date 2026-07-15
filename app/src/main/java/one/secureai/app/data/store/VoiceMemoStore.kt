package one.secureai.app.data.store

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date
import java.util.UUID

data class VoiceMemo(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val duration: Double = 0.0,
    val storagePath: String = "",
    val downloadURL: String? = null,
    val transcript: String? = null,
    val summary: String? = null,
    val projectId: String? = null,
    val createdAt: Date = Date()
) {
    val formattedDuration: String
        get() {
            val m = duration.toInt() / 60
            val s = duration.toInt() % 60
            return "%d:%02d".format(m, s)
        }

    fun toMap(): Map<String, Any?> = buildMap {
        put("title", title)
        put("duration", duration)
        put("storagePath", storagePath)
        put("createdAt", Timestamp(createdAt))
        if (downloadURL != null) put("downloadURL", downloadURL)
        if (transcript != null) put("transcript", transcript)
        if (summary != null) put("summary", summary)
        if (projectId != null) put("projectId", projectId)
    }

    companion object {
        fun fromDoc(id: String, data: Map<String, Any?>): VoiceMemo? {
            val title = data["title"] as? String ?: return null
            return VoiceMemo(
                id = id,
                title = title,
                duration = (data["duration"] as? Number)?.toDouble() ?: 0.0,
                storagePath = data["storagePath"] as? String ?: "",
                downloadURL = data["downloadURL"] as? String,
                transcript = data["transcript"] as? String,
                summary = data["summary"] as? String,
                projectId = data["projectId"] as? String,
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date()
            )
        }
    }
}

object VoiceMemoStore {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    // Own persistent scope — a save/update is often triggered from an action
    // (stop recording, navigate away) that can tear down the calling
    // composable's scope before the Storage/Firestore write completes.
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _memos = MutableStateFlow<List<VoiceMemo>>(emptyList())
    val memos: StateFlow<List<VoiceMemo>> = _memos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    fun clearSaveError() { _saveError.value = null }

    private fun col(uid: String) = db.collection("secure_ai").document(uid).collection("voice_memos")

    fun reset() { _memos.value = emptyList() }

    suspend fun load() {
        val u = uid ?: return
        _isLoading.value = true
        try {
            val snap = col(u).orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            _memos.value = snap.documents.mapNotNull { VoiceMemo.fromDoc(it.id, it.data ?: emptyMap()) }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    /** File deletion happens on completion, on the store's own scope, so the
     *  caller doesn't need to keep the temp file alive itself. */
    fun save(file: File, title: String, duration: Double, projectId: String? = null) {
        val u = uid ?: run {
            _saveError.value = "Sign in to save voice memos"
            return
        }
        val id = UUID.randomUUID().toString()
        val memo = VoiceMemo(id = id, title = title, duration = duration, projectId = projectId)
        _memos.value = listOf(memo) + _memos.value

        _isSaving.value = true
        storeScope.launch {
            try {
                val path = "secure_ai_library/$u/voice_$id.m4a"
                val ref = storage.reference.child(path)
                ref.putFile(android.net.Uri.fromFile(file)).await()
                val url = ref.downloadUrl.await().toString()
                val updated = memo.copy(storagePath = path, downloadURL = url)
                col(u).document(id).set(updated.toMap()).await()
                _memos.value = _memos.value.map { if (it.id == id) updated else it }
            } catch (e: Exception) {
                e.printStackTrace()
                _saveError.value = "Failed to upload memo"
            } finally {
                _isSaving.value = false
                file.delete()
            }
        }
    }

    fun updateTranscript(id: String, transcript: String) {
        val u = uid ?: return
        _memos.value = _memos.value.map { if (it.id == id) it.copy(transcript = transcript) else it }
        storeScope.launch {
            runCatching { col(u).document(id).update("transcript", transcript).await() }
        }
    }

    fun updateSummary(id: String, summary: String) {
        val u = uid ?: return
        _memos.value = _memos.value.map { if (it.id == id) it.copy(summary = summary) else it }
        storeScope.launch {
            runCatching { col(u).document(id).update("summary", summary).await() }
        }
    }

    fun delete(memo: VoiceMemo) {
        _memos.value = _memos.value.filter { it.id != memo.id }
        val u = uid ?: return
        col(u).document(memo.id).delete()
        if (memo.storagePath.isNotEmpty()) {
            storage.reference.child(memo.storagePath).delete()
        }
    }
}
