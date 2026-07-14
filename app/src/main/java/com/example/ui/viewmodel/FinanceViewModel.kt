package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.FinanceRepository
import com.example.data.remote.SpreadsheetAuthClient
import com.example.data.remote.SpreadsheetRequest
import com.example.data.remote.UserResponseInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(db.financeDao())

    // Shared Preferences for Settings
    private val prefs = application.getSharedPreferences("smart_finance_settings", Context.MODE_PRIVATE)

    // Exposed Flows from Room
    val transactions = repository.allTransactions
    val budgets = repository.allBudgets
    val savingGoals = repository.allSavingGoals
    val debtsCredits = repository.allDebtsCredits
    val categories = repository.allCategories

    // --- Search & Filter States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilterCategory = MutableStateFlow<String?>(null)
    val selectedFilterCategory = _selectedFilterCategory.asStateFlow()

    private val _minNominal = MutableStateFlow<Double?>(null)
    val minNominal = _minNominal.asStateFlow()

    private val _maxNominal = MutableStateFlow<Double?>(null)
    val maxNominal = _maxNominal.asStateFlow()

    private val _filterPaymentMethod = MutableStateFlow<String?>(null)
    val filterPaymentMethod = _filterPaymentMethod.asStateFlow()

    private val _filterStatus = MutableStateFlow<String?>(null)
    val filterStatus = _filterStatus.asStateFlow()

    private val _filterStartDate = MutableStateFlow<String?>(null) // YYYY-MM-DD
    val filterStartDate = _filterStartDate.asStateFlow()

    private val _filterEndDate = MutableStateFlow<String?>(null) // YYYY-MM-DD
    val filterEndDate = _filterEndDate.asStateFlow()

    // Sort order: "DATE_DESC", "DATE_ASC", "AMOUNT_DESC", "AMOUNT_ASC"
    private val _sortOrder = MutableStateFlow("DATE_DESC")
    val sortOrder = _sortOrder.asStateFlow()

    // --- Active Selection State for Bulk Action ---
    private val _selectedTransactionIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedTransactionIds = _selectedTransactionIds.asStateFlow()

    // --- Undo Action Stack ---
    private var lastDeletedTransaction: TransactionEntity? = null
    private val _showUndoToast = MutableStateFlow<String?>(null)
    val showUndoToast = _showUndoToast.asStateFlow()

    // --- Calendar State ---
    private val _calendarSelectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val calendarSelectedDate = _calendarSelectedDate.asStateFlow()

    // --- Settings Preferences ---
    private val _currency = MutableStateFlow(prefs.getString("currency", "Rp") ?: "Rp")
    val currency = _currency.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "id") ?: "id") // "id" or "en"
    val language = _language.asStateFlow()

    private val _themePreference = MutableStateFlow(prefs.getString("theme", "SYSTEM") ?: "SYSTEM") // "DARK", "LIGHT", "SYSTEM"
    val themePreference = _themePreference.asStateFlow()

    // --- User Session States ---
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("user_is_logged_in", false))
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _currentUserName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val currentUserName = _currentUserName.asStateFlow()

    private val _currentUserEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val currentUserEmail = _currentUserEmail.asStateFlow()

    private val _spreadsheetScriptUrl = MutableStateFlow(prefs.getString("spreadsheet_script_url", "https://script.google.com/macros/s/AKfycbz05__FCGfgTs1VD2nCErf9HfRus4ndfHi4Eq5GyJkbDWA5cRyzaPaMLz3v0RTdbSdC/exec") ?: "https://script.google.com/macros/s/AKfycbz05__FCGfgTs1VD2nCErf9HfRus4ndfHi4Eq5GyJkbDWA5cRyzaPaMLz3v0RTdbSdC/exec")
    val spreadsheetScriptUrl = _spreadsheetScriptUrl.asStateFlow()

    // --- Notification Center Messages ---
    private val _notifications = MutableStateFlow<List<String>>(emptyList())
    val notifications = _notifications.asStateFlow()

    // Filtered Transactions Flow
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        transactions,
        _searchQuery,
        _selectedFilterCategory,
        _minNominal,
        _maxNominal,
        _filterPaymentMethod,
        _filterStatus,
        _filterStartDate,
        _filterEndDate,
        _sortOrder
    ) { params ->
        val list = params[0] as List<TransactionEntity>
        val query = params[1] as String
        val cat = params[2] as String?
        val min = params[3] as Double?
        val max = params[4] as Double?
        val method = params[5] as String?
        val status = params[6] as String?
        val start = params[7] as String?
        val end = params[8] as String?
        val sort = params[9] as String

        var result = list

        if (query.isNotEmpty()) {
            result = result.filter {
                it.note.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true) ||
                        it.amount.toString().contains(query)
            }
        }
        if (cat != null) {
            result = result.filter { it.category == cat }
        }
        if (min != null) {
            result = result.filter { it.amount >= min }
        }
        if (max != null) {
            result = result.filter { it.amount <= max }
        }
        if (method != null) {
            result = result.filter { it.paymentMethod == method }
        }
        if (status != null) {
            result = result.filter { it.status == status }
        }
        if (start != null) {
            result = result.filter { it.date >= start }
        }
        if (end != null) {
            result = result.filter { it.date <= end }
        }

        // Sort
        result = when (sort) {
            "DATE_DESC" -> result.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.time })
            "DATE_ASC" -> result.sortedWith(compareBy<TransactionEntity> { it.date }.thenBy { it.time })
            "AMOUNT_DESC" -> result.sortedByDescending { it.amount }
            "AMOUNT_ASC" -> result.sortedBy { it.amount }
            else -> result
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Budget Status check
    init {
        viewModelScope.launch {
            // Check and seed initial database data
            repository.checkAndPrepopulate()
            
            // Generate initial notifications after checking budgets & target
            checkBudgetsAndGenerateNotifications()
        }
    }

    // --- Search & Filter Triggers ---
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setFilterCategory(cat: String?) { _selectedFilterCategory.value = cat }
    fun setMinNominal(nominal: Double?) { _minNominal.value = nominal }
    fun setMaxNominal(nominal: Double?) { _maxNominal.value = nominal }
    fun setFilterPaymentMethod(method: String?) { _filterPaymentMethod.value = method }
    fun setFilterStatus(status: String?) { _filterStatus.value = status }
    fun setFilterDateRange(start: String?, end: String?) {
        _filterStartDate.value = start
        _filterEndDate.value = end
    }
    fun setSortOrder(order: String) { _sortOrder.value = order }
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedFilterCategory.value = null
        _minNominal.value = null
        _maxNominal.value = null
        _filterPaymentMethod.value = null
        _filterStatus.value = null
        _filterStartDate.value = null
        _filterEndDate.value = null
    }

    // --- Transaction Actions ---
    fun addTransaction(
        amount: Double,
        type: String,
        category: String,
        note: String,
        paymentMethod: String,
        status: String,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
        attachmentUri: String? = null
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                date = date,
                time = time,
                amount = amount,
                type = type,
                category = category,
                note = note,
                paymentMethod = paymentMethod,
                status = status,
                attachmentUri = attachmentUri
            )
            repository.insertTransaction(entity)
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun updateTransaction(entity: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(entity)
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun deleteTransaction(entity: TransactionEntity) {
        viewModelScope.launch {
            lastDeletedTransaction = entity
            repository.deleteTransaction(entity)
            _showUndoToast.value = "Transaksi dihapus"
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun deleteTransactionsBulk(ids: List<Int>) {
        viewModelScope.launch {
            repository.deleteTransactionsBulk(ids)
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun undoDelete() {
        val lastDeleted = lastDeletedTransaction ?: return
        viewModelScope.launch {
            repository.insertTransaction(lastDeleted)
            lastDeletedTransaction = null
            _showUndoToast.value = null
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun clearUndoToast() {
        _showUndoToast.value = null
    }

    fun duplicateTransaction(entity: TransactionEntity) {
        viewModelScope.launch {
            val copy = entity.copy(
                id = 0,
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            )
            repository.insertTransaction(copy)
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun toggleFavorite(entity: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(entity.copy(isFavorite = !entity.isFavorite))
        }
    }

    fun togglePinned(entity: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(entity.copy(isPinned = !entity.isPinned))
        }
    }

    // --- Bulk Selection ---
    fun toggleSelectTransaction(id: Int) {
        val current = _selectedTransactionIds.value
        _selectedTransactionIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun selectAllTransactions(ids: List<Int>) {
        _selectedTransactionIds.value = ids.toSet()
    }

    fun clearSelection() {
        _selectedTransactionIds.value = emptySet()
    }

    fun deleteSelectedTransactions() {
        val ids = _selectedTransactionIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteTransactionsBulk(ids)
            _selectedTransactionIds.value = emptySet()
            checkBudgetsAndGenerateNotifications()
        }
    }

    // --- Budget Actions ---
    fun saveBudget(category: String, amount: Double) {
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            repository.insertBudget(BudgetEntity(category, amount, currentMonth))
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun deleteBudgetByCategory(category: String) {
        viewModelScope.launch {
            repository.deleteBudgetByCategory(category)
            checkBudgetsAndGenerateNotifications()
        }
    }

    // --- Saving Goal Actions ---
    fun addSavingGoal(name: String, target: Double, current: Double, note: String?) {
        viewModelScope.launch {
            repository.insertSavingGoal(SavingGoalEntity(name = name, targetAmount = target, currentAmount = current, note = note))
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun updateSavingGoal(entity: SavingGoalEntity) {
        viewModelScope.launch {
            repository.updateSavingGoal(entity)
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun deleteSavingGoal(entity: SavingGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingGoal(entity)
        }
    }

    // --- Debt & Credit Actions ---
    fun addDebtCredit(name: String, amount: Double, type: String, dueDate: String, description: String, isReminder: Boolean) {
        viewModelScope.launch {
            repository.insertDebtCredit(
                DebtCreditEntity(
                    personName = name,
                    amount = amount,
                    type = type,
                    dueDate = dueDate,
                    description = description,
                    status = "BELUM_LUNAS",
                    isReminderActive = isReminder
                )
            )
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun updateDebtCredit(entity: DebtCreditEntity) {
        viewModelScope.launch {
            repository.updateDebtCredit(entity)
            checkBudgetsAndGenerateNotifications()
        }
    }

    fun deleteDebtCredit(entity: DebtCreditEntity) {
        viewModelScope.launch {
            repository.deleteDebtCredit(entity)
        }
    }

    // --- Category Actions ---
    fun addCategory(name: String, type: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name, type, colorHex, iconName))
        }
    }

    fun deleteCategoryByName(name: String) {
        viewModelScope.launch {
            repository.deleteCategoryByName(name)
        }
    }

    // --- Calendar Selected Date ---
    fun selectCalendarDate(dateString: String) {
        _calendarSelectedDate.value = dateString
    }

    // --- Settings Preferences Updates ---
    fun setCurrency(curr: String) {
        prefs.edit().putString("currency", curr).apply()
        _currency.value = curr
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _language.value = lang
    }

    fun setThemePreference(theme: String) {
        prefs.edit().putString("theme", theme).apply()
        _themePreference.value = theme
    }

    // --- Real-time Notifications Generator ---
    private suspend fun checkBudgetsAndGenerateNotifications() {
        val currentTxList = transactions.first()
        val currentBudgetList = budgets.first()
        val currentGoalsList = savingGoals.first()
        val currentDebtsCreditsList = debtsCredits.first()

        val listNotif = mutableListOf<String>()

        // 1. Budget exhausted / exceeded notifications
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val monthlySpentByCategory = currentTxList
            .filter { it.type == "PENGELUARAN" && it.status == "BERHASIL" && it.date.startsWith(currentMonth) }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        for (budget in currentBudgetList) {
            val spent = monthlySpentByCategory[budget.category] ?: 0.0
            if (spent >= budget.limitAmount) {
                listNotif.add("⚠️ Anggaran '${budget.category}' HABIS! Pengeluaran (${formatCurrency(spent)}) melebihi batas (${formatCurrency(budget.limitAmount)}).")
            } else if (spent >= budget.limitAmount * 0.8) {
                listNotif.add("⚠️ Anggaran '${budget.category}' mendekati batas! Pengeluaran (${formatCurrency(spent)}) sudah mencapai 80% dari batas (${formatCurrency(budget.limitAmount)}).")
            }
        }

        // 2. Saving Goal achieved
        for (goal in currentGoalsList) {
            if (goal.currentAmount >= goal.targetAmount) {
                listNotif.add("🎉 Target Tabungan '${goal.name}' TERCAPAI! Selamat, Anda berhasil mengumpulkan ${formatCurrency(goal.currentAmount)} dari target ${formatCurrency(goal.targetAmount)}.")
            }
        }

        // 3. Debts Credits Due Soon
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayCalendar = Calendar.getInstance()
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        todayCalendar.set(Calendar.MINUTE, 0)
        todayCalendar.set(Calendar.SECOND, 0)
        todayCalendar.set(Calendar.MILLISECOND, 0)

        for (item in currentDebtsCreditsList) {
            if (item.status == "BELUM_LUNAS") {
                try {
                    val itemDate = df.parse(item.dueDate)
                    if (itemDate != null) {
                        val itemCalendar = Calendar.getInstance()
                        itemCalendar.time = itemDate
                        val diffMillis = itemCalendar.timeInMillis - todayCalendar.timeInMillis
                        val diffDays = diffMillis / (1000 * 60 * 60 * 24)
                        if (diffDays in 0..3) {
                            val prefix = if (item.type == "HUTANG") "Hutang kepada" else "Piutang dari"
                            listNotif.add("📅 Jatuh tempo dekat! $prefix ${item.personName} sebesar ${formatCurrency(item.amount)} jatuh tempo dalam $diffDays hari (${item.dueDate}).")
                        } else if (diffDays < 0) {
                            val prefix = if (item.type == "HUTANG") "Hutang kepada" else "Piutang dari"
                            listNotif.add("🚨 Terlewat jatuh tempo! $prefix ${item.personName} sebesar ${formatCurrency(item.amount)} terlambat ${-diffDays} hari.")
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }
        }

        _notifications.value = listNotif
    }

    private fun formatCurrency(amount: Double): String {
        return "${_currency.value} ${String.format("%,.0f", amount)}"
    }

    // --- Import / Export Simulated Actions ---
    fun backupData(): String {
        // Return a simulated JSON string of all active states
        return "SmartFinance_Backup_${System.currentTimeMillis()}"
    }

    fun restoreData(backupCode: String): Boolean {
        // In a real database we'd parse and restore, for now we simulate a success
        return backupCode.startsWith("SmartFinance_Backup_")
    }

    fun registerUser(name: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // Selalu simpan di local storage (cache) agar mendukung mode offline / login lokal
            prefs.edit()
                .putString("local_user_pwd_$email", password)
                .putString("local_user_name_$email", name)
                .apply()

            val url = _spreadsheetScriptUrl.value
            if (url.isBlank()) {
                onResult(true, "Registrasi berhasil disimpan di lokal storage!")
            } else {
                try {
                    val response = com.example.data.remote.SpreadsheetAuthClient.api.authenticateUser(
                        url = url,
                        request = com.example.data.remote.SpreadsheetRequest(
                            action = "register",
                            email = email,
                            name = name,
                            password = password
                        )
                    )
                    if (response.status == "success") {
                        onResult(true, response.message)
                    } else {
                        onResult(false, response.message)
                    }
                } catch (e: Exception) {
                    // Fallback to local storage if online sync fails
                    onResult(true, "Registrasi berhasil disimpan secara lokal (Offline)")
                }
            }
        }
    }

    fun loginUser(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val url = _spreadsheetScriptUrl.value
            val savedPwd = prefs.getString("local_user_pwd_$email", null)
            val savedName = prefs.getString("local_user_name_$email", null)

            if (url.isBlank()) {
                if (savedPwd == null || savedPwd != password) {
                    onResult(false, "Email atau password salah (Lokal)")
                } else {
                    prefs.edit()
                        .putBoolean("user_is_logged_in", true)
                        .putString("user_name", savedName ?: "Pengguna")
                        .putString("user_email", email)
                        .apply()
                    _isLoggedIn.value = true
                    _currentUserName.value = savedName ?: "Pengguna"
                    _currentUserEmail.value = email
                    onResult(true, "Login berhasil menggunakan lokal storage!")
                }
            } else {
                try {
                    val response = com.example.data.remote.SpreadsheetAuthClient.api.authenticateUser(
                        url = url,
                        request = com.example.data.remote.SpreadsheetRequest(
                            action = "login",
                            email = email,
                            password = password
                        )
                    )
                    if (response.status == "success" && response.user != null) {
                        prefs.edit()
                            .putBoolean("user_is_logged_in", true)
                            .putString("user_name", response.user.name)
                            .putString("user_email", response.user.email)
                            // Simpan di lokal storage agar sinkron
                            .putString("local_user_pwd_${response.user.email}", password)
                            .putString("local_user_name_${response.user.email}", response.user.name)
                            .apply()
                        _isLoggedIn.value = true
                        _currentUserName.value = response.user.name
                        _currentUserEmail.value = response.user.email
                        onResult(true, response.message)
                    } else {
                        // Fallback ke penyimpanan lokal jika server mengembalikan error tapi data lokal cocok
                        if (savedPwd != null && savedPwd == password) {
                            prefs.edit()
                                .putBoolean("user_is_logged_in", true)
                                .putString("user_name", savedName ?: "Pengguna")
                                .putString("user_email", email)
                                .apply()
                            _isLoggedIn.value = true
                            _currentUserName.value = savedName ?: "Pengguna"
                            _currentUserEmail.value = email
                            onResult(true, "Login berhasil menggunakan akun lokal storage!")
                        } else {
                            onResult(false, response.message)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback ke penyimpanan lokal jika koneksi gagal/offline
                    if (savedPwd != null && savedPwd == password) {
                        prefs.edit()
                            .putBoolean("user_is_logged_in", true)
                            .putString("user_name", savedName ?: "Pengguna")
                            .putString("user_email", email)
                            .apply()
                        _isLoggedIn.value = true
                        _currentUserName.value = savedName ?: "Pengguna"
                        _currentUserEmail.value = email
                        onResult(true, "Koneksi offline. Login berhasil menggunakan lokal storage.")
                    } else {
                        onResult(false, "Gagal terhubung ke Spreadsheet & tidak ada data lokal yang cocok.")
                    }
                }
            }
        }
    }

    fun generateResetCode(email: String, onResult: (Boolean, String, String) -> Unit) {
        val savedPwd = prefs.getString("local_user_pwd_$email", null)
        if (savedPwd == null) {
            onResult(false, "Alamat email tidak terdaftar di aplikasi ini.", "")
        } else {
            val code = (100000..999999).random().toString()
            prefs.edit().putString("local_user_reset_code_$email", code).apply()
            onResult(true, "Kode reset password berhasil dibuat!", code)
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String, onResult: (Boolean, String) -> Unit) {
        val savedCode = prefs.getString("local_user_reset_code_$email", null)
        val savedName = prefs.getString("local_user_name_$email", "Pengguna")
        if (savedCode == null || savedCode != code) {
            onResult(false, "Kode reset salah atau tidak sesuai!")
        } else {
            prefs.edit()
                .putString("local_user_pwd_$email", newPassword)
                .remove("local_user_reset_code_$email")
                .apply()
            
            // Selalu usahakan update juga di Apps Script jika online
            val url = _spreadsheetScriptUrl.value
            if (url.isNotBlank()) {
                viewModelScope.launch {
                    try {
                        com.example.data.remote.SpreadsheetAuthClient.api.authenticateUser(
                            url = url,
                            request = com.example.data.remote.SpreadsheetRequest(
                                action = "register", // Daftar ulang/tulis ulang credential
                                email = email,
                                name = savedName ?: "Pengguna",
                                password = newPassword
                            )
                        )
                    } catch (e: Exception) {
                        // Silent fail
                    }
                }
            }
            onResult(true, "Password berhasil diperbarui di lokal storage!")
        }
    }

    fun logoutUser() {
        prefs.edit()
            .putBoolean("user_is_logged_in", false)
            .putString("user_name", "")
            .putString("user_email", "")
            .apply()
        _isLoggedIn.value = false
        _currentUserName.value = ""
        _currentUserEmail.value = ""
    }

    fun setSpreadsheetScriptUrl(url: String) {
        prefs.edit().putString("spreadsheet_script_url", url).apply()
        _spreadsheetScriptUrl.value = url
    }
}
