package com.example.batteryguard

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BatteryService : Service() {
    private var isAlertShown = false

    // This helps our service stay alive
    private val NOTIFICATION_ID = 100
    private val CHANNEL_ID = "battery_service_channel"

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val percentage = (level * 100 / scale.toFloat()).toInt()

            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING

            // Show alert at 80% while charging
            if (percentage >= 80 && isCharging && !isAlertShown) {
                showBatteryAlert(percentage)
                isAlertShown = true
            } else if (percentage < 80) {
                isAlertShown = false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Make our service a foreground service
        val notification = createNotification("Battery Monitor Running", "Monitoring battery levels...")
        startForeground(NOTIFICATION_ID, notification)

        // Start listening for battery changes
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Battery Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(title: String, message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showBatteryAlert(percentage: Int) {
        val alertNotification = NotificationCompat.Builder(this, "battery_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Battery Alert!")
            .setContentText("Battery level is at $percentage%")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, alertNotification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
    }
}