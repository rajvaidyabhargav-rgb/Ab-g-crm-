package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import com.example.viewmodel.CrmViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClientsScreen(
    viewModel: CrmViewModel,
    onNavigateToClientProfile: (Int) -> Unit
) {
    val context = LocalContext.current
    val clientsList by viewModel.clients.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<Client?>(null) }

    // Filter list
    val filteredClients = clientsList.filter { client ->
        val matchesSearch = client.name.contains(searchQuery, ignoreCase = true) ||
                client.businessName.contains(searchQuery, ignoreCase = true) ||
                client.phone.contains(searchQuery)
        val matchesStatus = selectedStatusFilter == "All" || client.status == selectedStatusFilter
        matchesSearch && matchesStatus
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingClient = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Client")
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
            // Title Header
            Text(
                text = "Client Portfolios",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Search Bar & Filter Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, business, phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Status Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statuses = listOf("All", "Active", "Inactive", "Completed")
                statuses.forEach { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(status) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Clients List
            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No clients found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        ClientListItem(
                            client = client,
                            onClick = { onNavigateToClientProfile(client.id) },
                            onLongClick = {
                                editingClient = client
                                showAddEditDialog = true
                            },
                            onDelete = {
                                viewModel.deleteClient(client)
                                Toast.makeText(context, "Client Deleted: ${client.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Add/Edit Client Dialog Form ---
    if (showAddEditDialog) {
        ClientAddEditDialog(
            client = editingClient,
            onDismiss = { showAddEditDialog = false },
            onSave = { client ->
                viewModel.saveClient(client)
                showAddEditDialog = false
                Toast.makeText(context, if (editingClient == null) "Client Added" else "Client Updated", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClientListItem(
    client: Client,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (client.status.lowercase()) {
                                    "active" -> Color(0xFF00E676).copy(alpha = 0.15f)
                                    "completed" -> Color(0xFF00B0FF).copy(alpha = 0.15f)
                                    else -> Color.Gray.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = client.status.uppercase(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (client.status.lowercase()) {
                                    "active" -> Color(0xFF00E676)
                                    "completed" -> Color(0xFF00B0FF)
                                    else -> Color.Gray
                                }
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = client.businessName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = client.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Due: ${client.endDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open Profile") },
                        leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Client Details") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onLongClick()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete Client", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ClientAddEditDialog(
    client: Client?,
    onDismiss: () -> Unit,
    onSave: (Client) -> Unit
) {
    val context = LocalContext.current
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var name by remember { mutableStateOf(client?.name ?: "") }
    var businessName by remember { mutableStateOf(client?.businessName ?: "") }
    var phone by remember { mutableStateOf(client?.phone ?: "") }
    var whatsapp by remember { mutableStateOf(client?.whatsapp ?: "") }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var address by remember { mutableStateOf(client?.address ?: "") }
    var status by remember { mutableStateOf(client?.status ?: "Active") }
    var packageName by remember { mutableStateOf(client?.packageName ?: "") }
    var durationDays by remember { mutableStateOf(client?.durationDays?.toString() ?: "30") }
    var startDate by remember { mutableStateOf(client?.startDate?.ifBlank { simpleDateFormat.format(Date()) } ?: simpleDateFormat.format(Date())) }
    var endDate by remember { mutableStateOf(client?.endDate?.ifBlank { "" } ?: "") }
    var notes by remember { mutableStateOf(client?.notes ?: "") }
    var totalAmount by remember { mutableStateOf(client?.totalAmount?.toString() ?: "0.0") }
    var advancePaid by remember { mutableStateOf(client?.advancePaid?.toString() ?: "0.0") }

    // Recalculate end date based on start date and duration days automatically
    LaunchedEffect(startDate, durationDays) {
        try {
            val start = simpleDateFormat.parse(startDate)
            val duration = durationDays.toIntOrNull() ?: 30
            val calendar = Calendar.getInstance().apply {
                time = start
                add(Calendar.DAY_OF_YEAR, duration)
            }
            endDate = simpleDateFormat.format(calendar.time)
        } catch (e: Exception) {
            // ignore
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (client == null) "New Client Portfolio" else "Edit Client Portfolio") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Client Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Phone *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Office Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    // Status picker
                    Text("Service Status:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Active", "Inactive", "Completed").forEach { itemStatus ->
                            ElevatedFilterChip(
                                selected = status == itemStatus,
                                onClick = { status = itemStatus },
                                label = { Text(itemStatus) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Initial Package / Campaign Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { durationDays = it },
                        label = { Text("Duration (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    // Start Date date picker dialog launcher
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Campaign Start Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                try {
                                    val parsed = simpleDateFormat.parse(startDate)
                                    cal.time = parsed
                                } catch (e: Exception) {}
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        startDate = simpleDateFormat.format(selectedCal.time)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                            }
                        }
                    )
                }
                item {
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("Campaign End Date (Auto calculated)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = { Text("Total Bill Amount (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = advancePaid,
                        onValueChange = { advancePaid = it },
                        label = { Text("Advance Amount Received (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Campaign / Business Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || businessName.isBlank() || phone.isBlank()) {
                        Toast.makeText(context, "Please fill required fields Name, Business & Phone", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val doubleTotal = totalAmount.toDoubleOrNull() ?: 0.0
                    val doubleAdvance = advancePaid.toDoubleOrNull() ?: 0.0
                    val days = durationDays.toIntOrNull() ?: 30
                    val updatedClient = Client(
                        id = client?.id ?: 0,
                        name = name.trim(),
                        businessName = businessName.trim(),
                        phone = phone.trim(),
                        whatsapp = whatsapp.trim().ifBlank { phone.trim() },
                        email = email.trim(),
                        address = address.trim(),
                        status = status,
                        packageName = packageName.trim(),
                        durationDays = days,
                        startDate = startDate,
                        endDate = endDate,
                        notes = notes.trim(),
                        totalAmount = doubleTotal,
                        advancePaid = doubleAdvance,
                        pendingAmount = doubleTotal - doubleAdvance
                    )
                    onSave(updatedClient)
                }
            ) {
                Text("Save Portfolio")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
