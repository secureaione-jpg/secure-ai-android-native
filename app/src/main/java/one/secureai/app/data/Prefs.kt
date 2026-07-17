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
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    private const val KEY_TEXT_SIZE = "text_size"
    private const val KEY_INCOGNITO = "incognito_mode"
    private const val KEY_LAST_CHAT_TIMESTAMP = "last_chat_timestamp"
    private const val KEY_DAILY_MESSAGE_COUNT = "daily_message_count"
    private const val KEY_DAILY_IMAGE_COUNT = "daily_image_count"
    private const val KEY_DAILY_COUNT_DATE = "daily_count_date"
    private const val KEY_LIFETIME_MESSAGES = "lifetime_messages"
    private const val KEY_SHOW_CHATS = "sidebar_chats"
    private const val KEY_SHOW_PROJECTS = "sidebar_projects"
    private const val KEY_SHOW_PHOTOS = "sidebar_photos"
    private const val KEY_SHOW_NOTES = "sidebar_notes"
    private const val KEY_CHAT_BACKGROUND = "chat_background"

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

    fun getAnonToken(ctx: Context): String {
        val p = prefs(ctx)
        return p.getString(KEY_ANON_TOKEN, null) ?: UUID.randomUUID().toString().also {
            p.edit { putString(KEY_ANON_TOKEN, it) }
        }
    }

    fun isHapticsEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_HAPTICS_ENABLED, true)
    fun setHapticsEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit { putBoolean(KEY_HAPTICS_ENABLED, v) }

    // 0 = small (14sp), 1 = medium (16sp, default), 2 = large (19sp)
    fun getTextSize(ctx: Context) = prefs(ctx).getInt(KEY_TEXT_SIZE, 1)
    fun setTextSize(ctx: Context, v: Int) = prefs(ctx).edit { putInt(KEY_TEXT_SIZE, v) }
    fun textSizeSp(ctx: Context): Int = when (getTextSize(ctx)) { 0 -> 14; 2 -> 19; else -> 16 }

    fun isIncognito(ctx: Context) = prefs(ctx).getBoolean(KEY_INCOGNITO, false)
    fun setIncognito(ctx: Context, v: Boolean) = prefs(ctx).edit { putBoolean(KEY_INCOGNITO, v) }

    fun getLastChatTimestamp(ctx: Context) = prefs(ctx).getLong(KEY_LAST_CHAT_TIMESTAMP, 0L)
    fun setLastChatTimestamp(ctx: Context, v: Long) = prefs(ctx).edit { putLong(KEY_LAST_CHAT_TIMESTAMP, v) }

    private fun todayString(): String {
        val cal = java.util.Calendar.getInstance()
        return "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
    }

    fun incrementDailyMessages(ctx: Context) {
        val p = prefs(ctx)
        val today = todayString()
        val savedDate = p.getString(KEY_DAILY_COUNT_DATE, "") ?: ""
        if (savedDate != today) {
            p.edit {
                putString(KEY_DAILY_COUNT_DATE, today)
                putInt(KEY_DAILY_MESSAGE_COUNT, 1)
                putInt(KEY_DAILY_IMAGE_COUNT, 0)
            }
        } else {
            p.edit { putInt(KEY_DAILY_MESSAGE_COUNT, p.getInt(KEY_DAILY_MESSAGE_COUNT, 0) + 1) }
        }
        p.edit { putInt(KEY_LIFETIME_MESSAGES, p.getInt(KEY_LIFETIME_MESSAGES, 0) + 1) }
    }

    fun getDailyMessageCount(ctx: Context): Int {
        val p = prefs(ctx)
        return if (p.getString(KEY_DAILY_COUNT_DATE, "") == todayString()) p.getInt(KEY_DAILY_MESSAGE_COUNT, 0) else 0
    }

    fun incrementDailyImages(ctx: Context) {
        val p = prefs(ctx)
        val today = todayString()
        if ((p.getString(KEY_DAILY_COUNT_DATE, "") ?: "") != today) {
            p.edit {
                putString(KEY_DAILY_COUNT_DATE, today)
                putInt(KEY_DAILY_MESSAGE_COUNT, 0)
                putInt(KEY_DAILY_IMAGE_COUNT, 1)
            }
        } else {
            p.edit { putInt(KEY_DAILY_IMAGE_COUNT, p.getInt(KEY_DAILY_IMAGE_COUNT, 0) + 1) }
        }
    }

    fun getDailyImageCount(ctx: Context): Int {
        val p = prefs(ctx)
        return if (p.getString(KEY_DAILY_COUNT_DATE, "") == todayString()) p.getInt(KEY_DAILY_IMAGE_COUNT, 0) else 0
    }

    fun getLifetimeMessages(ctx: Context) = prefs(ctx).getInt(KEY_LIFETIME_MESSAGES, 0)

    // Sidebar item visibility — all default true, matching iOS SidebarCustomizeView.
    fun showChats(ctx: Context) = prefs(ctx).getBoolean(KEY_SHOW_CHATS, true)
    fun setShowChats(ctx: Context, v: Boolean) = prefs(ctx).edit { putBoolean(KEY_SHOW_CHATS, v) }
    fun showProjects(ctx: Context) = prefs(ctx).getBoolean(KEY_SHOW_PROJECTS, true)
    fun setShowProjects(ctx: Context, v: Boolean) = prefs(ctx).edit { putBoolean(KEY_SHOW_PROJECTS, v) }
    fun showPhotos(ctx: Context) = prefs(ctx).getBoolean(KEY_SHOW_PHOTOS, true)
    fun setShowPhotos(ctx: Context, v: Boolean) = prefs(ctx).edit { putBoolean(KEY_SHOW_PHOTOS, v) }
    fun showNotes(ctx: Context) = prefs(ctx).getBoolean(KEY_SHOW_NOTES, true)
    fun setShowNotes(ctx: Context, v: Boolean) = prefs(ctx).edit { putBoolean(KEY_SHOW_NOTES, v) }

    fun chatBackground(ctx: Context): String = prefs(ctx).getString(KEY_CHAT_BACKGROUND, "system") ?: "system"
    fun setChatBackground(ctx: Context, v: String) = prefs(ctx).edit { putString(KEY_CHAT_BACKGROUND, v) }
}
