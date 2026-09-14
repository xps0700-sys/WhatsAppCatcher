package com.example.whatsappcatcher

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CatcherService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val db by lazy { AppDatabase.get(this) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp" &&
            sbn.packageName != "com.whatsapp.w4b") return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        // لا نخزن إشعارات فارغة أو إشعارات النظام العامة.
        if (text.isBlank()) return
        if (title.isBlank() && text.isBlank()) return

        scope.launch {
            db.dao().insert(
                CapturedItem(
                    kind = "message",
                    sender = title,
                    text = text
                )
            )
        }
    }
}
