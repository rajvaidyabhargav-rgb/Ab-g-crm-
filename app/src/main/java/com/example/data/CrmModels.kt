package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val businessName: String,
    val phone: String,
    val whatsapp: String,
    val email: String,
    val address: String,
    val status: String = "Active", // Active, Inactive, Completed
    val packageName: String = "",
    val durationDays: Int = 30,
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = "",
    val totalAmount: Double = 0.0,
    val advancePaid: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val logoUrl: String = "",
    val photos: String = "" // Comma-separated image URLs
)

@Entity(tableName = "client_services")
data class ClientService(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val serviceName: String,
    val quantity: Int = 1,
    val rate: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0, // calculated as (quantity * rate) - discount
    val notes: String = ""
)

@Entity(tableName = "package_templates")
data class PackageTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateName: String,
    val serviceName: String,
    val quantity: Int = 1,
    val rate: Double = 0.0,
    val discount: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val clientId: Int,
    val dueDate: String = "", // YYYY-MM-DD
    val priority: String = "Medium", // Low, Medium, High
    val status: String = "Pending", // Pending, Done
    val notes: String = ""
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val billNumber: String,
    val billDate: String = "", // YYYY-MM-DD
    val clientId: Int,
    val totalAmount: Double = 0.0,
    val advancePaid: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val paymentStatus: String = "Pending", // Paid, Partial, Pending
    val notes: String = "",
    val proofUrl: String = ""
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val amount: Double = 0.0,
    val paymentDate: String = "", // YYYY-MM-DD
    val paymentMethod: String = "UPI", // UPI, Cash, Bank Transfer
    val status: String = "Paid",
    val notes: String = "",
    val screenshotUrl: String = ""
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String = "",
    val amount: Double = 0.0,
    val date: String = "", // YYYY-MM-DD
    val notes: String = ""
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1, // Fixed ID to always have exactly 1 record
    val agencyName: String = "AB Graphics",
    val agencyPhone: String = "",
    val agencyAddress: String = "",
    val upiId: String = "",
    val logoUrl: String = "",
    val qrUrl: String = "",
    val telegramToken: String = "",
    val telegramChatId: String = "",
    val telegramEnabled: Boolean = false,
    val supabaseUrl: String = "",
    val supabaseKey: String = ""
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val clientId: Int? = null
)
