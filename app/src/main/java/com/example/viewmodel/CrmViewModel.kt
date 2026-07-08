package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CrmViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = CrmDatabase.getDatabase(context)
    private val repository = CrmRepository(database.crmDao())

    // --- State Flows ---
    val clients = repository.allClients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val packageTemplates = repository.allPackageTemplates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bills = repository.allBills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val payments = repository.allPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = repository.allSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val activityLogs = repository.allActivityLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Initialize Settings on start
    init {
        viewModelScope.launch {
            repository.getOrInitSettings()
        }
    }

    // --- Dynamic Streams based on Selection ---
    fun getClientById(clientId: Int): Flow<Client?> = repository.getClientByIdFlow(clientId)
    fun getClientServices(clientId: Int): Flow<List<ClientService>> = repository.getClientServicesFlow(clientId)
    fun getTasksByClient(clientId: Int): Flow<List<Task>> = repository.getTasksByClientFlow(clientId)
    fun getBillsByClient(clientId: Int): Flow<List<Bill>> = repository.getBillsByClientFlow(clientId)
    fun getPaymentsByClient(clientId: Int): Flow<List<Payment>> = repository.getPaymentsByClientFlow(clientId)
    fun getActivityLogsByClient(clientId: Int): Flow<List<ActivityLog>> = repository.getActivityLogsByClientFlow(clientId)

    // --- Date Formatting and Logic Helpers ---
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayString(): String {
        return dateFormat.format(Date())
    }

    fun getTomorrowString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return dateFormat.format(cal.time)
    }

    private fun parseDate(dateStr: String): Date? {
        return try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun isOverdue(dueDateStr: String): Boolean {
        val dueDate = parseDate(dueDateStr) ?: return false
        val today = parseDate(getTodayString()) ?: return false
        return dueDate.before(today)
    }

    private fun isExpiringIn3Days(endDateStr: String): Boolean {
        val endDate = parseDate(endDateStr) ?: return false
        val today = parseDate(getTodayString()) ?: return false
        
        val diffTime = endDate.time - today.time
        val diffDays = diffTime / (1000 * 60 * 60 * 24)
        return diffDays in 0..3
    }

    // --- Dashboard Metrics Flow ---
    val dashboardMetrics = combine(
        clients,
        tasks,
        payments,
        expenses
    ) { clientsList, tasksList, paymentsList, expensesList ->
        
        val totalClients = clientsList.size
        val activeClients = clientsList.count { it.status.lowercase() == "active" }
        
        // Income calculation (all payments received in the current month)
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val monthlyIncome = paymentsList
            .filter { it.paymentDate.startsWith(currentMonthPrefix) }
            .sumOf { it.amount }

        val totalAdvanceReceived = paymentsList.sumOf { it.amount }
        val pendingPayments = clientsList.sumOf { it.pendingAmount }

        // Expenses calculation (expenses in the current month)
        val monthlyExpenses = expensesList
            .filter { it.date.startsWith(currentMonthPrefix) }
            .sumOf { it.amount }

        val monthlyProfit = monthlyIncome - monthlyExpenses

        // Tasks counters
        val todayStr = getTodayString()
        val tomorrowStr = getTomorrowString()

        val todayTasks = tasksList.count { it.dueDate == todayStr && it.status.lowercase() == "pending" }
        val tomorrowTasks = tasksList.count { it.dueDate == tomorrowStr && it.status.lowercase() == "pending" }
        val overdueTasks = tasksList.count { isOverdue(it.dueDate) && it.status.lowercase() == "pending" }
        val highPriorityTasks = tasksList.count { it.priority.lowercase() == "high" && it.status.lowercase() == "pending" }

        // Expiring soon clients (next 3 days)
        val expiringPlansCount = clientsList.count { 
            it.status.lowercase() == "active" && isExpiringIn3Days(it.endDate) 
        }

        DashboardMetrics(
            totalClients = totalClients,
            activeClients = activeClients,
            monthlyIncome = monthlyIncome,
            totalAdvanceReceived = totalAdvanceReceived,
            pendingPayments = pendingPayments,
            monthlyExpenses = monthlyExpenses,
            monthlyProfit = monthlyProfit,
            todayTasks = todayTasks,
            tomorrowTasks = tomorrowTasks,
            overdueTasks = overdueTasks,
            highPriorityTasks = highPriorityTasks,
            expiringPlansCount = expiringPlansCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())


    // --- CRUD Operations Wrapper ---

    // Clients
    fun saveClient(client: Client) {
        viewModelScope.launch {
            if (client.id == 0) {
                repository.insertClient(client)
            } else {
                repository.updateClient(client)
            }
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    // Client Services
    fun addClientService(service: ClientService) {
        viewModelScope.launch {
            repository.insertClientService(service)
        }
    }

    fun deleteClientService(service: ClientService) {
        viewModelScope.launch {
            repository.deleteClientService(service)
        }
    }

    // Package Templates
    fun savePackageTemplate(template: PackageTemplate) {
        viewModelScope.launch {
            repository.insertPackageTemplate(template)
        }
    }

    fun deletePackageTemplate(template: PackageTemplate) {
        viewModelScope.launch {
            repository.deletePackageTemplate(template)
        }
    }

    // Tasks
    fun saveTask(task: Task) {
        viewModelScope.launch {
            if (task.id == 0) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
            }
        }
    }

    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(status = if (task.status.lowercase() == "pending") "Done" else "Pending")
            repository.updateTask(updated)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Bills
    fun generateBill(bill: Bill) {
        viewModelScope.launch {
            if (bill.id == 0) {
                repository.insertBill(bill)
            } else {
                repository.updateBill(bill)
            }
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    // Payments
    fun addPayment(payment: Payment) {
        viewModelScope.launch {
            repository.insertPayment(payment)
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
        }
    }

    // Expenses
    fun saveExpense(expense: Expense) {
        viewModelScope.launch {
            if (expense.id == 0) {
                repository.insertExpense(expense)
            } else {
                repository.updateExpense(expense)
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // Settings
    fun saveSettings(settings: Settings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    // Upload Asset
    fun uploadAssetFile(fileName: String, fileBytes: ByteArray, mimeType: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            val url = repository.uploadAsset(fileName, fileBytes, mimeType)
            onComplete(url)
        }
    }

    // Direct Device Upload from URI
    fun uploadUri(
        uri: Uri,
        onStart: () -> Unit,
        onResult: (String?, String?) -> Unit
    ) {
        viewModelScope.launch {
            onStart()
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                var fileName = "file_${System.currentTimeMillis()}"
                
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name = it.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                fileName = name
                            }
                        }
                    }
                }
                
                val inputStream = contentResolver.openInputStream(uri)
                val fileBytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (fileBytes == null) {
                    onResult(null, "Unable to read file content.")
                    return@launch
                }
                
                val url = repository.uploadAsset(fileName, fileBytes, mimeType)
                if (url != null) {
                    onResult(url, null)
                } else {
                    onResult(null, "Upload failed. Verify Supabase credentials in settings.")
                }
            } catch (e: Exception) {
                Log.e("CrmViewModel", "URI upload error", e)
                onResult(null, e.localizedMessage ?: "Unknown upload error")
            }
        }
    }

    // Manual Telegram Alert
    fun pushTelegramClientProfile(clientId: Int) {
        viewModelScope.launch {
            val success = repository.pushClientTelegram(clientId)
            if (success) {
                Toast.makeText(context, "Client profile pushed to Telegram!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to push profile.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Manual Expiring Plans Alert to Telegram
    fun pushTelegramExpiringPlansAlert() {
        viewModelScope.launch {
            val count = repository.pushExpiringPlansAlert()
            if (count > 0) {
                Toast.makeText(context, "$count expiring plan alert(s) pushed to Telegram!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No active plans expiring within 3 days found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Messaging Integrations ---

    fun sendWhatsAppMessage(phone: String, messageText: String) {
        try {
            // Standard formatting for Indian numbers (or fallback)
            val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
            val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
            val url = "https://wa.me/$formattedPhone?text=${Uri.encode(messageText)}"
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch WhatsApp.", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateWhatsAppTemplates(client: Client, bill: Bill?, customUpdate: String = ""): List<WhatsAppTemplate> {
        val greeting = "Hi ${client.name},\n"
        val signature = "\n\nBest Regards,\nAB Graphics"
        
        return listOf(
            WhatsAppTemplate(
                title = "Normal Message",
                body = "${greeting}Hope you are doing well! Let us know if you need any graphic design or banner printing support today.$signature"
            ),
            WhatsAppTemplate(
                title = "Bill Message",
                body = "${greeting}We have generated invoice #${bill?.billNumber ?: "N/A"} for ₹${bill?.totalAmount ?: client.totalAmount}. Pending Balance: ₹${bill?.pendingAmount ?: client.pendingAmount}. Please process payment at your earliest.$signature"
            ),
            WhatsAppTemplate(
                title = "Payment Reminder",
                body = "${greeting}Friendly reminder regarding pending payment of ₹${client.pendingAmount} for design work completed. Please pay via UPI or Bank Transfer.$signature"
            ),
            WhatsAppTemplate(
                title = "Work Update",
                body = "${greeting}Work Update: ${customUpdate.ifBlank { "Your designs have been drafted and are ready for preview! Let us know your feedback." }}$signature"
            ),
            WhatsAppTemplate(
                title = "Package Expiry Reminder",
                body = "${greeting}This is to remind you that your package '${client.packageName}' is expiring on ${client.endDate}. Please renew soon to continue uninterrupted handling services.$signature"
            )
        )
    }

    // --- Native PDF Invoice Generation Actions ---
    enum class PdfAction {
        OPEN, DOWNLOAD, PRINT, SHARE
    }

    class PdfPrintAdapter(private val file: File) : android.print.PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: android.print.PrintAttributes?,
            newAttributes: android.print.PrintAttributes?,
            cancellationSignal: android.os.CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: android.os.Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }
            val info = android.print.PrintDocumentInfo.Builder(file.name)
                .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(android.print.PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            callback?.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out android.print.PageRange>?,
            destination: android.os.ParcelFileDescriptor?,
            cancellationSignal: android.os.CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            var input: java.io.FileInputStream? = null
            var output: java.io.FileOutputStream? = null
            try {
                input = java.io.FileInputStream(file)
                output = java.io.FileOutputStream(destination?.fileDescriptor)
                val buf = ByteArray(16384)
                var bytesRead: Int
                while (input.read(buf).also { bytesRead = it } >= 0) {
                    output.write(buf, 0, bytesRead)
                }
                callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback?.onWriteFailed(e.toString())
            } finally {
                try {
                    input?.close()
                    output?.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun drawMockQrCode(canvas: android.graphics.Canvas, startX: Float, startY: Float, size: Float) {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            style = android.graphics.Paint.Style.FILL
        }
        val whitePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(startX, startY, startX + size, startY + size, whitePaint)
        canvas.drawRect(startX, startY, startX + size, startY + size, borderPaint)

        fun drawFinderPattern(px: Float, py: Float) {
            canvas.drawRect(px, py, px + 24f, py + 24f, paint)
            canvas.drawRect(px + 4f, py + 4f, px + 20f, py + 20f, whitePaint)
            canvas.drawRect(px + 8f, py + 8f, px + 16f, py + 16f, paint)
        }

        drawFinderPattern(startX + 6f, startY + 6f)
        drawFinderPattern(startX + size - 30f, startY + 6f)
        drawFinderPattern(startX + 6f, startY + size - 30f)

        val cellSize = (size - 12f) / 15f
        val random = java.util.Random(42)
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                if (row < 5 && col < 5) continue
                if (row < 5 && col >= 10) continue
                if (row >= 10 && col < 5) continue

                if (random.nextBoolean()) {
                    val cx = startX + 6f + col * cellSize
                    val cy = startY + 6f + row * cellSize
                    canvas.drawRect(cx, cy, cx + cellSize, cy + cellSize, paint)
                }
            }
        }
    }

    fun generateInvoicePdf(
        bill: Bill,
        client: Client,
        services: List<ClientService> = emptyList(),
        action: PdfAction = PdfAction.OPEN
    ) {
        viewModelScope.launch {
            try {
                val currentSettings = repository.getOrInitSettings()
                val finalServices = if (services.isEmpty()) {
                    repository.getClientServicesFlow(bill.clientId).firstOrNull() ?: emptyList()
                } else {
                    services
                }

                var logoBitmap: android.graphics.Bitmap? = null
                if (currentSettings.logoUrl.isNotBlank()) {
                    try {
                        val url = java.net.URL(currentSettings.logoUrl)
                        val connection = url.openConnection()
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        logoBitmap = android.graphics.BitmapFactory.decodeStream(connection.getInputStream())
                    } catch (e: Exception) {
                        Log.e("CrmViewModel", "Failed to load logo", e)
                    }
                }

                var qrBitmap: android.graphics.Bitmap? = null
                if (currentSettings.qrUrl.isNotBlank()) {
                    try {
                        val url = java.net.URL(currentSettings.qrUrl)
                        val connection = url.openConnection()
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        qrBitmap = android.graphics.BitmapFactory.decodeStream(connection.getInputStream())
                    } catch (e: Exception) {
                        Log.e("CrmViewModel", "Failed to load QR image", e)
                    }
                }

                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val primaryPaint = Paint().apply {
                    color = Color.parseColor("#E53935")
                    textSize = 22f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val titlePaint = Paint().apply {
                    color = Color.parseColor("#212121")
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val boldTextPaint = Paint().apply {
                    color = Color.parseColor("#212121")
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val regularTextPaint = Paint().apply {
                    color = Color.parseColor("#424242")
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val linePaint = Paint().apply {
                    color = Color.parseColor("#E0E0E0")
                    strokeWidth = 1f
                }
                val headerBgPaint = Paint().apply {
                    color = Color.parseColor("#E53935")
                    style = Paint.Style.FILL
                }
                val headerTextPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val rowBgPaint = Paint().apply {
                    color = Color.parseColor("#F5F5F5")
                    style = Paint.Style.FILL
                }

                if (logoBitmap != null) {
                    try {
                        val scaledLogo = android.graphics.Bitmap.createScaledBitmap(logoBitmap, 50, 50, true)
                        canvas.drawBitmap(scaledLogo, 40f, 40f, null)
                    } catch (e: Exception) {
                        canvas.drawRect(40f, 40f, 90f, 90f, Paint().apply { color = Color.parseColor("#E53935") })
                        canvas.drawText("AB", 52f, 72f, Paint().apply { color = Color.WHITE; textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                    }
                } else {
                    canvas.drawRect(40f, 40f, 90f, 90f, Paint().apply { color = Color.parseColor("#E53935") })
                    canvas.drawText("AB", 52f, 72f, Paint().apply { color = Color.WHITE; textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                }

                canvas.drawText(currentSettings.agencyName.ifBlank { "AB GRAPHICS" }.uppercase(), 105f, 65f, primaryPaint)
                canvas.drawText("Premium Graphic Designing & Brand Operations", 105f, 80f, regularTextPaint)

                var rightY = 48f
                val rightX = 350f
                canvas.drawText("AGENCY DETAILS:", rightX, rightY, boldTextPaint)
                rightY += 14f
                canvas.drawText(currentSettings.agencyName.ifBlank { "AB Graphics" }, rightX, rightY, regularTextPaint)
                rightY += 12f
                canvas.drawText("Phone: ${currentSettings.agencyPhone.ifBlank { "+91 98765 43210" }}", rightX, rightY, regularTextPaint)
                rightY += 12f
                canvas.drawText("Address: ${currentSettings.agencyAddress.ifBlank { "Delhi, India" }}", rightX, rightY, regularTextPaint)

                canvas.drawLine(40f, 105f, 555f, 105f, linePaint)

                canvas.drawText("INVOICE", 40f, 135f, primaryPaint.apply { textSize = 16f })
                canvas.drawText("Invoice Number: ${bill.billNumber}", 40f, 155f, boldTextPaint)
                canvas.drawText("Invoice Date: ${bill.billDate}", 40f, 170f, regularTextPaint)
                canvas.drawText("Campaign Term: ${client.startDate} to ${client.endDate}", 40f, 185f, regularTextPaint)
                canvas.drawText("Duration Days: ${client.durationDays} Days", 40f, 200f, regularTextPaint)

                var billToY = 135f
                canvas.drawText("BILL TO:", rightX, billToY, boldTextPaint)
                billToY += 18f
                canvas.drawText("Client Name: ${client.name}", rightX, billToY, regularTextPaint)
                billToY += 14f
                canvas.drawText("Business Name: ${client.businessName}", rightX, billToY, regularTextPaint)
                billToY += 14f
                canvas.drawText("Contact No: ${client.phone}", rightX, billToY, regularTextPaint)
                billToY += 14f
                canvas.drawText("WhatsApp: ${client.whatsapp}", rightX, billToY, regularTextPaint)
                billToY += 14f
                canvas.drawText("Address: ${client.address.ifBlank { "N/A" }}", rightX, billToY, regularTextPaint)

                canvas.drawLine(40f, 225f, 555f, 225f, linePaint)

                var tableY = 245f
                canvas.drawRect(40f, tableY, 555f, tableY + 20f, headerBgPaint)
                canvas.drawText("SERVICE / PACKAGE DESCRIPTION", 45f, tableY + 13f, headerTextPaint)
                canvas.drawText("QTY", 310f, tableY + 13f, headerTextPaint)
                canvas.drawText("RATE", 365f, tableY + 13f, headerTextPaint)
                canvas.drawText("DISCOUNT", 430f, tableY + 13f, headerTextPaint)
                canvas.drawText("TOTAL", 500f, tableY + 13f, headerTextPaint)

                tableY += 20f

                var currentY = tableY + 18f
                var isAltRow = false

                if (finalServices.isEmpty()) {
                    canvas.drawText(client.packageName.ifBlank { "Social Media & Graphics Services" }, 45f, currentY, regularTextPaint)
                    canvas.drawText("1", 310f, currentY, regularTextPaint)
                    canvas.drawText("₹${client.totalAmount}", 365f, currentY, regularTextPaint)
                    canvas.drawText("₹0.0", 430f, currentY, regularTextPaint)
                    canvas.drawText("₹${client.totalAmount}", 500f, currentY, boldTextPaint)
                    currentY += 22f
                } else {
                    for (srv in finalServices) {
                        if (isAltRow) {
                            canvas.drawRect(40f, currentY - 12f, 555f, currentY + 6f, rowBgPaint)
                        }
                        canvas.drawText(srv.serviceName, 45f, currentY, regularTextPaint)
                        canvas.drawText(srv.quantity.toString(), 310f, currentY, regularTextPaint)
                        canvas.drawText("₹${srv.rate}", 365f, currentY, regularTextPaint)
                        canvas.drawText("₹${srv.discount}", 430f, currentY, regularTextPaint)
                        canvas.drawText("₹${srv.total}", 500f, currentY, boldTextPaint)
                        
                        currentY += 22f
                        isAltRow = !isAltRow
                        if (currentY > 600f) break
                    }
                }

                canvas.drawLine(40f, currentY - 5f, 555f, currentY - 5f, linePaint)
                currentY += 15f

                var summaryY = currentY
                
                if (qrBitmap != null) {
                    try {
                        val scaledQr = android.graphics.Bitmap.createScaledBitmap(qrBitmap, 90, 90, true)
                        canvas.drawBitmap(scaledQr, 40f, summaryY, null)
                    } catch (e: Exception) {
                        drawMockQrCode(canvas, 40f, summaryY, 90f)
                    }
                } else {
                    drawMockQrCode(canvas, 40f, summaryY, 90f)
                }

                canvas.drawText("SCAN TO PAY VIA UPI", 145f, summaryY + 15f, boldTextPaint)
                canvas.drawText("UPI ID: ${currentSettings.upiId.ifBlank { "abgraphics@upi" }}", 145f, summaryY + 30f, regularTextPaint)
                canvas.drawText("Receiver: ${currentSettings.agencyName.ifBlank { "AB Graphics" }}", 145f, summaryY + 42f, regularTextPaint)
                canvas.drawText("Scan with any UPI app (GPay, PhonePe, Paytm)", 145f, summaryY + 54f, regularTextPaint.apply { textSize = 7.5f })

                val sumX = 350f
                canvas.drawText("FINANCIAL SUMMARY:", sumX, summaryY + 15f, boldTextPaint)
                canvas.drawLine(sumX, summaryY + 22f, 555f, summaryY + 22f, linePaint)

                canvas.drawText("Total Invoice Amount:", sumX, summaryY + 38f, regularTextPaint)
                canvas.drawText("₹${bill.totalAmount}", 500f, summaryY + 38f, boldTextPaint)

                canvas.drawText("Advance Paid:", sumX, summaryY + 53f, regularTextPaint)
                canvas.drawText("₹${bill.advancePaid}", 500f, summaryY + 53f, regularTextPaint.apply { color = Color.parseColor("#2E7D32") })

                canvas.drawText("Pending Balance Due:", sumX, summaryY + 68f, boldTextPaint)
                canvas.drawText("₹${bill.pendingAmount}", 500f, summaryY + 68f, boldTextPaint.apply { color = Color.parseColor("#D32F2F") })

                val statusColor = when (bill.paymentStatus.lowercase()) {
                    "paid" -> Color.parseColor("#2E7D32")
                    "partial" -> Color.parseColor("#EF6C00")
                    else -> Color.parseColor("#D32F2F")
                }
                canvas.drawRect(sumX, summaryY + 78f, sumX + 205f, summaryY + 95f, Paint().apply { color = statusColor; alpha = 38 })
                canvas.drawText("PAYMENT STATUS: ${bill.paymentStatus.uppercase()}", sumX + 10f, summaryY + 90f, boldTextPaint.apply { color = statusColor; textSize = 8.5f })

                summaryY += 115f

                canvas.drawLine(40f, summaryY, 555f, summaryY, linePaint)
                summaryY += 20f

                canvas.drawText("TERMS & NOTES:", 40f, summaryY, boldTextPaint)
                summaryY += 14f
                canvas.drawText(bill.notes.ifBlank { "This is an electronically generated service invoice. Payment must be cleared within due dates." }, 40f, summaryY, regularTextPaint)
                summaryY += 15f
                canvas.drawText("Thank you for your business! We look forward to working with you.", 40f, summaryY, regularTextPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); color = Color.parseColor("#D32F2F") })

                pdfDocument.finishPage(page)

                val fileName = "ABG_Invoice_${bill.billNumber}.pdf"
                val cacheFile = File(context.cacheDir, fileName)
                FileOutputStream(cacheFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                pdfDocument.close()

                when (action) {
                    PdfAction.DOWNLOAD -> {
                        val savedUri = savePdfToPublicDownloads(context, cacheFile, fileName)
                        if (savedUri != null) {
                            Toast.makeText(context, "Invoice Saved to Downloads Folder!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Saved to cache: $fileName", Toast.LENGTH_LONG).show()
                        }
                    }
                    PdfAction.PRINT -> {
                        try {
                            val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                            val jobName = "ABG Invoice ${bill.billNumber}"
                            printManager.print(jobName, PdfPrintAdapter(cacheFile), null)
                        } catch (e: Exception) {
                            Log.e("CrmViewModel", "Print failed", e)
                            Toast.makeText(context, "Printing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    PdfAction.SHARE -> {
                        sharePdfFile(cacheFile)
                    }
                    PdfAction.OPEN -> {
                        openPdfFile(cacheFile)
                    }
                }
            } catch (e: Exception) {
                Log.e("CrmViewModel", "PDF execution failed", e)
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun savePdfToPublicDownloads(context: Context, file: File, fileName: String): Uri? {
        val resolver = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        java.io.FileInputStream(file).use { input ->
                            input.copyTo(out)
                        }
                    }
                    return uri
                } catch (e: Exception) {
                    Log.e("CrmViewModel", "Failed saving to MediaStore", e)
                }
            }
        } else {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)
                java.io.FileInputStream(file).use { input ->
                    FileOutputStream(targetFile).use { out ->
                        input.copyTo(out)
                    }
                }
                return Uri.fromFile(targetFile)
            } catch (e: Exception) {
                Log.e("CrmViewModel", "Failed saving to Environment.Downloads", e)
            }
        }
        return null
    }

    private fun openPdfFile(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePdfFile(file)
        }
    }

    private fun sharePdfFile(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open or share PDF.", Toast.LENGTH_SHORT).show()
        }
    }
}

// --- Companion Helper Classes ---

data class DashboardMetrics(
    val totalClients: Int = 0,
    val activeClients: Int = 0,
    val monthlyIncome: Double = 0.0,
    val totalAdvanceReceived: Double = 0.0,
    val pendingPayments: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val monthlyProfit: Double = 0.0,
    val todayTasks: Int = 0,
    val tomorrowTasks: Int = 0,
    val overdueTasks: Int = 0,
    val highPriorityTasks: Int = 0,
    val expiringPlansCount: Int = 0
)

data class WhatsAppTemplate(
    val title: String,
    val body: String
)
