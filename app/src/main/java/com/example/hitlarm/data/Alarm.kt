package com.example.hitlarm.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class StopPreference {
    NORMAL,
    PUSHUPS,
    SQUATS,
    SHAKE,
    MATH,
    BARCODE
}

@Serializable
data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "Wake Up!",
    val isActive: Boolean = true,
    val days: Set<Int> = emptySet(), // 1 for Sunday, 2 for Monday, ..., 7 for Saturday
    val stopPreference: StopPreference = StopPreference.NORMAL,
    val targetCount: Int = 10,
    val targetBarcode: String? = null
)
