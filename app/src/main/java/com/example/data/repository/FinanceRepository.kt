package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class FinanceRepository(private val financeDao: FinanceDao) {

    // Streams
    val allTransactions: Flow<List<TransactionEntity>> = financeDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = financeDao.getAllBudgets()
    val allSavingGoals: Flow<List<SavingGoalEntity>> = financeDao.getAllSavingGoals()
    val allDebtsCredits: Flow<List<DebtCreditEntity>> = financeDao.getAllDebtsCredits()
    val allCategories: Flow<List<CategoryEntity>> = financeDao.getAllCategories()

    // --- Transactions CRUD ---
    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return financeDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        financeDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        financeDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        financeDao.deleteTransactionById(id)
    }

    suspend fun deleteTransactionsBulk(ids: List<Int>) {
        financeDao.deleteTransactionsBulk(ids)
    }

    suspend fun getTransactionById(id: Int): TransactionEntity? {
        return financeDao.getTransactionById(id)
    }


    // --- Budgets CRUD ---
    suspend fun insertBudget(budget: BudgetEntity) {
        financeDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        financeDao.deleteBudget(budget)
    }

    suspend fun deleteBudgetByCategory(category: String) {
        financeDao.deleteBudgetByCategory(category)
    }


    // --- Saving Goals CRUD ---
    suspend fun insertSavingGoal(goal: SavingGoalEntity): Long {
        return financeDao.insertSavingGoal(goal)
    }

    suspend fun updateSavingGoal(goal: SavingGoalEntity) {
        financeDao.updateSavingGoal(goal)
    }

    suspend fun deleteSavingGoal(goal: SavingGoalEntity) {
        financeDao.deleteSavingGoal(goal)
    }


    // --- Debts & Credits CRUD ---
    suspend fun insertDebtCredit(item: DebtCreditEntity): Long {
        return financeDao.insertDebtCredit(item)
    }

    suspend fun updateDebtCredit(item: DebtCreditEntity) {
        financeDao.updateDebtCredit(item)
    }

    suspend fun deleteDebtCredit(item: DebtCreditEntity) {
        financeDao.deleteDebtCredit(item)
    }


    // --- Categories CRUD ---
    suspend fun insertCategory(category: CategoryEntity) {
        financeDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        financeDao.deleteCategory(category)
    }

    suspend fun deleteCategoryByName(name: String) {
        financeDao.deleteCategoryByName(name)
    }

    // --- Pre-populate Database if Empty ---
    suspend fun checkAndPrepopulate() {
        val existingCategories = allCategories.first()
        if (existingCategories.isEmpty()) {
            // 1. Populate standard Indonesian categories
            val defaultCategories = listOf(
                // Pemasukan
                CategoryEntity("Gaji", "PEMASUKAN", "#3B82F6", "work"),
                CategoryEntity("Bonus", "PEMASUKAN", "#10B981", "star"),
                CategoryEntity("THR", "PEMASUKAN", "#F59E0B", "celebration"),
                CategoryEntity("Penjualan", "PEMASUKAN", "#8B5CF6", "shopping_bag"),
                CategoryEntity("Investasi", "PEMASUKAN", "#EC4899", "trending_up"),
                CategoryEntity("Lainnya (Pemasukan)", "PEMASUKAN", "#6B7280", "more_horiz"),

                // Pengeluaran
                CategoryEntity("Makan", "PENGELUARAN", "#10B981", "restaurant"),
                CategoryEntity("Transportasi", "PENGELUARAN", "#3B82F6", "directions_car"),
                CategoryEntity("Belanja", "PENGELUARAN", "#F59E0B", "shopping_cart"),
                CategoryEntity("Tagihan", "PENGELUARAN", "#EF4444", "receipt"),
                CategoryEntity("Internet", "PENGELUARAN", "#06B6D4", "wifi"),
                CategoryEntity("Pulsa", "PENGELUARAN", "#3B82F6", "phone_android"),
                CategoryEntity("Pendidikan", "PENGELUARAN", "#6366F1", "school"),
                CategoryEntity("Kesehatan", "PENGELUARAN", "#EC4899", "local_hospital"),
                CategoryEntity("Hiburan", "PENGELUARAN", "#8B5CF6", "sports_esports"),
                CategoryEntity("Cicilan", "PENGELUARAN", "#F59E0B", "credit_card"),
                CategoryEntity("Pajak", "PENGELUARAN", "#EF4444", "gavel"),
                CategoryEntity("Donasi", "PENGELUARAN", "#EC4899", "favorite"),
                CategoryEntity("Lainnya (Pengeluaran)", "PENGELUARAN", "#6B7280", "more_horiz")
            )
            for (cat in defaultCategories) {
                financeDao.insertCategory(cat)
            }








        }
    }
}
