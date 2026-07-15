package one.secureai.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import one.secureai.app.BuildConfig
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImageService {

    class ImageError(message: String) : IOException(message)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // image generation is slow — no SSE to keep the connection "alive" in smaller increments
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Returns raw PNG/JPEG bytes for a single 1024x1024 generated image. */
    suspend fun generate(prompt: String, context: Context? = null): ByteArray = withContext(Dispatchers.IO) {
        if (context != null && !isOnline(context)) {
            throw ImageError("No internet connection. Connect to a network and try again.")
        }
        val payload = JSONObject().apply {
            put("prompt", prompt)
            put("size", "1024x1024")
        }
        val request = Request.Builder()
            .url("${BuildConfig.WORKER_URL}/image")
            .header("X-App-Secret", BuildConfig.APP_SECRET)
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = runCatching { JSONObject(bodyString).optString("error") }.getOrNull()
                throw ImageError(msg?.takeIf { it.isNotEmpty() } ?: "Image generation failed (${response.code}).")
            }
            val json = runCatching { JSONObject(bodyString) }.getOrNull()
                ?: throw ImageError("The image couldn't be created. Try rephrasing your request.")
            val item = json.optJSONArray("data")?.optJSONObject(0)
                ?: throw ImageError("The image couldn't be created. Try rephrasing your request.")

            item.optString("b64_json").takeIf { it.isNotEmpty() }?.let {
                return@withContext Base64.decode(it, Base64.DEFAULT)
            }
            item.optString("url").takeIf { it.isNotEmpty() }?.let { imageUrl ->
                val imgRequest = Request.Builder().url(imageUrl).build()
                client.newCall(imgRequest).execute().use { imgResponse ->
                    val bytes = imgResponse.body?.bytes()
                    if (imgResponse.isSuccessful && bytes != null) return@withContext bytes
                }
            }
            throw ImageError("The image couldn't be created. Try rephrasing your request.")
        }
    }
}
