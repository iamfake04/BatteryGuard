package com.example.batteryguard

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

private const val TIRAMISU = 33
// Our main colors
val RoyalBlue = Color(0xFF4169E1)
val DeepPurple = Color(0xFF483D8B)
val DarkBackground = Color(0xFF121212)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        createNotificationChannel()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = RoyalBlue,
                    secondary = DeepPurple,
                    background = DarkBackground
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BatteryMonitorScreen()
                }
            }
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "battery_channel",
                "Battery Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    override fun onStart() {
        super.onStart()
        // Start our background service
        Intent(this, BatteryService::class.java).also { intent ->
            startForegroundService(intent)
        }
    }
}

@Composable
fun BatteryMonitorScreen() {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var isAlertShown by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                batteryLevel = (level * 100 / scale.toFloat()).toInt()

                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING

                if (batteryLevel >= 80 && isCharging && !isAlertShown) {
                    showBatteryAlert(context)
                    isAlertShown = true
                } else if (batteryLevel < 80) {
                    isAlertShown = false
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)

        onDispose {
            context.unregisterReceiver(batteryReceiver)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Modern way to use CircularProgressIndicator
        CircularProgressIndicator(
            modifier = Modifier.size(200.dp),
            strokeWidth = 15.dp,
            progress = { batteryLevel / 100f },
            color = RoyalBlue,
            trackColor = DeepPurple
        )

        Text(
            text = "$batteryLevel%",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = if (batteryLevel >= 80 && isCharging)
                "Battery level high! Please unplug"
            else "Monitoring battery...",
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun showBatteryAlert(context: Context?) {
    context?.let {
        val builder = NotificationCompat.Builder(it, "battery_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Battery Alert")
            .setContentText("Battery level has reached 80%")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }
}