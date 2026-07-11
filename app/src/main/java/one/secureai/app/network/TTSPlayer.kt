package one.secureai.app.network

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import one.secureai.app.BuildConfig
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object TTSPlayer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var player: MediaPlayer? = null
    private var tempFile: File? = null

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    suspend fun togglePlay(messageId: String, text: String, cacheDir: File) {
        if (_playingMessageId.value == messageId) {
            stop()
            return
        }
        stop()
        _playingMessageId.value = messageId
        try {
            val audioBytes = fetchTTS(text)
            val file = File(cacheDir, "tts_$messageId.mp3")
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { it.write(audioBytes) }
            }
            tempFile = file
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener { stop() }
                start()
            }
        } catch (_: Exception) {
            stop()
        }
    }

    fun stop() {
        player?.release()
        player = null
        tempFile?.delete()
        tempFile = null
        _playingMessageId.value = null
    }

    private suspend fun fetchTTS(text: String): ByteArray = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("text", text.take(4096))
            put("voice", "alloy")
        }
        val request = Request.Builder()
            .url("${BuildConfig.WORKER_URL}/tts")
            .header("X-App-Secret", BuildConfig.APP_SECRET)
            .header("X-App-Source", "secure-ai-android")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { resp ->
            if (resp.code != 200) throw Exception("TTS failed: ${resp.code}")
            resp.body?.bytes() ?: throw Exception("Empty TTS response")
        }
    }
}
