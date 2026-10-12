package com.tilluhacker.rgbpanel

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.*
import android.widget.*
import java.io.BufferedReader
import java.io.FileReader
import java.util.Locale
import kotlin.concurrent.thread

class OverlayService : Service() {
    companion object {
        const val ACTION_START = "com.tilluhacker.rgbpanel.START"
        private const val CHANNEL = "tillu_panel_status"
        private const val NOTIFICATION_ID = 781
    }

    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: LinearLayout? = null
    private var expanded = false
    private var statsView: TextView? = null
    private var profileView: TextView? = null
    private var currentProfile = 0
    private val profiles = arrayOf(
        "Balanced • Sensitivity profile",
        "Rush • Sensitivity profile",
        "Precision • Sensitivity profile"
    )
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            if (expanded) statsView?.text = readStats()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        showBubble()
        handler.post(refresh)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START && bubble == null) showBubble()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL, "TILLU panel", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java)
        val pending = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        return builder.setContentTitle("TILLU HACKER RGB Panel")
            .setContentText("Floating performance monitor is running")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop", pending).build())
            .build()
    }

    private fun windowParams(width: Int, height: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else WindowManager.LayoutParams.TYPE_PHONE
        return WindowManager.LayoutParams(width, height, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 18; y = 180 }
    }

    private fun showBubble() {
        if (bubble != null) return
        val b = TextView(this).apply {
            text = "TH"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(0, 255, 220))
            setBackgroundColor(Color.rgb(25, 18, 48))
            elevation = 12f
            setOnClickListener { if (expanded) collapsePanel() else expandPanel() }
            setOnTouchListener(DragTouchListener(this@OverlayService))
        }
        bubble = b
        wm.addView(b, windowParams(dp(58), dp(58)))
    }

    private fun expandPanel() {
        if (panel != null) { expanded = true; panel?.visibility = View.VISIBLE; return }
        expanded = true
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(Color.rgb(13, 15, 29))
            elevation = 18f
        }
        val heading = TextView(this).apply {
            text = "TILLU HACKER  •  RGB"
            textSize = 17f
            setTextColor(Color.rgb(0, 255, 210))
        }
        statsView = TextView(this).apply {
            text = readStats()
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(0, dp(10), 0, dp(10))
        }
        val profileTitle = TextView(this).apply {
            text = "FREE FIRE MAX • LOCAL PROFILES"
            textSize = 12f
            setTextColor(Color.rgb(200, 120, 255))
        }
        profileView = TextView(this).apply {
            text = profiles[currentProfile]
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(0, dp(6), 0, dp(6))
        }
        val cycle = Button(this).apply {
            text = "Change profile"
            isAllCaps = false
            setOnClickListener {
                currentProfile = (currentProfile + 1) % profiles.size
                profileView?.text = profiles[currentProfile]
                getSharedPreferences("profiles", MODE_PRIVATE).edit().putInt("selected", currentProfile).apply()
            }
        }
        currentProfile = getSharedPreferences("profiles", MODE_PRIVATE).getInt("selected", 0).coerceIn(0, profiles.size - 1)
        profileView?.text = profiles[currentProfile]
        val close = Button(this).apply {
            text = "Hide panel"
            isAllCaps = false
            setOnClickListener { collapsePanel() }
        }
        box.addView(heading)
        box.addView(statsView)
        box.addView(profileTitle)
        box.addView(profileView)
        box.addView(cycle)
        box.addView(close)
        panel = box
        val p = windowParams(dp(290), WindowManager.LayoutParams.WRAP_CONTENT).apply { x = dp(12); y = dp(250) }
        wm.addView(box, p)
    }

    private fun collapsePanel() {
        expanded = false
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
    }

    private fun readStats(): String {
        val cpu = try {
            val first = readCpuLine()
            SystemClock.sleep(30)
            val second = readCpuLine()
            val total = second.first - first.first
            val idle = second.second - first.second
            if (total > 0) String.format(Locale.US, "%.0f%%", (100.0 * (total - idle) / total)) else "N/A"
        } catch (_: Exception) { "N/A" }
        val mem = try {
            var total = 0L; var available = 0L
            BufferedReader(FileReader("/proc/meminfo")).useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("MemTotal:")) total = line.filter { it.isDigit() || it == ' ' }.trim().split(Regex("\\s+"))[0].toLong()
                    if (line.startsWith("MemAvailable:")) available = line.filter { it.isDigit() || it == ' ' }.trim().split(Regex("\\s+"))[0].toLong()
                }
            }
            if (total > 0) "${((total - available) / 1024)} / ${total / 1024} MB used" else "N/A"
        } catch (_: Exception) { "N/A" }
        val battery = try {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            "${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
        } catch (_: Exception) { "N/A" }
        val temp = try {
            val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val tenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            if (tenths >= 0) String.format(Locale.US, "%.1f°C", tenths / 10f) else "N/A"
        } catch (_: Exception) { "N/A" }
        return "CPU: $cpu\nRAM: $mem\nBattery: $battery\nBattery temp: $temp\n\nSystem metrics only — no game memory access."
    }

    private fun readCpuLine(): Pair<Long, Long> {
        val line = BufferedReader(FileReader("/proc/stat")).use { it.readLine() }
        val values = line.trim().split(Regex("\\s+")).drop(1).map { it.toLong() }
        val total = values.sum()
        val idle = values.getOrElse(3) { 0L } + values.getOrElse(4) { 0L }
        return total to idle
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null; bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private class DragTouchListener(private val service: OverlayService) : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchX = 0f
        private var touchY = 0f
        private var dragged = false
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            val params = v.layoutParams as WindowManager.LayoutParams
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = e.rawX; touchY = e.rawY; dragged = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5) dragged = true
                    params.x = startX + dx; params.y = startY + dy
                    try { service.wm.updateViewLayout(v, params) } catch (_: Exception) {}
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) v.performClick()
                    return true
                }
            }
            return false
        }
    }
}
