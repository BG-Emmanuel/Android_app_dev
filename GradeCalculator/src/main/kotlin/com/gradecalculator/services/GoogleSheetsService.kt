package com.gradecalculator.services

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.gradecalculator.models.StudentRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// Google Sheets Service
// ─────────────────────────────────────────────────────────────────────────────

class GoogleSheetsService {

    companion object {
        private const val APPLICATION_NAME = "GradeCalculator"
        private val JSON_FACTORY           = GsonFactory.getDefaultInstance()
        private val SCOPES                 = listOf(SheetsScopes.SPREADSHEETS_READONLY)
    }

    private var sheetsService: Sheets? = null

    /** Initialize with a service-account credentials JSON file */
    fun initialize(credentialsFile: File) {
        val credentials = GoogleCredentials
            .fromStream(credentialsFile.inputStream())
            .createScoped(SCOPES)

        val transport = GoogleNetHttpTransport.newTrustedTransport()

        sheetsService = Sheets.Builder(
            transport,
            JSON_FACTORY,
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    /** Extract spreadsheet ID from a Google Sheets URL */
    fun extractSpreadsheetId(url: String): String {
        val regex = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)")
        return regex.find(url)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Invalid Google Sheets URL: $url")
    }

    /** Fetch students from a Google Sheet URL */
    suspend fun fetchStudents(url: String): List<StudentRecord> = withContext(Dispatchers.IO) {
        val service = sheetsService
            ?: throw IllegalStateException("Google Sheets not initialized. Please add credentials in Settings.")

        val spreadsheetId = extractSpreadsheetId(url)

        // Read first sheet, full range
        val response = service.spreadsheets().values()
            .get(spreadsheetId, "Sheet1")
            .execute()

        val rows = response.getValues() ?: return@withContext emptyList()
        if (rows.isEmpty()) return@withContext emptyList()

        val headers = rows[0].map { it.toString().trim().lowercase() }

        fun colIndex(aliases: List<String>): Int =
            headers.indexOfFirst { h -> aliases.any { a -> h.contains(a) } }

        val idIdx   = colIndex(listOf("id", "student id", "reg no"))
        val nameIdx = colIndex(listOf("name", "student name"))
        val caIdx   = colIndex(listOf("ca", "ca score", "coursework"))
        val examIdx = colIndex(listOf("exam", "exam score"))

        rows.drop(1).mapIndexed { idx, row ->
            fun cell(colIdx: Int): String =
                if (colIdx >= 0 && colIdx < row.size) row[colIdx].toString().trim() else ""

            StudentRecord(
                id        = cell(idIdx).ifBlank { "GS_ROW_${idx + 2}" },
                name      = cell(nameIdx).ifBlank { "Unknown_${idx + 2}" },
                caScore   = cell(caIdx).toDoubleOrNull()   ?: 0.0,
                examScore = cell(examIdx).toDoubleOrNull() ?: 0.0
            )
        }
    }

    val isInitialized: Boolean get() = sheetsService != null
}
