package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.MediaUploadZone
import com.example.viewmodel.CrmViewModel
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyRow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClientProfileScreen(
    clientId: Int,
    viewModel: CrmViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clientState by viewModel.getClientById(clientId).collectAsState(initial = null)
    val services by viewModel.getClientServices(clientId).collectAsState(initial = emptyList())
    val bills by viewModel.getBillsByClient(clientId).collectAsState(initial = emptyList())
    val payments by viewModel.getPaymentsByClient(clientId).collectAsState(initial = emptyList())
    val tasks by viewModel.getTasksByClient(clientId).collectAsState(initial = emptyList())
    val logs by viewModel.getActivityLogsByClient(clientId).collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Operations", "Services & Bills", "Tasks & Payments")

    // Modals state
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showGenerateBillDialog by remember { mutableStateOf(false) }
    var customWorkUpdateText by remember { mutableStateOf("") }

    val client = clientState ?: return // Show loading or return

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(client.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.pushTelegramClientProfile(client.id)
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Telegram Push", tint = Color(0xFF00B0FF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Brief client snapshot ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = client.businessName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Status: ${client.status}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (client.status.lowercase() == "active") Color(0xFF00E676) else Color.Gray
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Billing: ₹${client.totalAmount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Paid: ₹${client.advancePaid}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00E676))
                        Text("Pending: ₹${client.pendingAmount}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tab bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                }
            }

            // Tab contents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> TabClientOperations(
                        client = client,
                        logs = logs,
                        customWorkUpdateText = customWorkUpdateText,
                        onUpdateTextChange = { customWorkUpdateText = it },
                        onSendWhatsApp = { templateText ->
                            viewModel.sendWhatsAppMessage(client.whatsapp.ifBlank { client.phone }, templateText)
                        },
                        viewModel = viewModel
                    )
                    1 -> TabServicesAndBills(
                        services = services,
                        bills = bills,
                        onAddService = { showAddServiceDialog = true },
                        onDeleteService = { viewModel.deleteClientService(it) },
                        onGenerateBill = { showGenerateBillDialog = true },
                        onDeleteBill = { viewModel.deleteBill(it) },
                        onExportPdf = { bill -> viewModel.generateInvoicePdf(bill, client, services) },
                        onWhatsAppInvoice = { bill ->
                            val text = "Hi ${client.name},\nWe have generated bill #${bill.billNumber} for ₹${bill.totalAmount}. Pending amount: ₹${bill.pendingAmount}.\nPlease process payments soon.\n\nAB Graphics"
                            viewModel.sendWhatsAppMessage(client.whatsapp.ifBlank { client.phone }, text)
                        }
                    )
                    2 -> TabTasksAndPayments(
                        tasks = tasks,
                        payments = payments,
                        onAddTask = { showAddTaskDialog = true },
                        onToggleTask = { viewModel.toggleTaskStatus(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onAddPayment = { showAddPaymentDialog = true },
                        onDeletePayment = { viewModel.deletePayment(it) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddServiceDialog) {
        AddServiceDialog(
            clientId = client.id,
            onDismiss = { showAddServiceDialog = false },
            onAdd = { service ->
                viewModel.addClientService(service)
                showAddServiceDialog = false
                Toast.makeText(context, "Service Added", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddPaymentDialog) {
        AddPaymentDialog(
            clientId = client.id,
            viewModel = viewModel,
            onDismiss = { showAddPaymentDialog = false },
            onAdd = { payment ->
                viewModel.addPayment(payment)
                showAddPaymentDialog = false
                Toast.makeText(context, "Payment Saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            clientId = client.id,
            onDismiss = { showAddTaskDialog = false },
            onAdd = { task ->
                viewModel.saveTask(task)
                showAddTaskDialog = false
                Toast.makeText(context, "Task Added", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showGenerateBillDialog) {
        GenerateBillDialog(
            clientId = client.id,
            totalClientBilling = client.totalAmount,
            advanceClientPaid = client.advancePaid,
            viewModel = viewModel,
            onDismiss = { showGenerateBillDialog = false },
            onGenerate = { bill ->
                viewModel.generateBill(bill)
                showGenerateBillDialog = false
                Toast.makeText(context, "Invoice Generated", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// --- TAB 1: OPERATIONS & WHATSAPP ---
@Composable
fun TabClientOperations(
    client: Client,
    logs: List<ActivityLog>,
    customWorkUpdateText: String,
    onUpdateTextChange: (String) -> Unit,
    onSendWhatsApp: (String) -> Unit,
    viewModel: CrmViewModel
) {
    val templates = viewModel.generateWhatsAppTemplates(client, null, customWorkUpdateText)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // WhatsApp templates panel
        item {
            Text("Proactive Client Messaging (WhatsApp)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customWorkUpdateText,
                onValueChange = onUpdateTextChange,
                label = { Text("Custom Work Update Details") },
                placeholder = { Text("Enter work updates to pre-fill the WhatsApp update template...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(templates) { tpl ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tpl.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        Button(
                            onClick = { onSendWhatsApp(tpl.body) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(tpl.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // About / Notes Card
        item {
            Text("Portfolio Details & Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📍 Address: ${client.address.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodyMedium)
                    Text("📧 Email: ${client.email.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodyMedium)
                    Text("📞 Call Support: ${client.phone}", style = MaterialTheme.typography.bodyMedium)
                    Text("⏰ Campaign Period: ${client.startDate} to ${client.endDate} (${client.durationDays} Days)", style = MaterialTheme.typography.bodyMedium)
                    if (client.notes.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Business Strategy Notes:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(client.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Client Media Assets Card
        item {
            Text("Client Media Assets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Client Business Logo Upload
                    MediaUploadZone(
                        label = "Client Business Logo",
                        currentUrl = client.logoUrl,
                        onUrlChanged = { newUrl ->
                            viewModel.saveClient(client.copy(logoUrl = newUrl))
                        },
                        viewModel = viewModel,
                        allowedMimeType = "image/*",
                        helpText = "Supports PNG, JPG, JPEG, WEBP"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Client Photos Header
                    Text(
                        text = "Client / Project Photos",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Display existing photos
                    val photoList = remember(client.photos) {
                        client.photos.split(",").filter { it.isNotBlank() }
                    }

                    if (photoList.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(photoList) { photoUrl ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray)
                                ) {
                                    SubcomposeAsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Client Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Delete overlay button
                                    IconButton(
                                        onClick = {
                                            val updatedPhotos = photoList.filter { it != photoUrl }.joinToString(",")
                                            viewModel.saveClient(client.copy(photos = updatedPhotos))
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                                            .size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No client photos uploaded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Upload new photo
                    MediaUploadZone(
                        label = "Add Client Photo / Graphic Asset",
                        currentUrl = "",
                        onUrlChanged = { newPhotoUrl ->
                            if (newPhotoUrl.isNotEmpty()) {
                                val updatedPhotos = if (client.photos.isBlank()) newPhotoUrl else "${client.photos},$newPhotoUrl"
                                viewModel.saveClient(client.copy(photos = updatedPhotos))
                            }
                        },
                        viewModel = viewModel,
                        allowedMimeType = "image/*",
                        helpText = "Upload PNG, JPG, JPEG or WEBP project files"
                    )
                }
            }
        }

        // Timeline log items
        item {
            Text("Client Action History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (logs.isEmpty()) {
            item {
                Text("No activities logged for this client yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else {
            items(logs) { log ->
                ActivityLogItem(log = log, onNavigateToClientProfile = {})
            }
        }
    }
}

// --- TAB 2: SERVICES & BILLS ---
@Composable
fun TabServicesAndBills(
    services: List<ClientService>,
    bills: List<Bill>,
    onAddService: () -> Unit,
    onDeleteService: (ClientService) -> Unit,
    onGenerateBill: () -> Unit,
    onDeleteBill: (Bill) -> Unit,
    onExportPdf: (Bill) -> Unit,
    onWhatsAppInvoice: (Bill) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Services table heading
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Associated Service Charges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddService) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add Service", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (services.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No custom services added. Tap + to add charges (Visiting Card, Banner Print, Website Design, etc.)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        } else {
            items(services) { srv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(srv.serviceName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Quantity: ${srv.quantity}  |  Rate: ₹${srv.rate}  |  Discount: ₹${srv.discount}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            if (srv.notes.isNotBlank()) {
                                Text("Notes: ${srv.notes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("₹${srv.total}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { onDeleteService(srv) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Bills/Invoice section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Invoices Generated", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onGenerateBill,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Invoice", fontSize = 11.sp, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (bills.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No invoices generated yet for this client.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        } else {
            items(bills) { bill ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Invoice #${bill.billNumber}", fontWeight = FontWeight.Bold)
                                Text("Date: ${bill.billDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            // Status badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (bill.paymentStatus.lowercase() == "paid") Color(0xFF00E676).copy(alpha = 0.15f)
                                        else Color(0xFFFFD600).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    bill.paymentStatus.uppercase(),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = if (bill.paymentStatus.lowercase() == "paid") Color(0xFF00E676) else Color(0xFFE65100)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total: ₹${bill.totalAmount}", fontSize = 13.sp)
                            Text("Paid: ₹${bill.advancePaid}", fontSize = 13.sp, color = Color(0xFF00E676))
                            Text("Pending: ₹${bill.pendingAmount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        if (bill.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Notes: ${bill.notes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Invoice Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onDeleteBill(bill) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (bill.proofUrl.isNotBlank()) {
                                    val context = LocalContext.current
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(bill.proofUrl))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View Proof", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                                Button(
                                    onClick = { onWhatsAppInvoice(bill) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { onExportPdf(bill) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF / Print", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 3: TASKS & PAYMENTS ---
@Composable
fun TabTasksAndPayments(
    tasks: List<Task>,
    payments: List<Payment>,
    onAddTask: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddPayment: () -> Unit,
    onDeletePayment: (Payment) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tasks heading
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Client Operations Checklist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddTask) {
                    Icon(Icons.Default.AddTask, contentDescription = "Add Task", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (tasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No pending campaign tasks.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        } else {
            items(tasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = task.status.lowercase() == "done",
                                onCheckedChange = { onToggleTask(task) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = task.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (task.status.lowercase() == "done") Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                                Text("Due: ${task.dueDate}  |  Priority: ${task.priority}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { onDeleteTask(task) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Payments heading
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Received Payments History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onAddPayment,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Payment", fontSize = 11.sp, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (payments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No payment transactions recorded yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        } else {
            items(payments) { pay ->
                val context = LocalContext.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Received: ₹${pay.amount}", fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                            Text("Date: ${pay.paymentDate}  |  Method: ${pay.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            if (pay.notes.isNotBlank()) {
                                Text("Ref: ${pay.notes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (pay.screenshotUrl.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.LightGray)
                                        .clickable {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(pay.screenshotUrl))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                ) {
                                    SubcomposeAsyncImage(
                                        model = pay.screenshotUrl,
                                        contentDescription = "Payment Proof",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            IconButton(onClick = { onDeletePayment(pay) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-DIALOGS ---

@Composable
fun AddServiceDialog(
    clientId: Int,
    onDismiss: () -> Unit,
    onAdd: (ClientService) -> Unit
) {
    val context = LocalContext.current
    var serviceName by remember { mutableStateOf("Reel Shooting") }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Service Charge") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    Text("Select Service Type:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))) {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                            items(defaultOptions) { opt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { serviceName = opt }
                                        .background(if (serviceName == opt) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(opt, fontWeight = if (serviceName == opt) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
                item {
                    if (serviceName == "Other") {
                        OutlinedTextField(
                            value = serviceName,
                            onValueChange = { serviceName = it },
                            label = { Text("Specify Service Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                        label = { Text("Rate (₹)") },
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
                        label = { Text("Service Line Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantity.toIntOrNull() ?: 1
                    val r = rate.toDoubleOrNull() ?: 0.0
                    val d = discount.toDoubleOrNull() ?: 0.0
                    val service = ClientService(
                        clientId = clientId,
                        serviceName = serviceName,
                        quantity = q,
                        rate = r,
                        discount = d,
                        total = (q * r) - d,
                        notes = notes
                    )
                    onAdd(service)
                }
            ) {
                Text("Add Charge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddPaymentDialog(
    clientId: Int,
    viewModel: CrmViewModel,
    onDismiss: () -> Unit,
    onAdd: (Payment) -> Unit
) {
    val context = LocalContext.current
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var amount by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf(simpleDateFormat.format(Date())) }
    var paymentMethod by remember { mutableStateOf("UPI") }
    var notes by remember { mutableStateOf("") }
    var screenshotUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount Received (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = { paymentDate = it },
                    label = { Text("Payment Date (YYYY-MM-DD)") },
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
                                    paymentDate = simpleDateFormat.format(sel.time)
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
                Text("Payment Method:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("UPI", "Cash", "Bank Transfer").forEach { method ->
                        ElevatedFilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Payment Reference / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                MediaUploadZone(
                    label = "Payment Proof Screenshot",
                    currentUrl = screenshotUrl,
                    onUrlChanged = { screenshotUrl = it },
                    viewModel = viewModel,
                    allowedMimeType = "image/*",
                    helpText = "Upload UPI or Bank transfer receipt screenshot"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dbl = amount.toDoubleOrNull()
                    if (dbl == null || dbl <= 0.0) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val payment = Payment(
                        clientId = clientId,
                        amount = dbl,
                        paymentDate = paymentDate,
                        paymentMethod = paymentMethod,
                        status = "Paid",
                        notes = notes,
                        screenshotUrl = screenshotUrl
                    )
                    onAdd(payment)
                }
            ) {
                Text("Save Transaction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddTaskDialog(
    clientId: Int,
    onDismiss: () -> Unit,
    onAdd: (Task) -> Unit
) {
    val context = LocalContext.current
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(simpleDateFormat.format(Date())) }
    var priority by remember { mutableStateOf("Medium") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Campaign Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Description / Action *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Target Due Date (YYYY-MM-DD)") },
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
                                    dueDate = simpleDateFormat.format(sel.time)
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
                Text("Priority Level:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High").forEach { pr ->
                        ElevatedFilterChip(
                            selected = priority == pr,
                            onClick = { priority = pr },
                            label = { Text(pr) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Specific Action Details") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Task action is required", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val task = Task(
                        clientId = clientId,
                        title = title.trim(),
                        dueDate = dueDate,
                        priority = priority,
                        status = "Pending",
                        notes = notes.trim()
                    )
                    onAdd(task)
                }
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GenerateBillDialog(
    clientId: Int,
    totalClientBilling: Double,
    advanceClientPaid: Double,
    viewModel: CrmViewModel,
    onDismiss: () -> Unit,
    onGenerate: (Bill) -> Unit
) {
    val context = LocalContext.current
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var billNumber by remember { mutableStateOf("INV-${1000 + Random().nextInt(9000)}") }
    var billDate by remember { mutableStateOf(simpleDateFormat.format(Date())) }
    var paymentStatus by remember { mutableStateOf("Pending") }
    var notes by remember { mutableStateOf("Thank you for your business!") }
    var proofUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Invoice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = billNumber,
                    onValueChange = { billNumber = it },
                    label = { Text("Invoice Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = billDate,
                    onValueChange = { billDate = it },
                    label = { Text("Invoice Date (YYYY-MM-DD)") },
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
                                    billDate = simpleDateFormat.format(sel.time)
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
                Text("Invoice Status:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Paid", "Partial", "Pending").forEach { st ->
                        ElevatedFilterChip(
                            selected = paymentStatus == st,
                            onClick = { paymentStatus = st },
                            label = { Text(st) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Invoice Footer Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                MediaUploadZone(
                    label = "Bill / Campaign Proof Document",
                    currentUrl = proofUrl,
                    onUrlChanged = { proofUrl = it },
                    viewModel = viewModel,
                    allowedMimeType = "*/*",
                    helpText = "Upload PNG, JPG, JPEG, WEBP or PDF proof"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (billNumber.isBlank()) {
                        Toast.makeText(context, "Invoice Number is required", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val bill = Bill(
                        billNumber = billNumber.trim(),
                        billDate = billDate,
                        clientId = clientId,
                        totalAmount = totalClientBilling,
                        advancePaid = advanceClientPaid,
                        pendingAmount = totalClientBilling - advanceClientPaid,
                        paymentStatus = paymentStatus,
                        notes = notes.trim(),
                        proofUrl = proofUrl
                    )
                    onGenerate(bill)
                }
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
