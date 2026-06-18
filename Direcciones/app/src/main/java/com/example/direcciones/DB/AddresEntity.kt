package com.example.direcciones.DB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey val addressId: Int,
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val stateProvince: String,
    val countryRegion: String,
    val postalCode: String,
    val rowguid: String,
    val modifiedDate: String
)