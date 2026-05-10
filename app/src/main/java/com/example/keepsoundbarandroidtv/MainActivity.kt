package com.example.keepsoundbarandroidtv

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var txtStatus: TextView

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        setContentView(R.layout.activity_main)

        txtStatus = findViewById(R.id.txtStatus)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        updateStatus()

        btnStart.setOnClickListener {
            if (checkNotificationPermission()) {
                startKeepAliveService()
            }
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, KeepSoundbarService::class.java))
            txtStatus.postDelayed({ updateStatus() }, 500)
        }
    }

    private fun startKeepAliveService() {
        val serviceIntent = Intent(this, KeepSoundbarService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        txtStatus.postDelayed({ updateStatus() }, 500)
    }

    private fun updateStatus() {
        if (isServiceRunning(KeepSoundbarService::class.java)) {
            txtStatus.text = "Status: Running"
            txtStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        } else {
            txtStatus.text = "Status: Stopped"
            txtStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                return false
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}