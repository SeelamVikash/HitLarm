package com.example.hitlarm.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hitlarm.theme.CyberCyan
import com.example.hitlarm.theme.DarkGrey
import com.example.hitlarm.theme.LightGrey
import com.example.hitlarm.theme.Obsidian

@Composable
fun StreakFreezeDialog(
    freezeDaysUsed: Int,
    onDismiss: () -> Unit,
    onFreezeConfirmed: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val remaining = maxOf(0, 2 - freezeDaysUsed)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Obsidian),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Freeze Daily Streak",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Freeze protects your streak if you can't wake up. You have $remaining of 2 freeze days remaining in a row.",
                    fontSize = 14.sp,
                    color = LightGrey,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for freeze") },
                    placeholder = { Text("e.g. sick, traveling") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkGrey,
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = LightGrey,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = LightGrey)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (reason.isNotBlank()) {
                                onFreezeConfirmed(reason)
                            }
                        },
                        enabled = reason.isNotBlank() && remaining > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Obsidian,
                            disabledContainerColor = DarkGrey,
                            disabledContentColor = LightGrey
                        )
                    ) {
                        Text("Freeze Streak", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
