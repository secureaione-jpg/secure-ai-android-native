package one.secureai.app.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import one.secureai.app.BuildConfig
import one.secureai.app.chat.ChatMessage
import one.secureai.app.chat.ChatRole
import one.secureai.app.data.Prefs
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Kotlin port of secure-ai-iOS's WorkerTransport + RemoteAIService, combined
 * into one file (Android has no shared package like iOS's WorkerClient yet —
 * fine for a single app, worth extracting if Sanna Android grows this too).
 *
 * Guest-only for now: no Authorization header is sent, so every request
 * resolves to the Worker's anonymous free-tier path (see worker.ts identity
 * resolution's final `else` branch) keyed to a stable per-install anonToken.
 * Sign-in / Firebase Auth is a follow-up, not needed to prove the core
 * native-chat architecture works.
 */
class RemoteAIService(context: Context) {

    private val appContext = context.applicationContext

    // 25s / 1 retry — matches the fix applied to iOS's WorkerClient after a
    // stalled connection there once left users waiting up to ~6 minutes with
    // the previous 120s/2-retry settings.
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    sealed class StreamError(message: String) : IOException(message) {
        data class Http(val code: Int, val bodyMessage: String?) : StreamError("HTTP $code: ${bodyMessage ?: ""}")
        data object Incomplete : StreamError("Response was interrupted.")
    }

    /**
     * Sends the full message history (last item is the newest user message)
     * and streams the assistant's reply via [onChunk]. Returns the full
     * accumulated response text.
     */
    suspend fun sendMessageStreaming(
        history: List<ChatMessage>,
        model: String = "secureai-auto",
        onChunk: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val messagesJson = JSONArray().apply {
            history.forEach { m ->
                put(JSONObject().apply {
                    put("role", m.role.wireValue)
                    put("content", m.content)
                })
            }
        }
        val payload = JSONObject().apply {
            put("model", model)
            put("messages", messagesJson)
            put("stream", true)
            put("anonToken", Prefs.getAnonToken(appContext))
        }

        val request = Request.Builder()
            .url(BuildConfig.WORKER_URL)
            .header("X-App-Secret", BuildConfig.APP_SECRET)
            .header("X-App-Source", "secure-ai-android")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { response ->
            when (response.code) {
                200 -> { /* fall through to stream parsing */ }
                401 -> throw StreamError.Http(401, "Authentication failed.")
                403 -> throw StreamError.Http(403, parseErrorMessage(response.body?.string()))
                429 -> throw StreamError.Http(429, parseErrorMessage(response.body?.string()))
                in 500..599 -> throw StreamError.Http(response.code, "Server error. Try again shortly.")
                else -> throw StreamError.Http(response.code, null)
            }

            val provider = response.header("X-Resolved-Provider") ?: "openai"
            val source = response.body?.source() ?: throw StreamError.Incomplete

            var fullResponse = ""
            var completed = false
            var receivedAny = false

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isEmpty() || line.startsWith(":") || line.startsWith("event:")) continue

                val data = if (line.startsWith("data: ")) line.substring(6) else line
                if (data == "[DONE]") { completed = true; break }

                val json = runCatching { JSONObject(data) }.getOrNull() ?: continue

                if (provider == "anthropic" && json.optString("type") == "message_stop") {
                    completed = true; break
                }

                val text = extractDelta(json, provider)
                if (!text.isNullOrEmpty()) {
                    receivedAny = true
                    fullResponse += text
                    onChunk(text)
                }
            }

            if (provider == "google" && receivedAny) completed = true
            if (!completed) throw StreamError.Incomplete

            fullResponse
        }
    }

    private fun extractDelta(json: JSONObject, provider: String): String? = when (provider) {
        "anthropic" -> {
            if (json.optString("type") == "content_block_delta") {
                json.optJSONObject("delta")?.optString("text")
            } else null
        }
        "google" -> {
            json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
        }
        else -> { // openai
            json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("delta")
                ?.optString("content")
        }
    }

    private fun parseErrorMessage(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return runCatching { JSONObject(trimmed).optString("error") }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: trimmed.takeIf { it.length <= 200 }
    }
}
