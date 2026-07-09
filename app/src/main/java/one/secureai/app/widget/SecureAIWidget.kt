package one.secureai.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import one.secureai.app.MainActivity
import one.secureai.app.R

class SecureAIWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_secure_ai)

            fun pendingIntent(url: String, requestCode: Int): PendingIntent {
                val intent = Intent(context, MainActivity::class.java).apply {
                    data = Uri.parse(url)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                return PendingIntent.getActivity(
                    context, requestCode, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            views.setOnClickPendingIntent(R.id.widget_new_chat, pendingIntent("https://secureai.one/chat?new=1", 1))
            views.setOnClickPendingIntent(R.id.widget_tasks, pendingIntent("https://secureai.one/tasks", 2))
            views.setOnClickPendingIntent(R.id.widget_memories, pendingIntent("https://secureai.one/memories", 3))
            views.setOnClickPendingIntent(R.id.widget_logo, pendingIntent("https://secureai.one/chat", 4))

            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
