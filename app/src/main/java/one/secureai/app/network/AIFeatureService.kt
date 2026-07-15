package one.secureai.app.network

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import one.secureai.app.BuildConfig
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Kotlin port of secure-ai-iOS's AIFeatureService — note generation/summarization
 * and voice-memo transcription, hitting the same Cloudflare worker endpoints.
 */
object AIFeatureService {
    private const val BASE = "https://secure-ai-worker.secureai-one.workers.dev"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val transcribeClient = client.newBuilder()
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    data class GeneratedNote(val title: String, val body: String)

    suspend fun generateNote(prompt: String): GeneratedNote? {
        val body = JSONObject().apply {
            put("prompt", prompt)
            put("action", "generate")
        }
        return decodeNote(post("/ai/note", body))
    }

    suspend fun summarizeNote(text: String): GeneratedNote? {
        val body = JSONObject().apply {
            put("existingText", text)
            put("action", "summarize")
        }
        return decodeNote(post("/ai/note", body))
    }

    private fun decodeNote(raw: String?): GeneratedNote? {
        val json = raw?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return null
        val note = json.optJSONObject("note") ?: return null
        val title = note.optString("title", "")
        val body = note.optString("body", "")
        if (title.isEmpty() && body.isEmpty()) return null
        return GeneratedNote(title, body)
    }

    suspend fun summarizeText(text: String): String? {
        val body = JSONObject().apply { put("text", text) }
        val raw = post("/ai/summarize", body) ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        return json.optString("summary").takeIf { it.isNotEmpty() }
    }

    suspend fun transcribe(audioBytes: ByteArray, filename: String): String? = withContext(Dispatchers.IO) {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", filename,
                audioBytes.toRequestBody("audio/m4a".toMediaType())
            )
            .build()

        val requestBuilder = Request.Builder()
            .url("$BASE/transcribe")
            .header("X-App-Secret", BuildConfig.APP_SECRET)
            .post(multipart)
        applyAuth(requestBuilder)

        runCatching {
            transcribeClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(response.body?.string().orEmpty())
                json.optString("text").takeIf { it.isNotEmpty() }
            }
        }.getOrNull()
    }

    private suspend fun post(path: String, body: JSONObject): String? = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url("$BASE$path")
            .header("X-App-Secret", BuildConfig.APP_SECRET)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
        applyAuth(requestBuilder)

        runCatching {
            client.newCall(requestBuilder.build()).execute().use { response ->
                response.body?.string()
            }
        }.getOrNull()
    }

    private suspend fun applyAuth(builder: Request.Builder) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        runCatching {
            val token = user.getIdToken(false).await().token
            if (!token.isNullOrEmpty()) builder.header("Authorization", "Bearer $token")
        }
    }
}
