package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Payment as CrmPayment
import com.example.viewmodel.CrmViewModel

@Composable
fun PaymentsScreen(viewModel: CrmViewModel) {
    val paymentsList by viewModel.payments.collectAsState()
    val clientsList by viewModel.clients.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Payments Received History",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Track advance deposits, installment payouts, and historical collections securely.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (paymentsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No transactions completed yet.", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(paymentsList, key = { it.id }) { pay ->
                    val client = clientsList.find { it.id == pay.clientId }

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
                                Text("Amount: ₹${pay.amount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF00E676))
                                if (client != null) {
                                    Text("Client: ${client.name} (${client.businessName})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Text("Date: ${pay.paymentDate}  |  Method: ${pay.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                if (pay.notes.isNotBlank()) {
                                    Text("Ref: ${pay.notes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = { viewModel.deletePayment(pay) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
