package com.example.hitlarm.data

import kotlinx.serialization.Serializable

@Serializable
enum class RecordStatus {
    WOKE_UP,
    FROZEN,
    MISSED,
    SKIPPED
}

@Serializable
data class WakeUpRecord(
    val date: String, // "YYYY-MM-DD"
    val time: String?, // "07:30 AM" (null if frozen/missed)
    val status: RecordStatus,
    val reason: String? = null // Reason typed when using a streak freeze
)

@Serializable
data class StreakState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDate: String? = null, // "YYYY-MM-DD"
    val freezeDaysUsedInRow: Int = 0, // max 2
    val history: List<WakeUpRecord> = emptyList()
)
