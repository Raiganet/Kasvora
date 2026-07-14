package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,          // YYYY-MM-DD
    val time: String,          // HH:MM
    val amount: Double,
    val type: String,          // "PEMASUKAN" or "PENGELUARAN"
    val category: String,      // e.g. "Gaji", "Makan"
    val note: String = "",
    val paymentMethod: String, // "Cash", "Transfer", "QRIS", "Debit", "Kredit", "E-Wallet"
    val status: String,        // "PENDING", "BERHASIL", "DIBATALKAN"
    val attachmentUri: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String, // Budget per category
    val limitAmount: Double,
    val month: String                 // YYYY-MM
)

@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val note: String? = null
)

@Entity(tableName = "debts_credits")
data class DebtCreditEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val amount: Double,
    val type: String,          // "HUTANG" or "PIUTANG"
    val dueDate: String,       // YYYY-MM-DD
    val description: String = "",
    val status: String,        // "BELUM_LUNAS", "LUNAS"
    val isReminderActive: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val type: String,          // "PEMASUKAN" or "PENGELUARAN"
    val colorHex: String,      // e.g. "#10B981"
    val iconName: String       // e.g. "Restaurant", "DirectionsCar"
)
