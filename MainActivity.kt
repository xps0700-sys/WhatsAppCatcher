package com.example.whatsappcatcher

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.get(this) }
    private lateinit var status: TextView
    private lateinit var list: LinearLayout

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "مقتنص المحذوفات"
            textSize = 26f
        }
        root.addView(title)

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, 16, 0, 16)
        }
        root.addView(status)

        val notificationBtn = Button(this).apply {
            text = "تفعيل وصول الإشعارات"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        root.addView(notificationBtn)

        val storageBtn = Button(this).apply {
            text = "تفعيل الوصول للملفات"
            setOnClickListener { openStorageSettings() }
        }
        root.addView(storageBtn)

        val startBtn = Button(this).apply {
            text = "تشغيل مراقبة الوسائط"
            setOnClickListener { startMediaService() }
        }
        root.addView(startBtn)

        val aboutBtn = Button(this).apply {
            text = "حول التطبيق"
            setOnClickListener { showAboutDialog() }
        }
        root.addView(aboutBtn)

        val footer = TextView(this).apply {
            text = "إعداد وتطوير محمد أمين © 2026"
            textSize = 14f
            setPadding(0, 10, 0, 10)
        }
        root.addView(footer)

        val clearBtn = Button(this).apply {
            text = "حذف جميع السجلات"
            setOnClickListener {
                lifecycleScope.launch {
                    db.dao().deleteAll()
                    Toast.makeText(this@MainActivity, "تم حذف السجلات", Toast.LENGTH_SHORT).show()
                }
            }
        }
        root.addView(clearBtn)

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        startMediaService()
        observeDatabase()
        refreshStatus()
    }

    private fun openStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    private fun startMediaService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, MediaMonitorService::class.java))
        } else {
            startService(Intent(this, MediaMonitorService::class.java))
        }
    }

    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("حول مقتنص المحذوفات")
            .setMessage(
                "مقتنص المحذوفات\\n\\n" +
                "تطبيق محلي لحفظ محتوى إشعارات WhatsApp والوسائط التي يتم تنزيلها فعلياً على الجهاز.\\n\\n" +
                "الإصدار: 1.0.0\\n" +
                "يعمل بدون اتصال بالإنترنت.\\n\\n" +
                "إعداد وتطوير محمد أمين © 2026"
            )
            .setPositiveButton("إغلاق", null)
            .show()
    }

    private fun observeDatabase() {
        lifecycleScope.launch {
            db.dao().observeAll().collect { items ->
                list.removeAllViews()

                items.forEach { item ->
                    val tv = TextView(this@MainActivity).apply {
                        text = if (item.kind == "message") {
                            val date = SimpleDateFormat(
                                "yyyy-MM-dd HH:mm",
                                Locale.getDefault()
                            ).format(Date(item.createdAt))
                            "📝 $date\n${item.sender}: ${item.text}\n"
                        } else {
                            "📁 ${item.filePath}\n"
                        }
                        textSize = 16f
                        setPadding(0, 12, 0, 12)
                    }
                    list.addView(tv)
                }
            }
        }
    }

    private fun refreshStatus() {
        val storage = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()
        ) "مفعّل" else "غير مفعّل"

        status.text =
            "التخزين: $storage\n" +
            "التطبيق يعمل محلياً ولا يحتاج إلى اتصال بالإنترنت."
    }
}
