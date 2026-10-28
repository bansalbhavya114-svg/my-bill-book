package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 1,
    val category: String = "Rent", // Rent, Electricity, Salaries, Transport, Packaging, Tea/Snacks, Marketing, Other
    val amount: Double,
    val paymentMode: String = "CASH",
    val paidTo: String = "",
    val notes: String = "",
    val date: Long = System.currentTimeMillis()
)
