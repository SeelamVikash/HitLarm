package com.example.hitlarm.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hitlarm.data.RecordStatus
import com.example.hitlarm.data.WakeUpRecord
import com.example.hitlarm.theme.CyberCyan
import com.example.hitlarm.theme.DarkGrey
import com.example.hitlarm.theme.LightGrey
import com.example.hitlarm.theme.NeonGreen
import com.example.hitlarm.theme.NeonPink
import com.example.hitlarm.theme.Obsidian
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakHistoryBottomSheet(
    history: List<WakeUpRecord>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Obsidian,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .heightIn(max = 500.dp)
        ) {
            Text(
                text = "Streak History",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history logged yet. Complete alarms to build your streak!",
                        fontSize = 14.sp,
                        color = LightGrey
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(history.reversed()) { record ->
                        HistoryItemRow(record)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HistoryItemRow(record: WakeUpRecord) {
    val displayDate = formatDate(record.date)
    val statusColor = when (record.status) {
        RecordStatus.WOKE_UP -> NeonGreen
        RecordStatus.FROZEN -> CyberCyan
        RecordStatus.MISSED -> NeonPink
        RecordStatus.SKIPPED -> Color(0xFFFF9800) // Orange color
    }
    val statusText = when (record.status) {
        RecordStatus.WOKE_UP -> "Woke up at ${record.time ?: "--:--"}"
        RecordStatus.FROZEN -> "Streak Frozen"
        RecordStatus.MISSED -> "Missed day (Streak Reset)"
        RecordStatus.SKIPPED -> "Challenge Skipped (Streak Frozen)"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkGrey)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayDate,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                fontSize = 13.sp,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )
            if ((record.status == RecordStatus.FROZEN || record.status == RecordStatus.SKIPPED) && !record.reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reason: \"${record.reason}\"",
                    fontSize = 12.sp,
                    color = LightGrey
                )
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(statusColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(statusColor)
            )
        }
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US)
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}
