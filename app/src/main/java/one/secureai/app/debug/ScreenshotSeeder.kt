package one.secureai.app.debug

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import one.secureai.app.chat.ChatMessage
import one.secureai.app.chat.ChatRole
import one.secureai.app.data.store.ConversationStore
import one.secureai.app.data.store.LibraryStore
import one.secureai.app.data.store.Note
import one.secureai.app.data.store.NoteStore
import one.secureai.app.data.store.ProjectStore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Debug-only demo data for App Store / Play Store screenshots. Fetches a
 * shared JSON file (hosted at secureai.one, same content iOS reads) rather
 * than duplicating the fake projects/notes/chats/photos as hardcoded arrays
 * in two native codebases — one edit updates both platforms' screenshots.
 */
object ScreenshotSeeder {
    private const val SEED_URL = "https://www.secureai.one/seed-data.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val db = FirebaseFirestore.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun seed() = withContext(Dispatchers.IO) {
        if (uid == null) return@withContext
        val json = fetchSeedData() ?: return@withContext

        seedProjects(json)
        seedNotes(json)
        seedVoiceMemos(json)
        seedConversations(json)
        seedPhotos(json)
    }

    private fun fetchSeedData(): JSONObject? = runCatching {
        val request = Request.Builder().url(SEED_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            JSONObject(body)
        }
    }.getOrNull()

    // MARK: - Projects

    private suspend fun seedProjects(json: JSONObject) {
        val u = uid ?: return
        val col = db.collection("secure_ai").document(u).collection("projects")
        if (!isEmpty(col)) return
        val arr = json.optJSONArray("projects") ?: return
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            ProjectStore.add(
                name = p.getString("name"),
                emoji = p.getString("emoji"),
                systemPrompt = p.getString("systemPrompt")
            )
        }
    }

    // MARK: - Notes

    private suspend fun seedNotes(json: JSONObject) {
        val u = uid ?: return
        val col = db.collection("secure_ai").document(u).collection("notes")
        if (!isEmpty(col)) return
        val arr = json.optJSONArray("notes") ?: return
        for (i in 0 until arr.length()) {
            val n = arr.getJSONObject(i)
            val date = daysAgo(i)
            NoteStore.save(
                Note(
                    title = n.getString("title"),
                    body = n.getString("body"),
                    createdAt = date,
                    updatedAt = date
                )
            )
        }
    }

    // MARK: - Voice Memos
    // Metadata-only (matches iOS): no real audio file exists for demo
    // entries, so this writes directly to Firestore rather than going
    // through VoiceMemoStore.save(), which requires an actual recording to
    // upload to Storage.

    private suspend fun seedVoiceMemos(json: JSONObject) {
        val u = uid ?: return
        val col = db.collection("secure_ai").document(u).collection("voice_memos")
        if (!isEmpty(col)) return
        val arr = json.optJSONArray("voiceMemos") ?: return
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            val date = daysAgo(
                m.getInt("daysAgo"),
                hour = m.getInt("hour"),
                minute = m.getInt("minute")
            )
            val id = java.util.UUID.randomUUID().toString()
            val doc = hashMapOf<String, Any>(
                "title" to m.getString("title"),
                "duration" to m.getDouble("durationSeconds"),
                "storagePath" to "",
                "createdAt" to Timestamp(date)
            )
            runCatching { col.document(id).set(doc).await() }
        }
    }

    // MARK: - Conversations

    private suspend fun seedConversations(json: JSONObject) {
        val u = uid ?: return
        val col = db.collection("conversations").document(u).collection("chats")
        if (!isEmpty(col)) return
        val arr = json.optJSONArray("conversations") ?: return
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val secondsAgo = c.getLong("secondsAgo")
            val baseTime = Date(System.currentTimeMillis() - secondsAgo * 1000)
            val messages = listOf(
                ChatMessage(
                    role = ChatRole.USER,
                    content = c.getString("userMessage"),
                    timestamp = baseTime
                ),
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = c.getString("assistantMessage"),
                    timestamp = Date(baseTime.time + 3_000)
                )
            )
            val id = ConversationStore.saveConversation(
                conversationId = null,
                title = c.getString("title"),
                lastMessage = c.getString("assistantMessage").take(100),
                newMessages = messages,
                messageCount = messages.size
            )
            // saveConversation always stamps updatedAt as "now" — backdate it
            // afterward so seeded chats appear spread across the last two
            // weeks instead of all bunched at the top, freshly created.
            runCatching {
                col.document(id).update("updatedAt", Timestamp(baseTime)).await()
            }
        }
    }

    // MARK: - Photos

    private suspend fun seedPhotos(json: JSONObject) {
        val u = uid ?: return
        val col = db.collection("secure_ai").document(u).collection("library")
        if (!isEmpty(col)) return
        val arr = json.optJSONArray("photos") ?: return
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val picsumId = p.getInt("picsumId")
            val name = p.getString("name")
            val bytes = runCatching {
                java.net.URL("https://picsum.photos/id/$picsumId/1024/1024").readBytes()
            }.getOrNull() ?: continue
            runCatching {
                LibraryStore.add("$name.jpg", "image/jpeg", bytes, tags = listOf("ai-generated"))
            }
        }
    }

    // MARK: - Helpers

    private suspend fun isEmpty(col: com.google.firebase.firestore.CollectionReference): Boolean {
        return runCatching { col.limit(1).get().await().isEmpty }.getOrDefault(true)
    }

    private fun daysAgo(days: Int, hour: Int? = null, minute: Int? = null): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        if (hour != null) cal.set(Calendar.HOUR_OF_DAY, hour)
        if (minute != null) cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        return cal.time
    }
}
