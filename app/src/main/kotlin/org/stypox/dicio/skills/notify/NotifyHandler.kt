package org.stypox.dicio.skills.notify

import android.service.notification.NotificationListenerService
import android.util.Log

open class NotifyHandler: NotificationListenerService() {
    companion object Companion {
        private const val TAG: String = "NotifyHandler"
        var Instance: NotifyHandler? = null
    }

    override fun onCreate() {
        super.onCreate()
        Instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        Instance = null
    }

    fun getActiveNotificationsList(): List<Notification> {
        // getActiveNotifications() can only be run in NotificationListenerService() class
        return getActiveNotifications().mapNotNull { notification ->
            val title = notification.notification.extras.getString("android.title")
            val message = notification.notification.extras.getString("android.text")
            if (title.isNullOrBlank() && message.isNullOrBlank()) {
                return@mapNotNull null // skip empty notifications (like from android_system)
            }
            Log.e(TAG, notification.notification.extras.toString())

            val appName = try {
                val appInfo = packageManager.getApplicationInfo(notification.packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                Log.e(TAG, "Could not get app name", e)
                ""
            }

            return@mapNotNull Notification(appName, title ?: "", message ?: "")
        }
    }
}

/**
 * Either [message] or [title] will be non-empty
 */
data class Notification(
    val appName: String,
    val title: String,
    val message: String,
)