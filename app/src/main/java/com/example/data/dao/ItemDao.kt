package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE businessId = :businessId OR businessId = 0 ORDER BY name ASC")
    fun getItemsByBusiness(businessId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE (businessId = :businessId OR businessId = 0) AND stockQty <= minStockAlert")
    fun getLowStockItems(businessId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET stockQty = stockQty + :delta WHERE id = :id")
    suspend fun adjustStock(id: Long, delta: Double)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()
}
