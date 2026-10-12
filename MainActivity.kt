package com.tilluhacker.rgbpanel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 12, 22)
        window.navigationBarColor = Color.rgb(10, 12, 22)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(30), dp(22), dp(22))
            setBackgroundColor(Color.rgb(10, 12, 22))
        }
        val title = TextView(this).apply {
            text = "TILLU HACKER"
            textSize = 28f
            setTextColor(Color.rgb(0, 255, 210))
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "RGB FLOATING GAMING PANEL"
            textSize = 13f
            setTextColor(Color.rgb(190, 110, 255))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(22))
        }
        val info = TextView(this).apply {
            text = "Live CPU / RAM / battery monitor\nFree Fire MAX sensitivity & HUD profiles\nNo game-memory access or gameplay cheats."
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(24))
        }
        val permission = makeButton("1  •  Grant floating-window permission")
        permission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else Toast.makeText(this, "Overlay permission already enabled", Toast.LENGTH_SHORT).show()
        }
        val start = makeButton("2  •  Start floating panel")
        start.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "First grant floating-window permission", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                startForegroundService(Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_START))
                Toast.makeText(this, "Floating panel started", Toast.LENGTH_SHORT).show()
            }
        }
        val stop = makeButton("3  •  Stop floating panel")
        stop.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Floating panel stopped", Toast.LENGTH_SHORT).show()
        }
        root.addView(title, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(subtitle)
        root.addView(info)
        root.addView(permission)
        root.addView(start)
        root.addView(stop)
        setContentView(root)
    }

    private fun makeButton(label: String) = Button(this).apply {
        text = label
        textSize = 13f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(37, 32, 65))
        isAllCaps = false
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54))
        lp.setMargins(0, dp(6), 0, dp(6))
        layoutParams = lp
    }
}
