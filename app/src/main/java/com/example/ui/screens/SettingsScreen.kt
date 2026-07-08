package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.Settings
import com.example.data.SupabaseClient
import com.example.ui.components.MediaUploadZone
import com.example.viewmodel.CrmViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: CrmViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsState by viewModel.settings.collectAsState()

    val settings = settingsState ?: Settings()

    var agencyName by remember(settings) { mutableStateOf(settings.agencyName) }
    var agencyPhone by remember(settings) { mutableStateOf(settings.agencyPhone) }
    var agencyAddress by remember(settings) { mutableStateOf(settings.agencyAddress) }
    var upiId by remember(settings) { mutableStateOf(settings.upiId) }
    
    var logoUrl by remember(settings) { mutableStateOf(settings.logoUrl) }
    var qrUrl by remember(settings) { mutableStateOf(settings.qrUrl) }

    var telegramToken by remember(settings) { mutableStateOf(settings.telegramToken) }
    var telegramChatId by remember(settings) { mutableStateOf(settings.telegramChatId) }
    var telegramEnabled by remember(settings) { mutableStateOf(settings.telegramEnabled) }

    var supabaseUrl by remember(settings) { mutableStateOf(settings.supabaseUrl) }
    var supabaseKey by remember(settings) { mutableStateOf(settings.supabaseKey) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Heading
        item {
            Text(
                text = "Agency & System Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Section 1: Agency Brand Setup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("AB Graphics Profile Setup", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    
                    OutlinedTextField(
                        value = agencyName,
                        onValueChange = { agencyName = it },
                        label = { Text("Agency Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = agencyPhone,
                        onValueChange = { agencyPhone = it },
                        label = { Text("Agency Hotline / Contact Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = agencyAddress,
                        onValueChange = { agencyAddress = it },
                        label = { Text("Office Headquarters Address") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI Merchant ID for Invoices (e.g. abgraphics@upi)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section 2: Supabase REST Backend
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Supabase Connection Config", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    Text("Input your Supabase project REST URL and anon key to sync clients, invoices, and upload brand assets.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = { supabaseUrl = it },
                        label = { Text("Supabase API URL") },
                        placeholder = { Text("https://your-project.supabase.co") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supabaseKey,
                        onValueChange = { supabaseKey = it },
                        label = { Text("Supabase Service / Anon Key") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Supabase Storage Assets:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    MediaUploadZone(
                        label = "AB Graphics Agency Logo",
                        currentUrl = logoUrl,
                        onUrlChanged = { logoUrl = it },
                        viewModel = viewModel,
                        allowedMimeType = "image/*",
                        helpText = "Supports PNG, JPG, JPEG, WEBP"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MediaUploadZone(
                        label = "Bank / UPI QR Code",
                        currentUrl = qrUrl,
                        onUrlChanged = { qrUrl = it },
                        viewModel = viewModel,
                        allowedMimeType = "image/*",
                        helpText = "Supports PNG, JPG, JPEG, WEBP"
                    )
                }
            }
        }

        // Section 3: Telegram Alerts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Telegram Alerts Bot", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        Switch(
                            checked = telegramEnabled,
                            onCheckedChange = { telegramEnabled = it }
                        )
                    }
                    Text("Send real-time alerts to your agency group whenever client profiles are added, bills generated, or payments updated.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    OutlinedTextField(
                        value = telegramToken,
                        onValueChange = { telegramToken = it },
                        label = { Text("Telegram Bot Token") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = telegramEnabled
                    )
                    OutlinedTextField(
                        value = telegramChatId,
                        onValueChange = { telegramChatId = it },
                        label = { Text("Telegram Chat / Group ID") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = telegramEnabled
                    )

                    if (telegramEnabled) {
                        Button(
                            onClick = {
                                if (telegramToken.isBlank() || telegramChatId.isBlank()) {
                                    Toast.makeText(context, "Fill Bot Token & Chat ID first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    val success = SupabaseClient.sendTelegramMessage(
                                        token = telegramToken,
                                        chatId = telegramChatId,
                                        message = "⚡ <b>AB Graphics CRM Connection Test</b>\nCRM Bot integration works successfully! Ready to push live updates."
                                    )
                                    if (success) {
                                        Toast.makeText(context, "Test notification dispatched!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Dispatch failed. Check Token and Chat ID.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Bot Connection")
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    val updated = Settings(
                        id = 1,
                        agencyName = agencyName.trim(),
                        agencyPhone = agencyPhone.trim(),
                        agencyAddress = agencyAddress.trim(),
                        upiId = upiId.trim(),
                        logoUrl = logoUrl.trim(),
                        qrUrl = qrUrl.trim(),
                        telegramToken = telegramToken.trim(),
                        telegramChatId = telegramChatId.trim(),
                        telegramEnabled = telegramEnabled,
                        supabaseUrl = supabaseUrl.trim(),
                        supabaseKey = supabaseKey.trim()
                    )
                    viewModel.saveSettings(updated)
                    Toast.makeText(context, "Settings Saved Successfully", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Configurations", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
