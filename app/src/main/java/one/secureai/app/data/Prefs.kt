package one.secureai.app.data

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

object Prefs {
    private const val FILE = "secureai_prefs"

    private const val KEY_ONBOARDED = "onboarded"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_SESSION_COUNT = "session_count"
    private const val KEY_REVIEW_REQUESTED = "review_requested"
    private const val KEY_NOTIFICATIONS_PROMPTED = "notifications_prompted"
    private const val KEY_ANON_TOKEN = "anon_token"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isOnboarded(ctx: Context) = prefs(ctx).getBoolean(KEY_ONBOARDED, false)
    fun setOnboarded(ctx: Context) = prefs(ctx).edit { putBoolean(KEY_ONBOARDED, true) }

    fun isBiometricEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_BIOMETRIC_ENABLED, true)
    fun setBiometricEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit { putBoolean(KEY_BIOMETRIC_ENABLED, v) }

    fun getSessionCount(ctx: Context) = prefs(ctx).getInt(KEY_SESSION_COUNT, 0)
    fun incrementSession(ctx: Context) = prefs(ctx).edit { putInt(KEY_SESSION_COUNT, getSessionCount(ctx) + 1) }

    fun isReviewRequested(ctx: Context) = prefs(ctx).getBoolean(KEY_REVIEW_REQUESTED, false)
    fun setReviewRequested(ctx: Context) = prefs(ctx).edit { putBoolean(KEY_REVIEW_REQUESTED, true) }

    fun isNotificationsPrompted(ctx: Context) = prefs(ctx).getBoolean(KEY_NOTIFICATIONS_PROMPTED, false)
    fun setNotificationsPrompted(ctx: Context) = prefs(ctx).edit { putBoolean(KEY_NOTIFICATIONS_PROMPTED, true) }

    /** Stable per-install id for guest (unauthenticated) chat requests — lets
     *  the Worker's free-tier daily quota bind to something other than just
     *  IP. Mirrors iOS's anonToken (there, Auth.auth().currentUser?.uid). */
    fun getAnonToken(ctx: Context): String {
        val p = prefs(ctx)
        return p.getString(KEY_ANON_TOKEN, null) ?: UUID.randomUUID().toString().also {
            p.edit { putString(KEY_ANON_TOKEN, it) }
        }
    }
}
