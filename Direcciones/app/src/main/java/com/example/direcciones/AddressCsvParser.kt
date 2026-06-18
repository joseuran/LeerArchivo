package com.example.direcciones

import com.example.direcciones.DB.AddressEntity
import org.apache.commons.csv.CSVFormat
import java.io.InputStream
import java.io.InputStreamReader

import javax.inject.Inject

class AddressCsvParser @Inject constructor()  {

    fun parse(input: InputStream): List<AddressEntity> {
        val reader = InputStreamReader(input, Charsets.UTF_8)

        val format = CSVFormat.DEFAULT
            .builder()
            .setHeader() // usa la primera fila como header
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build()

        val records = format.parse(reader)

        return records.map { r ->
            AddressEntity(
                addressId = r.get("AddressID").toInt(),
                addressLine1 = r.get("AddressLine1"),
                addressLine2 = r.get("AddressLine2").takeIf { it.isNotBlank() },
                city = r.get("City"),
                stateProvince = r.get("StateProvince"),
                countryRegion = r.get("CountryRegion"),
                postalCode = r.get("PostalCode"),
                rowguid = r.get("rowguid"),
                modifiedDate = r.get("ModifiedDate")
            )
        }
    }
}