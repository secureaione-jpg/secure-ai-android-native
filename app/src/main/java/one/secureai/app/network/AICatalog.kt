package one.secureai.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import one.secureai.app.BuildConfig
import one.secureai.app.data.model.SubscriptionTier
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Projects and (where noted) images are unlimited server-side (Worker sends
// null for those fields) — represented locally as Int.MAX_VALUE, the existing
// convention for "no cap" in this non-nullable Int model.
data class TierLimits(
    val dailyMessages: Int = 10,
    val dailyImages: Int = 3,
    val projectLimit: Int = Int.MAX_VALUE
)

object AICatalog {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var cachedLimits = mapOf<SubscriptionTier, TierLimits>()

    // Mirrors TIER_LIMITS in cloudflare-worker/worker.ts — kept in sync manually
    // as the offline/first-launch fallback until fetchConfig() succeeds.
    private val defaultLimits = mapOf(
        SubscriptionTier.FREE to TierLimits(10, 3, Int.MAX_VALUE),
        SubscriptionTier.PLUS to TierLimits(50, 10, Int.MAX_VALUE),
        SubscriptionTier.BUSINESS to TierLimits(50, 10, Int.MAX_VALUE),
        SubscriptionTier.PRO to TierLimits(150, 50, Int.MAX_VALUE),
        SubscriptionTier.ULTRA to TierLimits(300, 100, Int.MAX_VALUE),
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
                // Worker's /config response key is "tierLimits", not "tiers".
                val tierLimits = json.optJSONObject("tierLimits") ?: return@withContext
                val map = mutableMapOf<SubscriptionTier, TierLimits>()
                for (key in tierLimits.keys()) {
                    val tier = SubscriptionTier.entries.find { it.wireValue == key } ?: continue
                    val obj = tierLimits.getJSONObject(key)
                    map[tier] = TierLimits(
                        dailyMessages = obj.optInt("dailyMessages", defaultLimits[tier]?.dailyMessages ?: 10),
                        // optInt falls back correctly when the field is JSON null (unlimited).
                        dailyImages = obj.optInt("dailyImages", Int.MAX_VALUE),
                        projectLimit = obj.optInt("projectLimit", Int.MAX_VALUE)
                    )
                }
                cachedLimits = map
            }
        } catch (_: Exception) {}
    }
}
