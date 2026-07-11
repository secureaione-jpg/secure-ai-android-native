package one.secureai.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import one.secureai.app.BuildConfig
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object EngagementReporter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun report(exchangeId: String, signal: String) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("exchangeId", exchangeId)
                put("signal", signal)
            }
            val request = Request.Builder()
                .url("${BuildConfig.WORKER_URL}/engagement")
                .header("X-App-Secret", BuildConfig.APP_SECRET)
                .header("X-App-Source", "secure-ai-android")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().close()
        } catch (_: Exception) {}
    }
}
