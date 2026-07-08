package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Bill
import com.example.viewmodel.CrmViewModel

@Composable
fun BillsScreen(
    viewModel: CrmViewModel,
    onNavigateToClientProfile: (Int) -> Unit
) {
    val context = LocalContext.current
    val billsList by viewModel.bills.collectAsState()
    val clientsList by viewModel.clients.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Billing & Invoices",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Generate and download PDF invoices for client charges. Recalculates balance sheets in real-time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (billsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No bills generated yet.", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(billsList, key = { it.id }) { bill ->
                    val client = clientsList.find { it.id == bill.clientId }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Invoice #${bill.billNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Date: ${bill.billDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text(
                                    text = bill.paymentStatus.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (bill.paymentStatus.lowercase() == "paid") Color(0xFF00E676) else Color(0xFFE65100)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (client != null) {
                                Text(
                                    text = "Client: ${client.name} (${client.businessName})",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.width(IntrinsicSize.Max)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total: ₹${bill.totalAmount}", fontSize = 13.sp)
                                Text("Paid: ₹${bill.advancePaid}", fontSize = 13.sp, color = Color(0xFF00E676))
                                Text("Balance: ₹${bill.pendingAmount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.deleteBill(bill) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                                Button(
                                    onClick = {
                                        if (client != null) {
                                            viewModel.generateInvoicePdf(bill, client, emptyList())
                                        } else {
                                            Toast.makeText(context, "Associated Client missing.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download PDF Invoice")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
