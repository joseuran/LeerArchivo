package com.example.direcciones


import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.direcciones.DB.AddressBDao
import com.example.direcciones.DB.AddressEntity

@Database(entities = [AddressEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun addressDao(): AddressBDao
}