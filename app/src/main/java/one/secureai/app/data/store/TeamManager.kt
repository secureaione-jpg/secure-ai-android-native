package one.secureai.app.data.store

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import one.secureai.app.BuildConfig
import org.json.JSONObject
import java.util.Date
import java.util.concurrent.TimeUnit

data class Team(
    val id: String,
    val name: String,
    val inviteCode: String = "",
    val memberCount: Int = 0,
    val createdAt: Date = Date()
)

data class GroupMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val isAI: Boolean = false,
    val timestamp: Date = Date()
)

object TeamManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val db = FirebaseFirestore.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _groupMessages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val groupMessages: StateFlow<List<GroupMessage>> = _groupMessages.asStateFlow()

    private var messageListener: ListenerRegistration? = null

    suspend fun loadTeams() = withContext(Dispatchers.IO) {
        val token = getIdToken() ?: return@withContext
        try {
            val request = Request.Builder()
                .url("${BuildConfig.WORKER_URL}/api/team/list")
                .header("Authorization", "Bearer $token")
                .header("X-App-Secret", BuildConfig.APP_SECRET)
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.code != 200) return@withContext
                val json = JSONObject(resp.body?.string() ?: return@withContext)
                val arr = json.optJSONArray("teams") ?: return@withContext
                val list = mutableListOf<Team>()
                for (i in 0 until arr.length()) {
                    val t = arr.getJSONObject(i)
                    list.add(Team(
                        id = t.getString("id"),
                        name = t.getString("name"),
                        inviteCode = t.optString("inviteCode", ""),
                        memberCount = t.optInt("memberCount", 0)
                    ))
                }
                _teams.value = list
            }
        } catch (_: Exception) {}
    }

    suspend fun createTeam(name: String): Team? = withContext(Dispatchers.IO) {
        val token = getIdToken() ?: return@withContext null
        try {
            val payload = JSONObject().apply { put("name", name) }
            val request = Request.Builder()
                .url("${BuildConfig.WORKER_URL}/api/team/create")
                .header("Authorization", "Bearer $token")
                .header("X-App-Secret", BuildConfig.APP_SECRET)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.code != 200) return@withContext null
                val json = JSONObject(resp.body?.string() ?: return@withContext null)
                val team = Team(
                    id = json.getString("id"),
                    name = name,
                    inviteCode = json.optString("inviteCode", "")
                )
                _teams.value = listOf(team) + _teams.value
                team
            }
        } catch (_: Exception) { null }
    }

    suspend fun joinTeam(inviteCode: String): Boolean = withContext(Dispatchers.IO) {
        val token = getIdToken() ?: return@withContext false
        try {
            val payload = JSONObject().apply { put("inviteCode", inviteCode) }
            val request = Request.Builder()
                .url("${BuildConfig.WORKER_URL}/api/team/join")
                .header("Authorization", "Bearer $token")
                .header("X-App-Secret", BuildConfig.APP_SECRET)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.code == 200) { loadTeams(); true } else false
            }
        } catch (_: Exception) { false }
    }

    fun listenToMessages(teamId: String, conversationId: String = "default") {
        messageListener?.remove()
        val path = db.collection("teams").document(teamId)
            .collection("conversations").document(conversationId)
            .collection("messages")
        messageListener = path.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                _groupMessages.value = snap.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    GroupMessage(
                        id = doc.id,
                        senderId = data["senderId"] as? String ?: "",
                        senderName = data["senderName"] as? String ?: "",
                        content = data["content"] as? String ?: "",
                        isAI = data["isAI"] as? Boolean ?: false,
                        timestamp = (data["timestamp"] as? Timestamp)?.toDate() ?: Date()
                    )
                }
            }
    }

    suspend fun sendGroupMessage(teamId: String, text: String, senderName: String, conversationId: String = "default") {
        val u = uid ?: return
        val msgData = hashMapOf<String, Any>(
            "senderId" to u,
            "senderName" to senderName,
            "content" to text,
            "isAI" to false,
            "timestamp" to Timestamp.now()
        )
        try {
            db.collection("teams").document(teamId)
                .collection("conversations").document(conversationId)
                .collection("messages")
                .add(msgData).await()

            val lowerText = text.lowercase()
            if ("@ai" in lowerText || "@secure ai" in lowerText) {
                triggerAIResponse(teamId, conversationId, text)
            }
        } catch (_: Exception) {}
    }

    private suspend fun triggerAIResponse(teamId: String, conversationId: String, userText: String) {
        val token = getIdToken() ?: return
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("teamId", teamId)
                    put("conversationId", conversationId)
                    put("message", userText)
                }
                val request = Request.Builder()
                    .url("${BuildConfig.WORKER_URL}/api/team/chat")
                    .header("Authorization", "Bearer $token")
                    .header("X-App-Secret", BuildConfig.APP_SECRET)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().close()
            } catch (_: Exception) {}
        }
    }

    fun stopListening() {
        messageListener?.remove()
        messageListener = null
        _groupMessages.value = emptyList()
    }

    fun reset() {
        stopListening()
        _teams.value = emptyList()
    }

    private suspend fun getIdToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            user.getIdToken(false).await().token
        } catch (_: Exception) { null }
    }
}
