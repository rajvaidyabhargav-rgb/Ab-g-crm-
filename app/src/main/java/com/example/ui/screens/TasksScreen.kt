package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Task
import com.example.viewmodel.CrmViewModel

@Composable
fun TasksScreen(
    viewModel: CrmViewModel,
    onNavigateToClientProfile: (Int) -> Unit
) {
    val context = LocalContext.current
    val tasksList by viewModel.tasks.collectAsState()
    val clientsList by viewModel.clients.collectAsState()

    var selectedStatusFilter by remember { mutableStateOf("Pending") }

    var showEditTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    val filteredTasks = tasksList.filter { task ->
        selectedStatusFilter == "All" ||
        (selectedStatusFilter == "Pending" && task.status.lowercase() == "pending") ||
        (selectedStatusFilter == "Done" && task.status.lowercase() == "done")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "CRM Tasks Checklist",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        // Status Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Pending", "Done", "All").forEach { tab ->
                FilterChip(
                    selected = selectedStatusFilter == tab,
                    onClick = { selectedStatusFilter = tab },
                    label = { Text(tab) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No tasks found in this status.", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredTasks, key = { it.id }) { task ->
                    val client = clientsList.find { it.id == task.clientId }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = task.status.lowercase() == "done",
                                    onCheckedChange = { viewModel.toggleTaskStatus(task) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = task.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (task.status.lowercase() == "done") Color.Gray else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Client: ${client?.name ?: "Unknown"} (${client?.businessName ?: "N/A"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            client?.id?.let { onNavigateToClientProfile(it) }
                                        }
                                    )
                                    Text(
                                        text = "Due: ${task.dueDate}  |  Priority: ${task.priority}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Priority badge
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when (task.priority.lowercase()) {
                                                "high" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                "medium" -> Color(0xFFFFD600).copy(alpha = 0.15f)
                                                else -> Color.Gray.copy(alpha = 0.15f)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = task.priority.uppercase(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = when (task.priority.lowercase()) {
                                            "high" -> MaterialTheme.colorScheme.primary
                                            "medium" -> Color(0xFFE65100)
                                            else -> Color.Gray
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    taskToEdit = task
                                    showEditTaskDialog = true
                                }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteTask(task) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditTaskDialog) {
        taskToEdit?.let { task ->
            EditTaskDialog(
                task = task,
                clientsList = clientsList,
                onDismiss = { showEditTaskDialog = false; taskToEdit = null },
                onSave = { updatedTask ->
                    viewModel.saveTask(updatedTask)
                    showEditTaskDialog = false
                    taskToEdit = null
                    Toast.makeText(context, "Task Updated", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
