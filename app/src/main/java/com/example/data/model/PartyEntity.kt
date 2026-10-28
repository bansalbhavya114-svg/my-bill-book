package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parties")
data class PartyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 1,
    val name: String,
    val type: String = "CUSTOMER", // CUSTOMER, SUPPLIER
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val address: String = "",
    val state: String = "Delhi",
    val stateCode: String = "07",
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0, // Positive = You will receive, Negative = You need to pay
    val creditLimit: Double = 50000.0,
    val paymentTermsDays: Int = 30
)
