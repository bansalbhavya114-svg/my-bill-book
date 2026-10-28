package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 1,
    val name: String,
    val sku: String = "",
    val hsnCode: String = "",
    val category: String = "General",
    val unit: String = "PCS",
    val mrp: Double = 0.0,
    val salePrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val taxRate: Double = 18.0, // 0, 5, 12, 18, 28
    val stockQty: Double = 0.0,
    val minStockAlert: Double = 5.0
)
