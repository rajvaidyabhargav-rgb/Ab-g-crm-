package com.example.data

import android.content.Context
import android.util.Log
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class CrmRepository(private val crmDao: CrmDao) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // --- Flows ---
    val allClients: Flow<List<Client>> = crmDao.getAllClientsFlow()
    val allPackageTemplates: Flow<List<PackageTemplate>> = crmDao.getAllPackageTemplatesFlow()
    val allTasks: Flow<List<Task>> = crmDao.getAllTasksFlow()
    val allBills: Flow<List<Bill>> = crmDao.getAllBillsFlow()
    val allPayments: Flow<List<Payment>> = crmDao.getAllPaymentsFlow()
    val allExpenses: Flow<List<Expense>> = crmDao.getAllExpensesFlow()
    val allSettings: Flow<Settings?> = crmDao.getSettingsFlow()
    val allActivityLogs: Flow<List<ActivityLog>> = crmDao.getAllActivityLogsFlow()

    fun getClientByIdFlow(clientId: Int): Flow<Client?> = crmDao.getClientByIdFlow(clientId)
    fun getClientServicesFlow(clientId: Int): Flow<List<ClientService>> = crmDao.getClientServicesFlow(clientId)
    fun getTasksByClientFlow(clientId: Int): Flow<List<Task>> = crmDao.getTasksByClientFlow(clientId)
    fun getBillsByClientFlow(clientId: Int): Flow<List<Bill>> = crmDao.getBillsByClientFlow(clientId)
    fun getPaymentsByClientFlow(clientId: Int): Flow<List<Payment>> = crmDao.getPaymentsByClientFlow(clientId)
    fun getActivityLogsByClientFlow(clientId: Int): Flow<List<ActivityLog>> = crmDao.getActivityLogsByClientFlow(clientId)

    // --- Helper to execute tasks and handle errors ---
    private fun runWithSync(action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
            } catch (e: Exception) {
                Log.e("CrmRepository", "Error in sync action", e)
            }
        }
    }

    // --- Settings initialization ---
    suspend fun getOrInitSettings(): Settings {
        val current = crmDao.getSettings()
        if (current == null) {
            val defaultSettings = Settings()
            crmDao.insertSettings(defaultSettings)
            return defaultSettings
        }
        return current
    }

    suspend fun updateSettings(settings: Settings) {
        crmDao.insertSettings(settings)
        logActivity("Settings Updated", "CRM configurations and connection keys updated.")
        
        // Push settings to Supabase if configured
        runWithSync {
            val json = JSONObject().apply {
                put("id", settings.id)
                put("agencyName", settings.agencyName)
                put("agencyPhone", settings.agencyPhone)
                put("agencyAddress", settings.agencyAddress)
                put("upiId", settings.upiId)
                put("logoUrl", settings.logoUrl)
                put("qrUrl", settings.qrUrl)
                put("telegramToken", settings.telegramToken)
                put("telegramChatId", settings.telegramChatId)
                put("telegramEnabled", settings.telegramEnabled)
            }
            SupabaseClient.insertRow("settings", json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    // --- Activity Log Helper ---
    suspend fun logActivity(title: String, description: String, clientId: Int? = null) {
        val log = ActivityLog(title = title, description = description, clientId = clientId)
        crmDao.insertActivityLog(log)
        
        // Push activity to Supabase if configured
        val settings = crmDao.getSettings() ?: return
        if (settings.supabaseUrl.isNotBlank() && settings.supabaseKey.isNotBlank()) {
            runWithSync {
                val json = JSONObject().apply {
                    put("title", log.title)
                    put("description", log.description)
                    put("timestamp", log.timestamp)
                    put("clientId", log.clientId ?: JSONObject.NULL)
                }
                SupabaseClient.insertRow("activity_logs", json.toString(), settings.supabaseUrl, settings.supabaseKey)
            }
        }
    }

    // --- Telegram Helper ---
    private suspend fun triggerTelegramNotification(message: String) {
        val settings = crmDao.getSettings() ?: return
        if (settings.telegramEnabled && settings.telegramToken.isNotBlank() && settings.telegramChatId.isNotBlank()) {
            SupabaseClient.sendTelegramMessage(
                token = settings.telegramToken,
                chatId = settings.telegramChatId,
                message = message
            )
        }
    }

    // --- Clients ---
    suspend fun insertClient(client: Client) {
        val calculatedPending = client.totalAmount - client.advancePaid
        val finalClient = client.copy(pendingAmount = calculatedPending)
        val generatedId = crmDao.insertClient(finalClient).toInt()
        
        logActivity("Client Added", "Added client ${finalClient.name} for business ${finalClient.businessName}.", generatedId)

        // Telegram alert with ALL fields
        val message = """
            <b>🟢 NEW CLIENT ADDED</b>
            <b>Name:</b> ${finalClient.name}
            <b>Business Name:</b> ${finalClient.businessName}
            <b>Phone:</b> ${finalClient.phone}
            <b>WhatsApp:</b> ${finalClient.whatsapp}
            <b>Email:</b> ${finalClient.email}
            <b>Address:</b> ${finalClient.address.ifBlank { "N/A" }}
            <b>Status:</b> ${finalClient.status}
            <b>Package Name:</b> ${finalClient.packageName}
            <b>Duration:</b> ${finalClient.durationDays} Days
            <b>Start Date:</b> ${finalClient.startDate}
            <b>End Date:</b> ${finalClient.endDate}
            <b>Total Billing:</b> ₹${finalClient.totalAmount}
            <b>Advance Paid:</b> ₹${finalClient.advancePaid}
            <b>Pending Amount:</b> ₹$calculatedPending
            <b>Notes:</b> ${finalClient.notes.ifBlank { "N/A" }}
        """.trimIndent()
        triggerTelegramNotification(message)

        // Supabase Sync
        val settings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("id", generatedId)
                put("name", finalClient.name)
                put("businessName", finalClient.businessName)
                put("phone", finalClient.phone)
                put("whatsapp", finalClient.whatsapp)
                put("email", finalClient.email)
                put("address", finalClient.address)
                put("status", finalClient.status)
                put("packageName", finalClient.packageName)
                put("durationDays", finalClient.durationDays)
                put("startDate", finalClient.startDate)
                put("endDate", finalClient.endDate)
                put("notes", finalClient.notes)
                put("totalAmount", finalClient.totalAmount)
                put("advancePaid", finalClient.advancePaid)
                put("pendingAmount", calculatedPending)
                put("logoUrl", finalClient.logoUrl)
                put("photos", finalClient.photos)
            }
            SupabaseClient.insertRow("clients", json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    suspend fun updateClient(client: Client) {
        val oldClient = crmDao.getClientById(client.id)
        val calculatedPending = client.totalAmount - client.advancePaid
        val finalClient = client.copy(pendingAmount = calculatedPending)
        crmDao.updateClient(finalClient)

        logActivity("Client Edited", "Updated client details for ${finalClient.name}.", finalClient.id)

        // Telegram alert showing changed values
        val message = buildString {
            appendLine("<b>🟡 CLIENT UPDATED</b>")
            appendLine("<b>Name:</b> ${finalClient.name}")
            appendLine("<b>Business:</b> ${finalClient.businessName}")
            appendLine("")
            appendLine("<b>CHANGES:</b>")
            if (oldClient != null) {
                var changed = false
                if (oldClient.name != finalClient.name) { appendLine("• Name: ${oldClient.name} ➡️ ${finalClient.name}"); changed = true }
                if (oldClient.businessName != finalClient.businessName) { appendLine("• Business: ${oldClient.businessName} ➡️ ${finalClient.businessName}"); changed = true }
                if (oldClient.phone != finalClient.phone) { appendLine("• Phone: ${oldClient.phone} ➡️ ${finalClient.phone}"); changed = true }
                if (oldClient.whatsapp != finalClient.whatsapp) { appendLine("• WhatsApp: ${oldClient.whatsapp} ➡️ ${finalClient.whatsapp}"); changed = true }
                if (oldClient.email != finalClient.email) { appendLine("• Email: ${oldClient.email} ➡️ ${finalClient.email}"); changed = true }
                if (oldClient.address != finalClient.address) { appendLine("• Address: ${oldClient.address.ifBlank { "N/A" }} ➡️ ${finalClient.address.ifBlank { "N/A" }}"); changed = true }
                if (oldClient.status != finalClient.status) { appendLine("• Status: ${oldClient.status} ➡️ ${finalClient.status}"); changed = true }
                if (oldClient.packageName != finalClient.packageName) { appendLine("• Package: ${oldClient.packageName} ➡️ ${finalClient.packageName}"); changed = true }
                if (oldClient.durationDays != finalClient.durationDays) { appendLine("• Duration: ${oldClient.durationDays} Days ➡️ ${finalClient.durationDays} Days"); changed = true }
                if (oldClient.startDate != finalClient.startDate) { appendLine("• Start Date: ${oldClient.startDate} ➡️ ${finalClient.startDate}"); changed = true }
                if (oldClient.endDate != finalClient.endDate) { appendLine("• End Date: ${oldClient.endDate} ➡️ ${finalClient.endDate}"); changed = true }
                if (oldClient.totalAmount != finalClient.totalAmount) { appendLine("• Total Billing: ₹${oldClient.totalAmount} ➡️ ₹${finalClient.totalAmount}"); changed = true }
                if (oldClient.advancePaid != finalClient.advancePaid) { appendLine("• Advance Paid: ₹${oldClient.advancePaid} ➡️ ₹${finalClient.advancePaid}"); changed = true }
                if (oldClient.pendingAmount != finalClient.pendingAmount) { appendLine("• Pending: ₹${oldClient.pendingAmount} ➡️ ₹${finalClient.pendingAmount}"); changed = true }
                if (!changed) {
                    appendLine("• No specific field values were changed (saved without modifications).")
                }
            } else {
                appendLine("• Previous values not available in local cache.")
            }
        }
        triggerTelegramNotification(message)

        // Check if package or expiry date changed, indicating a package renewal
        if (oldClient != null && (oldClient.endDate != finalClient.endDate || oldClient.packageName != finalClient.packageName)) {
            val renewMsg = """
                <b>🔄 PACKAGE RENEWED</b>
                <b>Client:</b> ${finalClient.name} &amp; ${finalClient.businessName}
                <b>Old Package:</b> ${oldClient.packageName} (${oldClient.durationDays} Days)
                <b>New Package:</b> ${finalClient.packageName} (${finalClient.durationDays} Days)
                <b>Total Billing:</b> ₹${finalClient.totalAmount}
                <b>Campaign Term:</b> ${finalClient.startDate} to ${finalClient.endDate}
            """.trimIndent()
            triggerTelegramNotification(renewMsg)
        }

        // Supabase Sync
        val settings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("name", finalClient.name)
                put("businessName", finalClient.businessName)
                put("phone", finalClient.phone)
                put("whatsapp", finalClient.whatsapp)
                put("email", finalClient.email)
                put("address", finalClient.address)
                put("status", finalClient.status)
                put("packageName", finalClient.packageName)
                put("durationDays", finalClient.durationDays)
                put("startDate", finalClient.startDate)
                put("endDate", finalClient.endDate)
                put("notes", finalClient.notes)
                put("totalAmount", finalClient.totalAmount)
                put("advancePaid", finalClient.advancePaid)
                put("pendingAmount", calculatedPending)
                put("logoUrl", finalClient.logoUrl)
                put("photos", finalClient.photos)
            }
            SupabaseClient.updateRow("clients", finalClient.id, json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    suspend fun deleteClient(client: Client) {
        crmDao.deleteClient(client)
        
        // Cascading deletes locally to keep data integrated cleanly
        crmDao.deleteServicesByClientId(client.id)
        crmDao.deleteTasksByClientId(client.id)
        crmDao.deleteBillsByClientId(client.id)
        crmDao.deletePaymentsByClientId(client.id)
        crmDao.deleteActivityLogsByClientId(client.id)

        logActivity("Client Deleted", "Deleted client ${client.name} and all related services, bills, and tasks.")

        // Telegram alert
        val message = """
            <b>🔴 CLIENT DELETED</b>
            <b>Name:</b> ${client.name}
            <b>Business:</b> ${client.businessName}
            All associated bills, tasks, and payments have been removed.
        """.trimIndent()
        triggerTelegramNotification(message)

        // Supabase Sync
        val settings = crmDao.getSettings() ?: return
        runWithSync {
            SupabaseClient.deleteRow("clients", client.id, settings.supabaseUrl, settings.supabaseKey)
            // also trigger cascading deletes in Supabase if relationships are not CASCADE
            SupabaseClient.deleteRow("client_services", client.id, settings.supabaseUrl, settings.supabaseKey) // assuming matching client_id
        }
    }

    // --- Client Services ---
    suspend fun insertClientService(service: ClientService) {
        val total = (service.quantity * service.rate) - service.discount
        val finalService = service.copy(total = total)
        val generatedId = crmDao.insertClientService(finalService).toInt()

        val client = crmDao.getClientById(service.clientId)
        logActivity("Service Added", "Added service '${service.serviceName}' to client ${client?.name ?: "Unknown"}.", service.clientId)

        // Trigger updates to client total amounts automatically
        recalculateClientFinancials(service.clientId)

        // Telegram
        triggerTelegramNotification("<b>💼 SERVICE ADDED</b>\n<b>Client:</b> ${client?.name ?: "Unknown"}\n<b>Service:</b> ${service.serviceName}\n<b>Total:</b> ₹$total")

        // Supabase
        val settings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("id", generatedId)
                put("clientId", finalService.clientId)
                put("serviceName", finalService.serviceName)
                put("quantity", finalService.quantity)
                put("rate", finalService.rate)
                put("discount", finalService.discount)
                put("total", total)
                put("notes", finalService.notes)
            }
            SupabaseClient.insertRow("client_services", json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    suspend fun deleteClientService(service: ClientService) {
        crmDao.deleteClientService(service)
        val client = crmDao.getClientById(service.clientId)
        logActivity("Service Deleted", "Removed service '${service.serviceName}' from client ${client?.name ?: "Unknown"}.", service.clientId)

        recalculateClientFinancials(service.clientId)

        // Telegram
        triggerTelegramNotification("<b>🗑️ SERVICE DELETED</b>\n<b>Client:</b> ${client?.name ?: "Unknown"}\n<b>Service:</b> ${service.serviceName}")

        // Supabase
        val settings = crmDao.getSettings() ?: return
        runWithSync {
            SupabaseClient.deleteRow("client_services", service.id, settings.supabaseUrl, settings.supabaseKey)
        }
    }

    private suspend fun recalculateClientFinancials(clientId: Int) {
        val services = crmDao.getClientServices(clientId)
        val sumTotal = services.sumOf { it.total }
        val payments = crmDao.getPaymentsByClient(clientId)
        val sumPayments = payments.sumOf { it.amount }

        val client = crmDao.getClientById(clientId) ?: return
        val updatedClient = client.copy(
            totalAmount = sumTotal,
            advancePaid = sumPayments,
            pendingAmount = sumTotal - sumPayments
        )
        crmDao.updateClient(updatedClient)
    }

    // --- Package Templates ---
    suspend fun insertPackageTemplate(template: PackageTemplate) {
        crmDao.insertPackageTemplate(template)
        logActivity("Template Saved", "Saved template '${template.templateName}' for service '${template.serviceName}'.")
        
        // Supabase Sync
        val settings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("templateName", template.templateName)
                put("serviceName", template.serviceName)
                put("quantity", template.quantity)
                put("rate", template.rate)
                put("discount", template.discount)
                put("notes", template.notes)
            }
            SupabaseClient.insertRow("package_templates", json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    suspend fun deletePackageTemplate(template: PackageTemplate) {
        crmDao.deletePackageTemplate(template)
        logActivity("Template Deleted", "Deleted template '${template.templateName}'.")

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            SupabaseClient.deleteRow("package_templates", template.id, settings.supabaseUrl, settings.supabaseKey)
        }
    }

    // --- Tasks ---
    suspend fun insertTask(task: Task) {
        val generatedId = crmDao.insertTask(task).toInt()
        val client = crmDao.getClientById(task.clientId)
        logActivity("Task Added", "Added task '${task.title}' for client ${client?.name ?: "Unknown"}.", task.clientId)

        val message = """
            <b>📋 TASK ADDED</b>
            <b>Title:</b> ${task.title}
            <b>Due Date:</b> ${task.dueDate}
            <b>Priority:</b> ${task.priority}
            <b>Client Name:</b> ${client?.name ?: "Unknown"}
            <b>Notes:</b> ${task.notes.ifBlank { "N/A" }}
        """.trimIndent()
        triggerTelegramNotification(message)

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("id", generatedId)
                put("title", task.title)
                put("clientId", task.clientId)
                put("dueDate", task.dueDate)
                put("priority", task.priority)
                put("status", task.status)
                put("notes", task.notes)
            }
            SupabaseClient.insertRow("tasks", json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    suspend fun updateTask(task: Task) {
        val oldTask = crmDao.getTaskById(task.id)
        crmDao.updateTask(task)
        val client = crmDao.getClientById(task.clientId)
        logActivity("Task Updated", "Updated task '${task.title}' status to ${task.status}.", task.clientId)

        val message = if (task.status.lowercase() == "completed" && oldTask?.status?.lowercase() != "completed") {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            """
                <b>✅ TASK COMPLETED</b>
                <b>Title:</b> ${task.title}
                <b>Completed Date:</b> ${sdf.format(java.util.Date())}
                <b>Client Name:</b> ${client?.name ?: "Unknown"}
            """.trimIndent()
        } else {
            """
                <b>🔔 TASK UPDATE</b>
                <b>Task:</b> ${task.title}
                <b>Client:</b> ${client?.name ?: "Unknown"}
                <b>Status:</b> ${task.status}
            """.trimIndent()
        }
        triggerTelegramNotification(message)

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("title", task.title)
                put("clientId", task.clientId)
                put("dueDate", task.dueDate)
                put("priority", task.priority)
                put("status", task.status)
                put("notes", task.notes)
            }
            SupabaseClient.updateRow("tasks", task.id, json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    suspend fun deleteTask(task: Task) {
        crmDao.deleteTask(task)
        logActivity("Task Deleted", "Deleted task '${task.title}'.", task.clientId)

        triggerTelegramNotification("<b>🗑️ TASK DELETED</b>\n<b>Task:</b> ${task.title}")

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            SupabaseClient.deleteRow("tasks", task.id, settings.supabaseUrl, settings.supabaseKey)
        }
    }

    // --- Bills ---
    suspend fun insertBill(bill: Bill) {
        val generatedId = crmDao.insertBill(bill).toInt()
        val client = crmDao.getClientById(bill.clientId)
        val services = crmDao.getClientServices(bill.clientId)
        val settings = crmDao.getSettings()
        
        logActivity("Bill Generated", "Invoice ${bill.billNumber} generated for client ${client?.name ?: "Unknown"}.", bill.clientId)

        val billingList = if (services.isEmpty()) {
            "• ${client?.packageName ?: "Graphic Design Package"}: 1 x ₹${bill.totalAmount} = ₹${bill.totalAmount}"
        } else {
            services.joinToString("\n") { srv ->
                "• ${srv.serviceName}: ${srv.quantity} x ₹${srv.rate} (Disc: ₹${srv.discount}) = ₹${srv.total}"
            }
        }

        val upiScanLink = "https://upiqr.in/api/qr?vpa=${settings?.upiId?.ifBlank { "abgraphics@upi" }}&amp;amount=${bill.pendingAmount}&amp;name=${Uri.encode(settings?.agencyName?.ifBlank { "AB Graphics" })}"

        val message = """
            <b>🧾 BILL GENERATED</b>
            <b>Bill Number:</b> ${bill.billNumber}
            <b>Client Name:</b> ${client?.name ?: "Unknown"}
            <b>Business Name:</b> ${client?.businessName ?: "Unknown"}
            
            <b>Billing Itemized List:</b>
            $billingList
            
            <b>Invoice Total:</b> ₹${bill.totalAmount}
            <b>Paid Amount:</b> ₹${bill.advancePaid}
            <b>Pending Balance:</b> ₹${bill.pendingAmount}
            <b>Due Date / Campaign Term:</b> ${client?.startDate ?: "N/A"} to ${client?.endDate ?: "N/A"}
            
            <b>UPI Payment QR Scan Link:</b>
            <a href="$upiScanLink">Scan to Pay ₹${bill.pendingAmount}</a>
        """.trimIndent()
        triggerTelegramNotification(message)

        val syncSettings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("id", generatedId)
                put("billNumber", bill.billNumber)
                put("billDate", bill.billDate)
                put("clientId", bill.clientId)
                put("totalAmount", bill.totalAmount)
                put("advancePaid", bill.advancePaid)
                put("pendingAmount", bill.pendingAmount)
                put("paymentStatus", bill.paymentStatus)
                put("notes", bill.notes)
                put("proofUrl", bill.proofUrl)
            }
            SupabaseClient.insertRow("bills", json.toString(), syncSettings.supabaseUrl, syncSettings.supabaseKey)
        }
    }

    suspend fun updateBill(bill: Bill) {
        crmDao.updateBill(bill)
        val client = crmDao.getClientById(bill.clientId)
        logActivity("Bill Updated", "Invoice ${bill.billNumber} details updated.", bill.clientId)

        val message = """
            <b>📝 BILL UPDATED</b>
            <b>Bill Number:</b> ${bill.billNumber}
            <b>Client:</b> ${client?.name ?: "Unknown"}
            <b>Total Amount:</b> ₹${bill.totalAmount}
            <b>Status:</b> ${bill.paymentStatus}
        """.trimIndent()
        triggerTelegramNotification(message)

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("billNumber", bill.billNumber)
                put("billDate", bill.billDate)
                put("clientId", bill.clientId)
                put("totalAmount", bill.totalAmount)
                put("advancePaid", bill.advancePaid)
                put("pendingAmount", bill.pendingAmount)
                put("paymentStatus", bill.paymentStatus)
                put("notes", bill.notes)
                put("proofUrl", bill.proofUrl)
            }
            SupabaseClient.updateRow("bills", bill.id, json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    suspend fun deleteBill(bill: Bill) {
        crmDao.deleteBill(bill)
        logActivity("Bill Deleted", "Deleted invoice ${bill.billNumber}.", bill.clientId)

        triggerTelegramNotification("<b>🗑️ BILL DELETED</b>\n<b>Bill Number:</b> ${bill.billNumber}")

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            SupabaseClient.deleteRow("bills", bill.id, settings.supabaseUrl, settings.supabaseKey)
        }
    }

    // --- Payments ---
    suspend fun insertPayment(payment: Payment) {
        val clientBefore = crmDao.getClientById(payment.clientId)
        val generatedId = crmDao.insertPayment(payment).toInt()
        val client = crmDao.getClientById(payment.clientId)
        logActivity("Payment Saved", "Received payment of ₹${payment.amount} via ${payment.paymentMethod} from client ${client?.name ?: "Unknown"}.", payment.clientId)

        recalculateClientFinancials(payment.clientId)

        val clientAfter = crmDao.getClientById(payment.clientId)
        val previousPaid = clientBefore?.advancePaid ?: 0.0
        val newPaid = clientAfter?.advancePaid ?: (previousPaid + payment.amount)
        val remainingBalance = clientAfter?.pendingAmount ?: ((clientBefore?.totalAmount ?: 0.0) - newPaid)

        val message = """
            <b>💰 PAYMENT RECEIVED</b>
            <b>Client:</b> ${clientAfter?.name ?: "Unknown"}
            <b>Business Name:</b> ${clientAfter?.businessName ?: "Unknown"}
            <b>Total Billing:</b> ₹${clientAfter?.totalAmount ?: 0.0}
            <b>Previous Paid:</b> ₹$previousPaid
            <b>New Paid (Incl. this):</b> ₹$newPaid
            <b>Remaining Balance:</b> ₹$remainingBalance
            <b>Payment Mode:</b> ${payment.paymentMethod}
            <b>Reference Notes:</b> ${payment.notes.ifBlank { "N/A" }}
            <b>Date:</b> ${payment.paymentDate}
        """.trimIndent()
        triggerTelegramNotification(message)

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("id", generatedId)
                put("clientId", payment.clientId)
                put("amount", payment.amount)
                put("paymentDate", payment.paymentDate)
                put("paymentMethod", payment.paymentMethod)
                put("status", payment.status)
                put("notes", payment.notes)
                put("screenshotUrl", payment.screenshotUrl)
            }
            SupabaseClient.insertRow("payments", json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    // --- Expiring Plans Utility ---
    suspend fun pushExpiringPlansAlert(): Int {
        val allClientsList = crmDao.getAllClients()
        val expiringClients = allClientsList.filter {
            it.status.lowercase() == "active" && isExpiringIn3DaysLocal(it.endDate)
        }
        
        var count = 0
        for (client in expiringClients) {
            val waLink = "https://api.whatsapp.com/send?phone=${client.whatsapp}&amp;text=${Uri.encode("Hello ${client.name}, this is a friendly reminder that your AB Graphics plan for '${client.packageName}' is expiring on ${client.endDate}. Please renew soon!")}"
            val message = """
                <b>⚠️ PLAN EXPIRING IN 3 DAYS</b>
                <b>Client Name:</b> ${client.name}
                <b>Business Name:</b> ${client.businessName}
                <b>Package:</b> ${client.packageName}
                <b>Duration:</b> ${client.durationDays} Days
                <b>Expiry Date:</b> ${client.endDate}
                
                <b>WhatsApp Remind Link:</b>
                <a href="$waLink">Send WhatsApp Reminder</a>
            """.trimIndent()
            triggerTelegramNotification(message)
            count++
        }
        return count
    }

    private fun isExpiringIn3DaysLocal(endDateStr: String): Boolean {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val end = sdf.parse(endDateStr) ?: return false
            val today = sdf.parse(sdf.format(java.util.Date())) ?: return false
            val diff = end.time - today.time
            val days = diff / (1000 * 60 * 60 * 24)
            return days in 0..3
        } catch (_: Exception) {}
        return false
    }

    suspend fun deletePayment(payment: Payment) {
        crmDao.deletePayment(payment)
        val client = crmDao.getClientById(payment.clientId)
        logActivity("Payment Deleted", "Deleted payment of ₹${payment.amount} for ${client?.name ?: "Unknown"}.", payment.clientId)

        recalculateClientFinancials(payment.clientId)

        triggerTelegramNotification("<b>🗑️ PAYMENT REMOVED</b>\n<b>Client:</b> ${client?.name ?: "Unknown"}\n<b>Amount:</b> ₹${payment.amount}")

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            SupabaseClient.deleteRow("payments", payment.id, settings.supabaseUrl, settings.supabaseKey)
        }
    }

    // --- Expenses ---
    suspend fun insertExpense(expense: Expense) {
        val generatedId = crmDao.insertExpense(expense).toInt()
        logActivity("Expense Added", "Recorded expense for '${expense.title}' of ₹${expense.amount} under category '${expense.category}'.")

        triggerTelegramNotification("<b>💸 EXPENSE ADDED</b>\n<b>Title:</b> ${expense.title}\n<b>Category:</b> ${expense.category}\n<b>Amount:</b> ₹${expense.amount}")

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("id", generatedId)
                put("title", expense.title)
                put("category", expense.category)
                put("amount", expense.amount)
                put("date", expense.date)
                put("notes", expense.notes)
            }
            SupabaseClient.insertRow("expenses", json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    suspend fun updateExpense(expense: Expense) {
        crmDao.updateExpense(expense)
        logActivity("Expense Updated", "Updated expense details for '${expense.title}'.")

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            val json = JSONObject().apply {
                put("title", expense.title)
                put("category", expense.category)
                put("amount", expense.amount)
                put("date", expense.date)
                put("notes", expense.notes)
            }
            SupabaseClient.updateRow("expenses", expense.id, json.toString(), settings.supabaseUrl, settings.supabaseKey)
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        crmDao.deleteExpense(expense)
        logActivity("Expense Deleted", "Removed expense record for '${expense.title}'.")

        triggerTelegramNotification("<b>🗑️ EXPENSE DELETED</b>\n<b>Title:</b> ${expense.title}\n<b>Amount:</b> ₹${expense.amount}")

        val settings = crmDao.getSettings() ?: return
        runWithSync {
            SupabaseClient.deleteRow("expenses", expense.id, settings.supabaseUrl, settings.supabaseKey)
        }
    }

    // --- Upload Assets to Supabase Storage ---
    suspend fun uploadAsset(fileName: String, fileBytes: ByteArray, mimeType: String): String? {
        val settings = crmDao.getSettings() ?: return null
        if (settings.supabaseUrl.isBlank() || settings.supabaseKey.isBlank()) return null
        
        return SupabaseClient.uploadFile(
            bucketName = "crm_assets",
            fileName = fileName,
            fileBytes = fileBytes,
            mimeType = mimeType,
            supabaseUrl = settings.supabaseUrl,
            supabaseKey = settings.supabaseKey
        )
    }

    // --- Trigger Manual Telegram Push ---
    suspend fun pushClientTelegram(clientId: Int): Boolean {
        val client = crmDao.getClientById(clientId) ?: return false
        val message = """
            <b>📢 TELEGRAM PUSH: CLIENT PROFILE</b>
            <b>Name:</b> ${client.name}
            <b>Business Name:</b> ${client.businessName}
            <b>Status:</b> ${client.status}
            <b>Phone:</b> ${client.phone}
            <b>Email:</b> ${client.email}
            <b>Address:</b> ${client.address}
            <b>Package Name:</b> ${client.packageName}
            <b>Total Billing:</b> ₹${client.totalAmount}
            <b>Advance Received:</b> ₹${client.advancePaid}
            <b>Pending Balance:</b> ₹${client.pendingAmount}
            <b>Start Date:</b> ${client.startDate}
            <b>End Date:</b> ${client.endDate}
            <b>Notes:</b> ${client.notes}
        """.trimIndent()
        triggerTelegramNotification(message)
        logActivity("Telegram Profile Pushed", "Manually pushed client ${client.name}'s profile alert to Telegram chat.", clientId)
        return true
    }
}
