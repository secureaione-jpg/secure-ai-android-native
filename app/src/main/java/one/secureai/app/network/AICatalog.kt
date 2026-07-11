package one.secureai.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import one.secureai.app.BuildConfig
import one.secureai.app.data.model.SubscriptionTier
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TierLimits(
    val dailyMessages: Int = 25,
    val dailyFrontier: Int = 0,
    val dailyImages: Int = 0,
    val projectLimit: Int = 0,
    val memoryFactCap: Int = 10
)

object AICatalog {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var cachedLimits = mapOf<SubscriptionTier, TierLimits>()

    private val defaultLimits = mapOf(
        SubscriptionTier.FREE to TierLimits(25, 0, 0, 0, 10),
        SubscriptionTier.PLUS to TierLimits(100, 10, 5, 3, 50),
        SubscriptionTier.BUSINESS to TierLimits(200, 30, 10, 10, 100),
        SubscriptionTier.PRO to TierLimits(500, 100, 25, 25, 500),
        SubscriptionTier.ULTRA to TierLimits(Int.MAX_VALUE, Int.MAX_VALUE, 100, Int.MAX_VALUE, Int.MAX_VALUE),
    )

    fun limits(tier: SubscriptionTier): TierLimits =
        cachedLimits[tier] ?: defaultLimits[tier] ?: TierLimits()

    suspend fun fetchConfig() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${BuildConfig.WORKER_URL}/config")
                .header("X-App-Secret", BuildConfig.APP_SECRET)
                .header("X-App-Source", "secure-ai-android")
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.code != 200) return@withContext
                val json = JSONObject(resp.body?.string() ?: return@withContext)
                val tiers = json.optJSONObject("tiers") ?: return@withContext
                val map = mutableMapOf<SubscriptionTier, TierLimits>()
                for (key in tiers.keys()) {
                    val tier = SubscriptionTier.entries.find { it.wireValue == key } ?: continue
                    val obj = tiers.getJSONObject(key)
                    map[tier] = TierLimits(
                        dailyMessages = obj.optInt("dailyMessages", defaultLimits[tier]?.dailyMessages ?: 25),
                        dailyFrontier = obj.optInt("dailyFrontier", 0),
                        dailyImages = obj.optInt("dailyImages", 0),
                        projectLimit = obj.optInt("projectLimit", 0),
                        memoryFactCap = obj.optInt("memoryFactCap", 10)
                    )
                }
                cachedLimits = map
            }
        } catch (_: Exception) {}
    }
}
