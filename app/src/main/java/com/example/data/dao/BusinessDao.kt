package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BusinessProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM business_profiles ORDER BY id ASC")
    fun getAllBusinesses(): Flow<List<BusinessProfileEntity>>

    @Query("SELECT * FROM business_profiles WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentBusiness(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profiles WHERE id = :id LIMIT 1")
    suspend fun getBusinessById(id: Long): BusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(business: BusinessProfileEntity): Long

    @Update
    suspend fun update(business: BusinessProfileEntity)

    @Query("UPDATE business_profiles SET isCurrent = 0")
    suspend fun clearCurrentBusiness()

    @Query("UPDATE business_profiles SET isCurrent = 1 WHERE id = :id")
    suspend fun setCurrentBusiness(id: Long)

    @Query("DELETE FROM business_profiles WHERE businessName LIKE '%Apex%' OR businessName LIKE '%Balaji%'")
    suspend fun deleteFakeBusinesses()

    @Query("DELETE FROM business_profiles")
    suspend fun deleteAllBusinesses()
}
