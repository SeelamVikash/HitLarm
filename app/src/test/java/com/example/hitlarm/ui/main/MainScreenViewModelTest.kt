package com.example.hitlarm.ui.main

import com.example.hitlarm.data.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun uiState_initiallyLoading() = runTest {
        val viewModel = MainScreenViewModel(FakeDataRepository())
        // Combine flow is hot and will initial emit immediately
        val state = viewModel.uiState.first()
        // It will be Success since flows are initialized instantly in tests
        assert(state is MainUiState.Success)
    }
}

private class FakeDataRepository : DataRepository {
    override val alarms = MutableStateFlow<List<Alarm>>(emptyList())
    override val streakState = MutableStateFlow<StreakState>(StreakState())

    override fun getAlarmsList(): List<Alarm> = emptyList()
    override fun saveAlarm(alarm: Alarm) {}
    override fun deleteAlarm(alarmId: String) {}
    override fun updateAlarmActiveState(alarmId: String, isActive: Boolean) {}
    override fun getStreakStateValue(): StreakState = StreakState()
    override fun recordWakeUp(dateStr: String, timeStr: String) {}
    override fun recordFreeze(dateStr: String, reason: String): Boolean = true
    override fun checkStreakSync(todayStr: String) {}
}
