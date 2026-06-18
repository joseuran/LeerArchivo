package com.example.direcciones.DB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressBDao {

    @Query("SELECT * FROM addresses ORDER BY addressId")
    fun observeAll(): Flow<List<AddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AddressEntity>): List<Long>

    @Query("DELETE FROM addresses")
    suspend fun clear(): Int

    @Query("""
       UPDATE addresses
       SET City = :city,
           StateProvince = :state,
           ModifiedDate = :modifiedDate
       WHERE AddressID = :id
    """)
    suspend fun updateCityStateAndModified(id: Int, city: String, state: String, modifiedDate: String): Int


    // Optional if you want to update whole Entity
    @Update
    suspend fun update(entity: AddressEntity): Int
}