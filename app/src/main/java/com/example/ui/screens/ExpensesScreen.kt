package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.viewmodel.CrmViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpensesScreen(viewModel: CrmViewModel) {
    val context = LocalContext.current
    val expensesList by viewModel.expenses.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Rent") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Rent", "Salaries", "Printing Material", "Facebook Ads", "Google Ads", "Office Expense", "Travel", "Other")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Agency Business Expenses",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Track payouts, hardware print stock, team salaries, and ad budgets. Profit margins refresh on the Dashboard instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expensesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No expense transactions recorded.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(expensesList, key = { it.id }) { exp ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(exp.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Category: ${exp.category}  |  Date: ${exp.date}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    if (exp.notes.isNotBlank()) {
                                        Text("Notes: ${exp.notes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("₹${exp.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(onClick = { viewModel.deleteExpense(exp) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Log Business Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Expense Title * (e.g. Office Rent)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            var expandedCategory by remember { mutableStateOf(false) }
                            Button(onClick = { expandedCategory = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Text(category, color = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat) }, onClick = {
                                        category = cat
                                        expandedCategory = false
                                    })
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount Paid (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Transaction Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val sel = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        date = simpleDateFormat.format(sel.time)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            }
                        }
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Transaction Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dbl = amount.toDoubleOrNull()
                        if (title.isBlank() || dbl == null || dbl <= 0.0) {
                            Toast.makeText(context, "Title and valid amount required", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        viewModel.saveExpense(
                            Expense(
                                title = title.trim(),
                                category = category,
                                amount = dbl,
                                date = date,
                                notes = notes.trim()
                            )
                        )
                        showDialog = false
                        Toast.makeText(context, "Expense Saved", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Payout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
