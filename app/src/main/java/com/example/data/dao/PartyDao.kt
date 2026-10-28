package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties WHERE businessId = :businessId ORDER BY name ASC")
    fun getPartiesByBusiness(businessId: Long): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE businessId = :businessId AND type = :type ORDER BY name ASC")
    fun getPartiesByType(businessId: Long, type: String): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    suspend fun getPartyById(id: Long): PartyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(party: PartyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parties: List<PartyEntity>)

    @Update
    suspend fun update(party: PartyEntity)

    @Query("UPDATE parties SET currentBalance = currentBalance + :delta WHERE id = :id")
    suspend fun updateBalance(id: Long, delta: Double)

    @Query("DELETE FROM parties WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM parties WHERE businessId = :businessId")
    suspend fun deleteAllPartiesByBusiness(businessId: Long)

    @Query("DELETE FROM parties")
    suspend fun deleteAllParties()
}
