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

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String = "📁",
    val systemPrompt: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("name", name.take(100))
        put("emoji", emoji)
        put("systemPrompt", systemPrompt.take(10_000))
        put("createdAt", Timestamp(createdAt))
        put("updatedAt", Timestamp.now())
    }

    companion object {
        fun fromDoc(id: String, data: Map<String, Any?>): Project? {
            val name = data["name"] as? String ?: return null
            return Project(
                id = id,
                name = name,
                emoji = data["emoji"] as? String ?: "📁",
                systemPrompt = data["systemPrompt"] as? String ?: "",
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
            )
        }
    }
}

object ProjectStore {
    private val db = FirebaseFirestore.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    // Distinguishes "confirmed empty" from "haven't fetched yet" so the
    // screen doesn't flash "No projects yet" before the first Firestore
    // round-trip completes.
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded: StateFlow<Boolean> = _hasLoaded.asStateFlow()

    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject.asStateFlow()

    fun setActive(project: Project?) { _activeProject.value = project }

    private fun col(uid: String) = db.collection("secure_ai").document(uid).collection("projects")

    suspend fun load() {
        val u = uid ?: return
        try {
            val snap = col(u).orderBy("updatedAt", Query.Direction.DESCENDING).get().await()
            _projects.value = snap.documents.mapNotNull { Project.fromDoc(it.id, it.data ?: emptyMap()) }
        } catch (e: Exception) { e.printStackTrace() }
        finally { _hasLoaded.value = true }
    }

    fun add(name: String, emoji: String, systemPrompt: String): Project {
        val u = uid ?: throw IllegalStateException("Not signed in")
        val project = Project(name = name.take(100), emoji = emoji, systemPrompt = systemPrompt.take(10_000))
        _projects.value = listOf(project) + _projects.value
        col(u).document(project.id).set(project.toMap())
        return project
    }

    fun update(project: Project) {
        val u = uid ?: return
        _projects.value = _projects.value.map { if (it.id == project.id) project else it }
        col(u).document(project.id).set(project.toMap(), com.google.firebase.firestore.SetOptions.merge())
    }

    fun delete(project: Project) {
        val u = uid ?: return
        _projects.value = _projects.value.filter { it.id != project.id }
        col(u).document(project.id).delete()
    }

    fun reset() { _projects.value = emptyList() }
}
