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

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

interface DataRepository {
    val alarms: Flow<List<Alarm>>
    val streakState: Flow<StreakState>

    fun getAlarmsList(): List<Alarm>
    fun saveAlarm(alarm: Alarm)
    fun deleteAlarm(alarmId: String)
    fun updateAlarmActiveState(alarmId: String, isActive: Boolean)
    fun getStreakStateValue(): StreakState
    fun recordWakeUp(dateStr: String, timeStr: String)
    fun recordFreeze(dateStr: String, reason: String): Boolean
    fun recordSkip(dateStr: String, timeStr: String, reason: String)
    fun checkStreakSync(todayStr: String)
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val prefs = context.getSharedPreferences("hitlarm_preferences", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    override val alarms: Flow<List<Alarm>> = _alarms.asStateFlow()

    private val _streakState = MutableStateFlow<StreakState>(StreakState())
    override val streakState: Flow<StreakState> = _streakState.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "key_alarms" -> loadAlarms()
            "key_streak_state" -> loadStreak()
        }
    }

    init {
        loadAlarms()
        loadStreak()
        // Sync streak based on current date
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        checkStreakSync(todayStr)
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun loadAlarms() {
        val alarmsJson = prefs.getString("key_alarms", null)
        val list = if (alarmsJson != null) {
            try {
                json.decodeFromString<List<Alarm>>(alarmsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        _alarms.value = list
    }

    private fun saveAlarms(list: List<Alarm>) {
        _alarms.value = list
        prefs.edit().putString("key_alarms", json.encodeToString(list)).apply()
    }

    private fun loadStreak() {
        val streakJson = prefs.getString("key_streak_state", null)
        val state = if (streakJson != null) {
            try {
                json.decodeFromString<StreakState>(streakJson)
            } catch (e: Exception) {
                StreakState()
            }
        } else {
            StreakState()
        }
        _streakState.value = state
    }

    private fun saveStreak(state: StreakState) {
        _streakState.value = state
        prefs.edit().putString("key_streak_state", json.encodeToString(state)).apply()
    }

    override fun getAlarmsList(): List<Alarm> = _alarms.value

    override fun saveAlarm(alarm: Alarm) {
        val current = _alarms.value.toMutableList()
        val index = current.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            current[index] = alarm
        } else {
            current.add(alarm)
        }
        saveAlarms(current)
    }

    override fun deleteAlarm(alarmId: String) {
        val current = _alarms.value.toMutableList()
        current.removeAll { it.id == alarmId }
        saveAlarms(current)
    }

    override fun updateAlarmActiveState(alarmId: String, isActive: Boolean) {
        val current = _alarms.value.map {
            if (it.id == alarmId) it.copy(isActive = isActive) else it
        }
        saveAlarms(current)
    }

    override fun getStreakStateValue(): StreakState = _streakState.value

    override fun recordWakeUp(dateStr: String, timeStr: String) {
        val current = _streakState.value
        val history = current.history.toMutableList()

        // Check if we already logged wake up or freeze today to avoid double counting
        val todayRecordIndex = history.indexOfFirst { it.date == dateStr }
        var newStreak = current.currentStreak
        var newFreezeDaysUsed = current.freezeDaysUsedInRow

        if (todayRecordIndex >= 0) {
            val existingRecord = history[todayRecordIndex]
            if (existingRecord.status == RecordStatus.FROZEN) {
                // Override freeze if they woke up later (e.g. woke up after freezing)
                history[todayRecordIndex] = WakeUpRecord(dateStr, timeStr, RecordStatus.WOKE_UP)
                newFreezeDaysUsed = maxOf(0, newFreezeDaysUsed - 1)
                // Since they woke up, increment the streak by 1
                newStreak = current.currentStreak + 1
            }
            // If already WOKE_UP, keep the initial record and time (do not overwrite)
        } else {
            // New day wakeup
            history.add(WakeUpRecord(dateStr, timeStr, RecordStatus.WOKE_UP))
            newStreak = incrementStreakValue(current)
            newFreezeDaysUsed = 0 // reset consecutive freeze days since they woke up today
        }

        val newBest = maxOf(current.bestStreak, newStreak)
        val newState = current.copy(
            currentStreak = newStreak,
            bestStreak = newBest,
            lastActiveDate = dateStr,
            freezeDaysUsedInRow = newFreezeDaysUsed,
            history = history
        )
        saveStreak(newState)
    }

    override fun recordSkip(dateStr: String, timeStr: String, reason: String) {
        val current = _streakState.value
        val history = current.history.toMutableList()

        val todayRecordIndex = history.indexOfFirst { it.date == dateStr }
        if (todayRecordIndex >= 0) {
            history[todayRecordIndex] = WakeUpRecord(dateStr, timeStr, RecordStatus.SKIPPED, reason)
        } else {
            history.add(WakeUpRecord(dateStr, timeStr, RecordStatus.SKIPPED, reason))
        }

        val newState = current.copy(
            currentStreak = current.currentStreak,
            lastActiveDate = dateStr,
            freezeDaysUsedInRow = current.freezeDaysUsedInRow + 1,
            history = history
        )
        saveStreak(newState)
    }

    private fun incrementStreakValue(state: StreakState): Int {
        val lastActive = state.lastActiveDate ?: return 1
        val days = getDaysBetween(lastActive, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        return if (days <= 1) {
            state.currentStreak + 1
        } else {
            1
        }
    }

    override fun recordFreeze(dateStr: String, reason: String): Boolean {
        val current = _streakState.value
        if (current.freezeDaysUsedInRow >= 2) {
            // Maximum consecutive freezes reached
            return false
        }

        val history = current.history.toMutableList()
        val todayRecordIndex = history.indexOfFirst { it.date == dateStr }

        // If already woke up today, freeze is not allowed/necessary
        if (todayRecordIndex >= 0 && history[todayRecordIndex].status == RecordStatus.WOKE_UP) {
            return false
        }

        if (todayRecordIndex >= 0) {
            history[todayRecordIndex] = WakeUpRecord(dateStr, null, RecordStatus.FROZEN, reason)
        } else {
            history.add(WakeUpRecord(dateStr, null, RecordStatus.FROZEN, reason))
        }

        val newFreezeDaysUsed = current.freezeDaysUsedInRow + 1
        // Freeze protects the streak, so we keep currentStreak unchanged and set lastActiveDate to today
        val newState = current.copy(
            lastActiveDate = dateStr,
            freezeDaysUsedInRow = newFreezeDaysUsed,
            history = history
        )
        saveStreak(newState)
        return true
    }

    override fun checkStreakSync(todayStr: String) {
        val current = _streakState.value
        val lastActive = current.lastActiveDate ?: return

        val daysBetween = getDaysBetween(lastActive, todayStr)
        if (daysBetween <= 1) {
            // Within active range (yesterday or today)
            return
        }

        // Missed days exist between lastActive and today
        val history = current.history.toMutableList()
        var newStreak = current.currentStreak
        var newFreezeDaysUsed = current.freezeDaysUsedInRow

        for (i in 1 until daysBetween) {
            val missedDate = addDaysToDate(lastActive, i)
            // Check if there is already a record for that date
            if (history.none { it.date == missedDate }) {
                // If they had consecutive freeze days in row, they might have broken it
                history.add(WakeUpRecord(missedDate, null, RecordStatus.MISSED))
                newStreak = 0
                newFreezeDaysUsed = 0
            }
        }

        // Also if last active was yesterday, they are still fine today. But if last active was before yesterday,
        // it means they missed yesterday, so streak is broken.
        if (daysBetween > 1) {
            newStreak = 0
            newFreezeDaysUsed = 0
        }

        val newState = current.copy(
            currentStreak = newStreak,
            freezeDaysUsedInRow = newFreezeDaysUsed,
            history = history
        )
        saveStreak(newState)
    }

    private fun getDaysBetween(date1Str: String, date2Str: String): Int {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date1 = format.parse(date1Str) ?: return 0
            val date2 = format.parse(date2Str) ?: return 0
            val cal1 = Calendar.getInstance().apply {
                time = date1
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val cal2 = Calendar.getInstance().apply {
                time = date2
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diff = cal2.timeInMillis - cal1.timeInMillis
            (diff / (24 * 60 * 60 * 1000)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun addDaysToDate(dateStr: String, days: Int): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = format.parse(dateStr) ?: return dateStr
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, days)
            }
            format.format(cal.time)
        } catch (e: Exception) {
            dateStr
        }
    }
}
