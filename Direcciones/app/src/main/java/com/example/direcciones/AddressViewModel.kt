package com.example.direcciones

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.direcciones.DB.AddressEntity
import com.example.direcciones.DB.AddressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections.emptyList

data class UiState(
    val loading: Boolean = false,
    val error: String? = null,
    val count: Int = 0
)

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val repo: AddressRepository,
    private val parser: AddressCsvParser
) : ViewModel() {



    fun importCsv(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            // marca loading manualmente si quieres (o crea otro StateFlow)
            try {
                val items = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        parser.parse(input)
                    } ?: emptyList()
                }

                withContext(Dispatchers.IO) {
                    repo.replaceAll(items)
                }
            } catch (e: Exception) {
                // aquí podrías actualizar un MutableStateFlow de error
                e.printStackTrace()
            }
        }
    }
    // Lista de direcciones (Room Flow -> StateFlow para Compose)
    val addresses: StateFlow<List<AddressEntity>> =
        repo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- UI state for editing dialog ---
    private val _editingAddress = MutableStateFlow<AddressEntity?>(null)
    val editingAddress: StateFlow<AddressEntity?> = _editingAddress

    private val _editingCity = MutableStateFlow("")
    val editingCity: StateFlow<String> = _editingCity

    private val _editingState = MutableStateFlow("")
    val editingState: StateFlow<String> = _editingState

    // Loading while saving
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    // Messages to show (Snackbars / Toasts)
    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    // --- Actions ---

    /** Open dialog and initialize editing fields with the AddressEntity values */
    fun openEditDialog(address: AddressEntity) {
        _editingAddress.value = address
        _editingCity.value = address.city
        _editingState.value = address.stateProvince
    }

    /** Close dialog and clear editing fields */
    fun closeEditDialog() {
        _editingAddress.value = null
        _editingCity.value = ""
        _editingState.value = ""
    }

    /** Update the in-VM copy of the city (two-way binding alternative) */
    fun onEditingCityChanged(newCity: String) {
        _editingCity.value = newCity
    }

    /** Update the in-VM copy of the state (two-way binding alternative) */
    fun onEditingStateChanged(newState: String) {
        _editingState.value = newState
    }

    /** Validate current inputs (simple) */
    private fun isEditingValid(addressId: Int, ciudad: String, estado: String): Boolean {
        val city = ciudad.trim()
        val state = estado.trim()
        return city.isNotEmpty() && state.isNotEmpty()
    }

    /**
     * Save only the city and stateProvince using the repository (preferred: single-column update query).
     * Emits UI message on success/error and closes dialog when done.
     */
    fun saveCityState(addressId: Int, ciudad: String, estado: String) {
        val target = addressId ?: return
        val newCity = ciudad.trim()
        val newState = estado.trim()

        if (!isEditingValid(target,newCity, newState)) {
            viewModelScope.launch { _uiMessage.emit("City and State cannot be blank") }
            return
        }
        val modified = nowIsoString()
        viewModelScope.launch {
            _isSaving.value = true
            try {
                withContext(Dispatchers.IO) {
                    repo.updateCityStateAndModified(target, newCity, newState, modified)

                }
                _uiMessage.emit("Dirección actualizada")
                closeEditDialog()
            } catch (e: Exception) {
                _uiMessage.emit("Error al actualizar: ${e.localizedMessage ?: e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Alternative: update the whole AddressEntity (if you prefer @Update)
     * This method creates a new AddressEntity copying fields, then updates via repo.update(entity)
     */
    /*
    fun saveWholeEntity() {
        val target = _editingAddress.value ?: return
        val newCity = _editingCity.value.trim()
        val newState = _editingState.value.trim()

        if (!isEditingValid()) {
            viewModelScope.launch { _uiMessage.emit("City and State cannot be blank") }
            return
        }

        val updated = target.copy(city = newCity, stateProvince = newState)
        viewModelScope.launch {
            _isSaving.value = true
            try {
                withContext(Dispatchers.IO) {
                    repo.updateAddress(updated) // implement in repository/dao as @Update
                }
                _uiMessage.emit("Dirección actualizada")
                closeEditDialog()
            } catch (e: Exception) {
                _uiMessage.emit("Error al actualizar: ${e.localizedMessage ?: e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }
*/



    // dentro del ViewModel
    private fun nowIsoString(): String {
        // Formato: 2026-02-28T14:23:00-05:00
        return ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}