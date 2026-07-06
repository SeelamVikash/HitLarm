package com.example.hitlarm.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitlarm.data.Alarm
import com.example.hitlarm.data.AlarmScheduler
import com.example.hitlarm.data.DataRepository
import com.example.hitlarm.data.StreakState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        repository.alarms,
        repository.streakState
    ) { alarms, streak ->
        MainUiState.Success(alarms, streak)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState.Loading
    )

    fun saveAlarm(alarm: Alarm, scheduler: AlarmScheduler) {
        viewModelScope.launch {
            repository.saveAlarm(alarm)
            if (alarm.isActive) {
                scheduler.schedule(alarm)
            } else {
                scheduler.cancel(alarm)
            }
        }
    }

    fun toggleAlarmActive(alarm: Alarm, isActive: Boolean, scheduler: AlarmScheduler) {
        viewModelScope.launch {
            repository.updateAlarmActiveState(alarm.id, isActive)
            val updatedAlarm = alarm.copy(isActive = isActive)
            if (isActive) {
                scheduler.schedule(updatedAlarm)
            } else {
                scheduler.cancel(updatedAlarm)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm, scheduler: AlarmScheduler) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm.id)
            scheduler.cancel(alarm)
        }
    }

    fun recordFreeze(dateStr: String, reason: String) {
        viewModelScope.launch {
            repository.recordFreeze(dateStr, reason)
        }
    }
}

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Success(val alarms: List<Alarm>, val streak: StreakState) : MainUiState
    data class Error(val throwable: Throwable) : MainUiState
}
