package com.example.whatsappcatcher

import android.app.*
import android.content.Intent
import android.os.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MediaMonitorService : Service() {

    private val observers = ConcurrentHashMap<String, FileObserver>()
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1001, notification())
        startMonitoring()
    }

    private fun startMonitoring() {
        if (running) return
        running = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()) return

        val root = File(
            Environment.getExternalStorageDirectory(),
            "Android/media/com.whatsapp/WhatsApp/Media"
        )
        if (!root.exists()) return

        scanAndWatch(root)
    }

    private fun scanAndWatch(dir: File) {
        if (!dir.isDirectory) return
        watchDirectory(dir)

        dir.listFiles()?.filter { it.isDirectory }?.forEach {
            scanAndWatch(it)
        }
    }

    private fun watchDirectory(dir: File) {
        val key = dir.absolutePath
        if (observers.containsKey(key)) return

        val observer = object : FileObserver(
            dir,
            CREATE or MOVED_TO or CLOSE_WRITE
        ) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null) return
                val file = File(dir, path)

                if (file.isDirectory) {
                    scanAndWatch(file)
                    return
                }

                if ((event and (CREATE or MOVED_TO or CLOSE_WRITE)) != 0) {
                    handler.postDelayed({ backup(file) }, 700)
                }
            }
        }

        observers[key] = observer
        observer.startWatching()
    }

    private fun backup(source: File) {
        if (!source.isFile || !source.exists()) return

        val lower = source.name.lowercase()
        val supported = listOf(
            ".jpg", ".jpeg", ".png", ".webp", ".gif",
            ".mp4", ".3gp", ".mkv",
            ".opus", ".ogg", ".mp3", ".m4a", ".aac",
            ".webm"
        ).any { lower.endsWith(it) }

        if (!supported) return

        try {
            val root = File(
                Environment.getExternalStorageDirectory(),
                "Captured_WhatsApp/Media"
            )
            if (!root.exists()) root.mkdirs()

            val relative = source.absolutePath.substringAfter(
                "Android/media/com.whatsapp/WhatsApp/Media/"
            )
            val destination = File(root, relative)
            destination.parentFile?.mkdirs()

            if (!destination.exists() || destination.length() != source.length()) {
                source.copyTo(destination, overwrite = true)
            }

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                AppDatabase.get(this@MediaMonitorService).dao().insert(
                    CapturedItem(
                        kind = "media",
                        filePath = destination.absolutePath
                    )
                )
            }
        } catch (_: Exception) {
            // تجاهل الملف إذا كان قيد الاستخدام أو تم حذفه أثناء النسخ.
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "media_monitor",
                "مراقبة وسائط WhatsApp",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun notification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, "media_monitor")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("مقتنص المحذوفات")
            .setContentText("مراقبة وسائط WhatsApp مفعلة")
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        observers.values.forEach { it.stopWatching() }
        observers.clear()
        running = false
        super.onDestroy()
    }
}
