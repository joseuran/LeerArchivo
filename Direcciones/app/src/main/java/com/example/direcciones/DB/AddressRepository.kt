package com.example.direcciones.DB

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AddressRepository @Inject constructor(private val dao: AddressBDao) {

    fun observeAll(): Flow<List<AddressEntity>> = dao.observeAll()

    suspend fun replaceAll(items: List<AddressEntity>) {
        dao.clear()
        dao.insertAll(items)
    }

    // the single-column update (preferred)
    /*suspend fun updateCityState(id: Int, city: String, state: String): Int {
        return dao.updateCityState(id, city, state)
    }*/
    suspend fun updateCityStateAndModified(id: Int, city: String, state: String, modifiedDate: String): Int {
        return dao.updateCityStateAndModified(id, city, state, modifiedDate)
    }

    // Optional: update whole entity (requires @Update in DAO)
    suspend fun updateAddress(entity: AddressEntity): Int {
        return dao.update(entity)
    }
}