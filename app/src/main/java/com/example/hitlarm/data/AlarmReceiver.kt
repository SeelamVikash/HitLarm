/*
 * ============================================================================
 * HitLarm - Android Anti-Oversleep Solution
 * ----------------------------------------------------------------------------
 * PROTECTED PROPERTY - ALL RIGHTS RESERVED
 * 
 * Ideation: Vikash Seelam
 * Place: Ingolstadt
 * Created completely by: Google Antigravity
 * Date: 06.07.2026
 * ============================================================================
 */
package com.example.hitlarm.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.hitlarm.AlarmActivity
import com.example.hitlarm.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val appContext = context.applicationContext

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule active alarms
            val repository = DefaultDataRepository(appContext)
            val scheduler = AlarmScheduler(appContext)
            repository.getAlarmsList().forEach { alarm ->
                if (alarm.isActive) {
                    scheduler.schedule(alarm)
                }
            }
        } else if (action == "android.intent.action.ALARM_TRIGGER") {
            val alarmId = intent.getStringExtra("extra_alarm_id") ?: return

            // Start playing the alarm ringtone via a Foreground Service
            val serviceIntent = Intent(appContext, AlarmService::class.java).apply {
                putExtra("extra_alarm_id", alarmId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent)
            } else {
                appContext.startService(serviceIntent)
            }

            // Fire Fullscreen Intent / Launch Activity
            launchAlarmActivity(appContext, alarmId)
        }
    }

    private fun launchAlarmActivity(context: Context, alarmId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "hitlarm_alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fires when an alarm is triggered"
                setBypassDnd(true)
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Fullscreen Intent
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("extra_alarm_id", alarmId)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Builder
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("HitLarm Alert!")
            .setContentText("Wake up! Complete your exercise to stop the alarm.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // Post Notification
        notificationManager.notify(1001, notification)

        // Also attempt direct launch
        try {
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            // Log or ignore, fullScreenIntent handles it if blocked
        }
    }
}
