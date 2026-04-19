package com.th3cavalry.androidllm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import java.util.Calendar

/**
 * Service that handles system-level actions on the Android device.
 * These actions are exposed as tools to the LLM.
 */
class SystemToolService(private val context: Context) {

    /**
     * Creates an alarm using the AlarmClock intent.
     * This method does not require direct SET_ALARM permission if using the standard intent.
     */
    fun createAlarm(label: String, hour: Int, minutes: Int, skipUi: Boolean = true): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_SKIP_UI, skipUi)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Successfully set alarm for %02d:%02d: $label".format(hour, minutes)
        } catch (e: Exception) {
            "Failed to set alarm: ${e.message}"
        }
    }

    /**
     * Creates a calendar event using an intent.
     * This opens the system calendar app for the user to confirm.
     */
    fun createCalendarEvent(title: String, description: String, location: String?): String {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Successfully opened calendar to create event: $title"
        } catch (e: Exception) {
            "Failed to open calendar: ${e.message}"
        }
    }

    /**
     * Returns current battery and device status information.
     */
    fun getBatteryStatus(): String {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = batteryManager.isCharging
            
            val status = if (isCharging) "Charging" else "Discharging"
            "Battery Level: $level%, Status: $status"
        } catch (e: Exception) {
            "Failed to read battery status: ${e.message}"
        }
    }

    /**
     * Returns the current device time and date.
     */
    fun getDeviceTime(): String {
        val calendar = Calendar.getInstance()
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(calendar.time)
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)
        return "Current Device Time: $time, Date: $date"
    }
}
