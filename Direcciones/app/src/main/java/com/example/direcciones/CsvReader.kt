package com.example.direcciones

import android.content.ContentResolver
import android.net.Uri
import javax.inject.Inject

class CsvReader @Inject constructor(
    private val contentResolver: ContentResolver
) {
    fun readLines(uri: Uri): List<List<String>> {
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "No se pudo abrir el archivo" }
            return input.bufferedReader().readLines()
                .filter { it.isNotBlank() }
                .map { line -> line.split(",").map { it.trim() } }
        }
    }
}