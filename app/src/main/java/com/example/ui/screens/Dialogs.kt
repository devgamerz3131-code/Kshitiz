package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CoinTransaction
import com.example.data.model.StudyPointTransaction
import com.example.ui.theme.RankifyPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon

@Composable
fun RewardHistoryDialog(
    spTransactions: List<StudyPointTransaction>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Study Points History",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            if (spTransactions.isEmpty()) {
                Text(
                    "No study points earned yet. Complete study activities to earn SP!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(spTransactions) { trans ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = trans.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = dateFormat.format(trans.createdAt?.toDate() ?: Date()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                val isSpend = trans.type == "SPEND"
                                Text(
                                    text = if (isSpend) "${trans.amount}" else "+${trans.amount}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSpend) MaterialTheme.colorScheme.error else Color(0xFF3B82F6)
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AddTargetDialog(
    onDismiss: () -> Unit,
    onAdd: (subject: String, chapter: String, task: String, priority: String, estMinutes: Int) -> Unit
) {
    var subject by remember { mutableStateOf("Physics") }
    var chapter by remember { mutableStateOf("Current Electricity") }
    var task by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("High") }
    var estMinutes by remember { mutableStateOf("45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("New Study Target", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Subject:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Physics", "Chemistry", "Mathematics").forEach { sub ->
                        FilterChip(
                            selected = (subject == sub),
                            onClick = {
                                subject = sub
                                chapter = when (sub) {
                                    "Physics" -> "Current Electricity"
                                    "Chemistry" -> "Electrochemistry"
                                    else -> "Matrices"
                                }
                            },
                            label = { Text(sub, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("Chapter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = task,
                    onValueChange = { task = it },
                    label = { Text("Task (e.g., Solve 5 PYQs)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = estMinutes,
                        onValueChange = { estMinutes = it },
                        label = { Text("Est. Mins") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Priority:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        FilterChip(
                            selected = (priority == p),
                            onClick = { priority = p },
                            label = { Text(p, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (task.isNotBlank()) {
                        val mins = estMinutes.toIntOrNull() ?: 45
                        onAdd(subject, chapter, task, priority, mins)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
            ) {
                Text("Save Target")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FinishSessionDialog(
    subject: String,
    chapter: String,
    elapsedMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (difficulty: String, notes: String) -> Unit
) {
    var difficulty by remember { mutableStateOf("Medium") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Session Finished! 🎯", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Studied $subject • $chapter for $elapsedMinutes mins",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("How difficult was this session?", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Easy", "Medium", "Hard").forEach { diff ->
                        FilterChip(
                            selected = (difficulty == diff),
                            onClick = { difficulty = diff },
                            label = { Text(diff, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("What did you complete? (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(difficulty, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
            ) {
                Text("Save to Analytics")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Discard")
            }
        }
    )
}
