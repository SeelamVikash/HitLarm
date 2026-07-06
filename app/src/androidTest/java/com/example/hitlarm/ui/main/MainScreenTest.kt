package com.example.hitlarm.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.hitlarm.data.Alarm
import com.example.hitlarm.data.StreakState
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent {
      MainContent(
        alarms = FAKE_ALARMS,
        streak = StreakState(),
        onToggleActive = { _, _ -> },
        onDeleteAlarm = { },
        onEditAlarm = { },
        onAddAlarmClick = { },
        onFreezeClick = { },
        onHistoryClick = { }
      )
    }
  }

  @Test
  fun alarmList_exists() {
    composeTestRule.onNodeWithText("Your Alarms").assertExists()
    composeTestRule.onNodeWithText("07:30").assertExists()
  }
}

private val FAKE_ALARMS = listOf(
  Alarm(id = "1", hour = 7, minute = 30, label = "Morning Alarm")
)
