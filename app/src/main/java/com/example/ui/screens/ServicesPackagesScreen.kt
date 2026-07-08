package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PackageTemplate
import com.example.viewmodel.CrmViewModel

@Composable
fun ServicesPackagesScreen(viewModel: CrmViewModel) {
    val context = LocalContext.current
    val templates by viewModel.packageTemplates.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    var templateName by remember { mutableStateOf("") }
    var serviceName by remember { mutableStateOf("Banner Print") }
    var quantity by remember { mutableStateOf("1") }
    var rate by remember { mutableStateOf("0.0") }
    var discount by remember { mutableStateOf("0.0") }
    var notes by remember { mutableStateOf("") }

    val defaultOptions = listOf(
        "Reel Shooting", "Reel Editing", "Google Ads", "Meta Ads",
        "Banner Design", "Banner Print", "Visiting Cards",
        "Account Handling", "Poster Design", "Story Design",
        "Logo Design", "Website Design", "Other"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Template")
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
                text = "Package Templates",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Save standard service configurations (e.g. standard design rates) to quickly add them to client portfolios later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (templates.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No package templates saved yet.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(templates, key = { it.id }) { template ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(template.templateName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Service: ${template.serviceName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Qty: ${template.quantity}  |  Rate: ₹${template.rate}  |  Discount: ₹${template.discount}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    if (template.notes.isNotBlank()) {
                                        Text("Notes: ${template.notes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                                IconButton(onClick = { viewModel.deletePackageTemplate(template) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Save Package Template") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        OutlinedTextField(
                            value = templateName,
                            onValueChange = { templateName = it },
                            label = { Text("Template Name * (e.g. Silver Plan)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("Select Service:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))) {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                items(defaultOptions) { opt ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { serviceName = opt }
                                            .background(if (serviceName == opt) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                            .padding(6.dp)
                                    ) {
                                        Text(opt, fontWeight = if (serviceName == opt) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = rate,
                            onValueChange = { rate = it },
                            label = { Text("Standard Rate (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = discount,
                            onValueChange = { discount = it },
                            label = { Text("Discount Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Template Description Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (templateName.isBlank()) {
                            Toast.makeText(context, "Template Name is required", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val q = quantity.toIntOrNull() ?: 1
                        val r = rate.toDoubleOrNull() ?: 0.0
                        val d = discount.toDoubleOrNull() ?: 0.0
                        viewModel.savePackageTemplate(
                            PackageTemplate(
                                templateName = templateName.trim(),
                                serviceName = serviceName,
                                quantity = q,
                                rate = r,
                                discount = d,
                                notes = notes.trim()
                            )
                        )
                        showDialog = false
                        Toast.makeText(context, "Template Saved", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Template")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
