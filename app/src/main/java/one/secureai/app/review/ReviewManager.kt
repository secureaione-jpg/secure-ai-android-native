package one.secureai.app.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import one.secureai.app.data.Prefs

object ReviewManager {
    private const val SESSIONS_BEFORE_REVIEW = 5

    fun maybeRequestReview(activity: Activity, context: Context) {
        if (Prefs.isReviewRequested(context)) return
        if (Prefs.getSessionCount(context) < SESSIONS_BEFORE_REVIEW) return

        val manager = ReviewManagerFactory.create(context)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result).addOnCompleteListener {
                    Prefs.setReviewRequested(context)
                }
            }
        }
    }
}
