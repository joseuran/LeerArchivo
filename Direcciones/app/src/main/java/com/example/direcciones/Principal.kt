package com.example.direcciones

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.direcciones.DB.AddressEntity

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


    @Composable
    fun AddressScreen(viewModel: AddressViewModel = hiltViewModel()) {
        val items by viewModel.addresses.collectAsState()
        var selected by remember { mutableStateOf<AddressEntity?>(null) }
        val context = LocalContext.current
        val addresses by viewModel.addresses.collectAsState()

        // Mensajes opcionales desde ViewModel (Toast)
        LaunchedEffect(Unit) {
            viewModel.uiMessage.collect { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

            }
        }
        val openCsvLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let {
                // optional: persist permission
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) { /* ignore */ }

                viewModel.importCsv(context.contentResolver, it)
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            Text(
                text = "Direcciones (${addresses.size})",
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = {
                    // mime types for csv/text
                    openCsvLauncher.launch(arrayOf("text/*", "text/csv", "application/vnd.ms-excel"))
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Importar CSV")
            }
            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.addressId }) { item ->
                    AddressRow(address = item, onEdit = { selected = it })
                }
            }
        }

        // Dialogo para editar
        selected?.let { address ->
            EditCityStateDialog(
                initialCity = address.city,
                initialState = address.stateProvince,
                onDismiss = { selected = null },
                onSave = { newCity, newState ->
                    viewModel.saveCityState(address.addressId, newCity.trim(), newState.trim())
                    selected = null
                }
            )
        }
    }

@Composable
fun AddressRow(
    address: AddressEntity,
    onEdit: (AddressEntity) -> Unit, // callback para abrir diálogo
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenido principal (usa weight para ocupar todo el ancho excepto el botón)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "ID: ${address.addressId}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text(text = address.addressLine1, style = MaterialTheme.typography.bodyLarge)
                address.addressLine2?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(text = "City: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Text(text = address.city, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(12.dp))
                    Text(text = "State: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Text(text = address.stateProvince, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(text = "Country: ${address.countryRegion}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Postal: ${address.postalCode}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Modified: ${prettyModifiedDate(address.modifiedDate)}", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Botón de editar alineado a la derecha
            IconButton(
                onClick = { onEdit(address) },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar ciudad y estado"
                )
            }
        }
    }
}


fun prettyModifiedDate(iso: String): String {
    return try {
        val z = ZonedDateTime.parse(iso, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        z.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
    } catch (e: DateTimeParseException) {
        iso // fallback: mostrar el string tal cual
    }
}

@Composable
fun EditCityStateDialog(
    initialCity: String,
    initialState: String,
    onDismiss: () -> Unit,
    onSave: (city: String, state: String) -> Unit
) {
    var city by remember { mutableStateOf(initialCity) }
    var state by remember { mutableStateOf(initialState) }
    val enabled = city.isNotBlank() && state.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar City y State") },
        text = {
            Column {
                TextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State / Province") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(city, state) }, enabled = enabled) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

