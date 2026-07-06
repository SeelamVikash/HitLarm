package com.example.hitlarm.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.hitlarm.data.Alarm
import com.example.hitlarm.data.AlarmScheduler
import com.example.hitlarm.data.DefaultDataRepository
import com.example.hitlarm.data.RecordStatus
import com.example.hitlarm.data.StopPreference
import com.example.hitlarm.data.StreakState
import com.example.hitlarm.theme.CyberCyan
import com.example.hitlarm.theme.DarkGrey
import com.example.hitlarm.theme.ElectricPurple
import com.example.hitlarm.theme.LightGrey
import com.example.hitlarm.theme.MutedText
import com.example.hitlarm.theme.NeonGreen
import com.example.hitlarm.theme.NeonPink
import com.example.hitlarm.theme.Obsidian
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val repository = remember { DefaultDataRepository(context) }
    val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(repository) }
    val scheduler = remember { AlarmScheduler(context) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var showFreezeDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {
        when (state) {
            MainUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyberCyan)
                }
            }
            is MainUiState.Success -> {
                val data = state as MainUiState.Success
                MainContent(
                    alarms = data.alarms,
                    streak = data.streak,
                    onToggleActive = { alarm, active ->
                        viewModel.toggleAlarmActive(alarm, active, scheduler)
                    },
                    onDeleteAlarm = { alarm ->
                        viewModel.deleteAlarm(alarm, scheduler)
                    },
                    onEditAlarm = { alarm ->
                        editingAlarm = alarm
                        showAddSheet = true
                    },
                    onAddAlarmClick = {
                        editingAlarm = null
                        showAddSheet = true
                    },
                    onFreezeClick = {
                        showFreezeDialog = true
                    },
                    onHistoryClick = {
                        showHistorySheet = true
                    }
                )

                // Dialogs & Sheets
                if (showAddSheet) {
                    AddAlarmBottomSheet(
                        alarm = editingAlarm,
                        onDismiss = { showAddSheet = false },
                        onSave = { alarm ->
                            viewModel.saveAlarm(alarm, scheduler)
                            showAddSheet = false
                        }
                    )
                }

                if (showFreezeDialog) {
                    StreakFreezeDialog(
                        freezeDaysUsed = data.streak.freezeDaysUsedInRow,
                        onDismiss = { showFreezeDialog = false },
                        onFreezeConfirmed = { reason ->
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            viewModel.recordFreeze(todayStr, reason)
                            showFreezeDialog = false
                        }
                    )
                }

                if (showHistorySheet) {
                    StreakHistoryBottomSheet(
                        history = data.streak.history,
                        onDismiss = { showHistorySheet = false }
                    )
                }
            }
            is MainUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: ${(state as MainUiState.Error).throwable.message}",
                        color = NeonPink,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MainContent(
    alarms: List<Alarm>,
    streak: StreakState,
    onToggleActive: (Alarm, Boolean) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onAddAlarmClick: () -> Unit,
    onFreezeClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp) // Leave space for Floating Action Button
    ) {
        // App Title
        Text(
            text = "HitLarm",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = CyberCyan,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Streak Stats Dashboard Card
        StreakDashboardCard(
            streak = streak,
            onFreezeClick = onFreezeClick,
            onHistoryClick = onHistoryClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Alarms Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Alarms",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Hold card to delete",
                fontSize = 12.sp,
                color = MutedText
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Alarms List
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No alarms scheduled. Tap + to add.",
                    color = LightGrey,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { active -> onToggleActive(alarm, active) },
                        onClick = { onEditAlarm(alarm) },
                        onLongClick = { onDeleteAlarm(alarm) }
                    )
                }
            }
        }
    }

    // Floating Action Button
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onAddAlarmClick,
            containerColor = CyberCyan,
            contentColor = Obsidian,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StreakDashboardCard(
    streak: StreakState,
    onFreezeClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val isFrozenToday = streak.history.any { it.date == todayStr && it.status == RecordStatus.FROZEN }
    val isWokeUpToday = streak.history.any { it.date == todayStr && it.status == RecordStatus.WOKE_UP }
    
    val remainingFreezes = maxOf(0, 2 - streak.freezeDaysUsedInRow)

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Brush.horizontalGradient(listOf(CyberCyan, ElectricPurple)), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkGrey)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${streak.currentStreak} Days",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔥", fontSize = 28.sp)
                    }
                    Text(
                        text = "Current Wakeup Streak",
                        fontSize = 12.sp,
                        color = LightGrey
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${streak.bestStreak} Days",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricPurple
                    )
                    Text(
                        text = "Best Streak",
                        fontSize = 12.sp,
                        color = LightGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Freeze Button
                val freezeButtonText = when {
                    isFrozenToday -> "Frozen Today ❄️"
                    isWokeUpToday -> "Woke Up Today ☀️"
                    remainingFreezes == 0 -> "No Freezes Left"
                    else -> "Freeze Streak ($remainingFreezes Left)"
                }
                
                Button(
                    onClick = onFreezeClick,
                    enabled = !isFrozenToday && !isWokeUpToday && remainingFreezes > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = Obsidian,
                        disabledContainerColor = Obsidian,
                        disabledContentColor = MutedText
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(freezeButtonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // History Button
                Button(
                    onClick = onHistoryClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Streak History 📊", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGrey),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Time
                val hour12 = if (alarm.hour == 0 || alarm.hour == 12) 12 else alarm.hour % 12
                val amPm = if (alarm.hour < 12) "AM" else "PM"
                val timeString = String.format(Locale.US, "%02d:%02d", hour12, alarm.minute)
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeString,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = amPm,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Label
                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        fontSize = 14.sp,
                        color = LightGrey,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Days & Challenge Badge
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Challenge Type
                    val challengeEmoji = when (alarm.stopPreference) {
                        StopPreference.NORMAL -> "⏰"
                        StopPreference.PUSHUPS -> "💪"
                        StopPreference.SQUATS -> "🦵"
                        StopPreference.SHAKE -> "📳"
                        StopPreference.MATH -> "🧮"
                        StopPreference.BARCODE -> "🏷️"
                    }
                    val challengeText = when (alarm.stopPreference) {
                        StopPreference.NORMAL -> "Normal Dismiss"
                        StopPreference.PUSHUPS -> "${alarm.targetCount} Pushups"
                        StopPreference.SQUATS -> "${alarm.targetCount} Squats"
                        StopPreference.SHAKE -> "${alarm.targetCount} Shakes"
                        StopPreference.MATH -> "${alarm.targetCount} Problems"
                        StopPreference.BARCODE -> "Scan Barcode"
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricPurple.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(challengeEmoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = challengeText,
                                fontSize = 11.sp,
                                color = ElectricPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Repeated Days
                    if (alarm.days.isNotEmpty()) {
                        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        val activeDays = alarm.days.sorted().map { dayNames[it - 1] }.joinToString(", ")
                        Text(
                            text = activeDays,
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    } else {
                        Text(
                            text = "Once",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                }
            }

            // Switch
            Switch(
                checked = alarm.isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Obsidian,
                    checkedTrackColor = CyberCyan,
                    uncheckedThumbColor = LightGrey,
                    uncheckedTrackColor = Obsidian,
                    uncheckedBorderColor = DarkGrey
                )
            )
        }
    }
}
