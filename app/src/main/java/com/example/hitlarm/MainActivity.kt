/*
 * ============================================================================
 * HitLarm - Android Anti-Oversleep Solution
 * ----------------------------------------------------------------------------
 * OPEN SOURCED
 * 
 * Ideation: Vikash Seelam
 * Place: Ingolstadt
 * Created completely by: Google Antigravity
 * Date: 06.07.2026
 * ============================================================================
 */
package com.example.hitlarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.hitlarm.data.AlarmService
import com.example.hitlarm.theme.HitLarmTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      HitLarmTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }

  override fun onResume() {
    super.onResume()
    if (AlarmService.isRunning && AlarmService.currentAlarmId != null) {
      val intent = Intent(this, AlarmActivity::class.java).apply {
        putExtra("extra_alarm_id", AlarmService.currentAlarmId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
      startActivity(intent)
    }
  }
}
