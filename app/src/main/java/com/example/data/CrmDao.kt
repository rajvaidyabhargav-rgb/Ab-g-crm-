package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CrmDao {

    // --- Clients ---
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClientsFlow(): Flow<List<Client>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    suspend fun getAllClients(): List<Client>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    fun getClientByIdFlow(id: Int): Flow<Client?>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientById(id: Int): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)


    // --- Client Services ---
    @Query("SELECT * FROM client_services WHERE clientId = :clientId")
    fun getClientServicesFlow(clientId: Int): Flow<List<ClientService>>

    @Query("SELECT * FROM client_services WHERE clientId = :clientId")
    suspend fun getClientServices(clientId: Int): List<ClientService>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClientService(service: ClientService): Long

    @Update
    suspend fun updateClientService(service: ClientService)

    @Delete
    suspend fun deleteClientService(service: ClientService)

    @Query("DELETE FROM client_services WHERE clientId = :clientId")
    suspend fun deleteServicesByClientId(clientId: Int)


    // --- Package Templates ---
    @Query("SELECT * FROM package_templates ORDER BY templateName ASC")
    fun getAllPackageTemplatesFlow(): Flow<List<PackageTemplate>>

    @Query("SELECT * FROM package_templates ORDER BY templateName ASC")
    suspend fun getAllPackageTemplates(): List<PackageTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackageTemplate(template: PackageTemplate): Long

    @Delete
    suspend fun deletePackageTemplate(template: PackageTemplate)


    // --- Tasks ---
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE clientId = :clientId ORDER BY dueDate ASC")
    fun getTasksByClientFlow(clientId: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE clientId = :clientId ORDER BY dueDate ASC")
    suspend fun getTasksByClient(clientId: Int): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Int): Task?

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE clientId = :clientId")
    suspend fun deleteTasksByClientId(clientId: Int)


    // --- Bills ---
    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: Int): Bill?

    @Query("SELECT * FROM bills ORDER BY billDate DESC, id DESC")
    fun getAllBillsFlow(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE clientId = :clientId ORDER BY billDate DESC")
    fun getBillsByClientFlow(clientId: Int): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE clientId = :clientId ORDER BY billDate DESC")
    suspend fun getBillsByClient(clientId: Int): List<Bill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("DELETE FROM bills WHERE clientId = :clientId")
    suspend fun deleteBillsByClientId(clientId: Int)


    // --- Payments ---
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPaymentsFlow(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY paymentDate DESC")
    fun getPaymentsByClientFlow(clientId: Int): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY paymentDate DESC")
    suspend fun getPaymentsByClient(clientId: Int): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("DELETE FROM payments WHERE clientId = :clientId")
    suspend fun deletePaymentsByClientId(clientId: Int)


    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpensesFlow(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)


    // --- Settings ---
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<Settings?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: Settings): Long


    // --- Activity Logs ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogsFlow(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE clientId = :clientId ORDER BY timestamp DESC")
    fun getActivityLogsByClientFlow(clientId: Int): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog): Long

    @Query("DELETE FROM activity_logs WHERE clientId = :clientId")
    suspend fun deleteActivityLogsByClientId(clientId: Int)
}
