package com.gradecalculator.viewmodels

import androidx.compose.runtime.*
import com.gradecalculator.controllers.ExportController
import com.gradecalculator.controllers.GradeController
import com.gradecalculator.controllers.ImportController
import com.gradecalculator.models.*
import com.gradecalculator.utils.FormattingUtils
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// Shared Application State (ViewModel)
// ─────────────────────────────────────────────────────────────────────────────

class AppViewModel {

    // ── Navigation ────────────────────────────────────────────────────────────
    var currentView by mutableStateOf(AppView.HOME)

    // ── Theme ─────────────────────────────────────────────────────────────────
    var isDarkTheme by mutableStateOf(false)

    // ── Grading Scales ────────────────────────────────────────────────────────
    var gradingScales by mutableStateOf(listOf(GradingScale.default(), GradingScale.weighted()))
    var activeScale   by mutableStateOf(gradingScales.first())

    // ── Import state (Home view) ──────────────────────────────────────────────
    var importedStudents  by mutableStateOf<List<StudentRecord>>(emptyList())
    var importWarnings    by mutableStateOf<List<String>>(emptyList())
    var currentFile       by mutableStateOf<ProcessedFile?>(null)
    var isLoading         by mutableStateOf(false)
    var loadingMessage    by mutableStateOf("")
    var errorMessage      by mutableStateOf<String?>(null)
    var successMessage    by mutableStateOf<String?>(null)
    var googleSheetUrl    by mutableStateOf("")

    // ── Search / filter (Home view table) ────────────────────────────────────
    var searchQuery       by mutableStateOf("")
    var sortColumn        by mutableStateOf("name")
    var sortAscending     by mutableStateOf(true)
    var currentPage       by mutableStateOf(0)
    val pageSize          = 20

    // ── Vault ─────────────────────────────────────────────────────────────────
    var vault             by mutableStateOf<List<ProcessedFile>>(emptyList())
    var vaultSearch       by mutableStateOf("")
    var selectedVaultFile by mutableStateOf<ProcessedFile?>(null)
    var vaultViewMode     by mutableStateOf(VaultViewMode.GRID)

    // ── Grade curve ───────────────────────────────────────────────────────────
    var curveEnabled      by mutableStateOf(false)
    var curvePoints       by mutableStateOf(0.0)

    // ── Controllers ───────────────────────────────────────────────────────────
    private val importController  = ImportController()
    private val exportController  = ExportController()
    private val gradeController   = GradeController()
    private val scope             = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val json              = Json { prettyPrint = true; encodeDefaults = true }

    // ── Persistence ───────────────────────────────────────────────────────────
    private val appDataDir = File(System.getProperty("user.home"), ".gradecalculator").also { it.mkdirs() }
    private val vaultFile  = File(appDataDir, "vault.json")

    init { loadVault() }

    // ─────────────────────────────────────────────────────────────────────────
    // Derived / filtered lists
    // ─────────────────────────────────────────────────────────────────────────

    val filteredStudents: List<StudentRecord>
        get() {
            val q = searchQuery.lowercase()
            var list = if (q.isBlank()) importedStudents
                       else importedStudents.filter {
                           it.name.lowercase().contains(q) || it.id.lowercase().contains(q)
                       }
            list = when (sortColumn) {
                "id"         -> if (sortAscending) list.sortedBy { it.id }         else list.sortedByDescending { it.id }
                "name"       -> if (sortAscending) list.sortedBy { it.name }       else list.sortedByDescending { it.name }
                "caScore"    -> if (sortAscending) list.sortedBy { it.caScore }    else list.sortedByDescending { it.caScore }
                "examScore"  -> if (sortAscending) list.sortedBy { it.examScore }  else list.sortedByDescending { it.examScore }
                "finalScore" -> if (sortAscending) list.sortedBy { it.finalScore } else list.sortedByDescending { it.finalScore }
                "grade"      -> if (sortAscending) list.sortedBy { it.grade }      else list.sortedByDescending { it.grade }
                else         -> list
            }
            return list
        }

    val pagedStudents: List<StudentRecord>
        get() = filteredStudents.drop(currentPage * pageSize).take(pageSize)

    val totalPages: Int
        get() = maxOf(1, (filteredStudents.size + pageSize - 1) / pageSize)

    val passRate: Double
        get() = importedStudents.calculatePassRate()

    val topPerformers: List<StudentRecord>
        get() = importedStudents.topPerformers(5)

    val gradeDistribution: Map<String, Int>
        get() = importedStudents
            .groupingBy { if (it.grade.isBlank()) "Ungraded" else it.grade }
            .eachCount()

    val failCount: Int
        get() = importedStudents.count { !it.hasPassed() }

    val excellentCount: Int
        get() = importedStudents.count { it.finalScore >= 70 }

    val filteredVault: List<ProcessedFile>
        get() {
            val q = vaultSearch.lowercase()
            return if (q.isBlank()) vault
                   else vault.filter { it.fileName.lowercase().contains(q) }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────────────────────────────────

    fun toggleTheme()     { isDarkTheme = !isDarkTheme }
    fun navigateTo(v: AppView) { currentView = v }

    fun toggleSort(col: String) {
        if (sortColumn == col) sortAscending = !sortAscending
        else { sortColumn = col; sortAscending = true }
        currentPage = 0
    }

    fun importFile(file: File) {
        scope.launch {
            setLoading("Importing ${file.name}…")
            try {
                val result       = importController.importFile(file, activeScale)
                val duplicates = result.processedFile.students.detectDuplicates()
                val duplicateWarnings = if (duplicates.isNotEmpty())
                    listOf("Duplicate student IDs detected: ${duplicates.joinToString()}")
                else emptyList()

                importedStudents = result.processedFile.students
                importWarnings   = result.warnings + result.invalidStudents + duplicateWarnings
                currentFile      = result.processedFile
                currentPage      = 0
                showSuccess("Imported ${result.processedFile.students.size} students from ${file.name}")
            } catch (e: Exception) {
                showError("Import failed: ${e.message}")
            } finally { clearLoading() }
        }
    }

    fun importGoogleSheet(url: String) {
        scope.launch {
            setLoading("Fetching from Google Sheets…")
            try {
                val result       = importController.importGoogleSheet(url, activeScale)
                val duplicates = result.processedFile.students.detectDuplicates()
                val duplicateWarnings = if (duplicates.isNotEmpty())
                    listOf("Duplicate student IDs detected: ${duplicates.joinToString()}")
                else emptyList()

                importedStudents = result.processedFile.students
                importWarnings   = duplicateWarnings
                currentFile      = result.processedFile
                currentPage      = 0
                showSuccess("Fetched ${result.processedFile.students.size} students from sheet")
            } catch (e: Exception) {
                showError("Google Sheets import failed: ${e.message}")
            } finally { clearLoading() }
        }
    }

    fun loadSampleData() {
        scope.launch {
            setLoading("Loading sample data…")
            try {
                val result       = importController.importSampleData(activeScale)
                val duplicates = result.processedFile.students.detectDuplicates()
                val duplicateWarnings = if (duplicates.isNotEmpty())
                    listOf("Duplicate student IDs detected: ${duplicates.joinToString()}")
                else emptyList()

                importedStudents = result.processedFile.students
                importWarnings   = result.warnings + duplicateWarnings
                currentFile      = result.processedFile
                currentPage      = 0
                showSuccess("Loaded ${result.processedFile.students.size} sample students")
            } catch (e: Exception) {
                showError("Failed to load sample data: ${e.message}")
            } finally { clearLoading() }
        }
    }

    fun processGrades() {
        scope.launch {
            setLoading("Processing grades…")
            try {
                gradeController.updateGradingScale(activeScale)
                var processed = gradeController.processStudents(importedStudents)
                if (curveEnabled && curvePoints > 0) {
                    processed = gradeController.applyGradeCurve(processed, curvePoints)
                }
                val stats = gradeController.calculateClassStatistics(processed)
                importedStudents = processed
                currentFile = currentFile?.copy(students = processed, statistics = stats)
                showSuccess("Grades processed for ${processed.size} students")
            } catch (e: Exception) {
                showError("Processing failed: ${e.message}")
            } finally { clearLoading() }
        }
    }

    fun deleteStudent(id: String) {
        val removed = importedStudents.any { it.id == id }
        if (!removed) return
        importedStudents = importedStudents.filter { it.id != id }
        if (currentPage >= totalPages) currentPage = totalPages - 1
        showSuccess("Deleted student $id")
    }

    fun addStudent(student: StudentRecord) {
        if (importedStudents.any { it.id.equals(student.id, ignoreCase = true) }) {
            return showError("Student with ID '${student.id}' already exists")
        }
        importedStudents = importedStudents + student
        showSuccess("Added student ${student.name}")
    }

    fun saveToVault() {
        val file = currentFile ?: return showError("No data to save")
        vault = vault + file
        persistVault()
        showSuccess("Saved '${file.fileName}' to Vault")
    }

    fun deleteFromVault(id: String) {
        vault = vault.filter { it.id != id }
        if (selectedVaultFile?.id == id) selectedVaultFile = null
        persistVault()
        showSuccess("Removed from Vault")
    }

    fun exportFile(processedFile: ProcessedFile, format: ExportFormat, destination: File) {
        scope.launch {
            setLoading("Exporting as ${format.name}…")
            try {
                exportController.export(processedFile, format, destination)
                showSuccess("Exported to ${destination.absolutePath}")
            } catch (e: Exception) {
                showError("Export failed: ${e.message}")
            } finally { clearLoading() }
        }
    }

    fun addGradingScale(scale: GradingScale) {
        gradingScales = gradingScales + scale
    }

    fun setDefaultScale(scale: GradingScale) {
        activeScale = scale
        gradingScales = gradingScales.map { it.copy(isDefault = it.id == scale.id) }
    }

    fun deleteGradingScale(id: String) {
        gradingScales = gradingScales.filter { it.id != id }
        if (activeScale.id == id) activeScale = gradingScales.firstOrNull() ?: GradingScale.default()
    }

    fun clearCurrentImport() {
        importedStudents = emptyList()
        currentFile      = null
        importWarnings   = emptyList()
        searchQuery      = ""
        currentPage      = 0
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Messaging helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun setLoading(msg: String)  { isLoading = true;  loadingMessage = msg }
    private fun clearLoading()           { isLoading = false; loadingMessage = "" }

    fun showError(msg: String) {
        scope.launch(Dispatchers.Main) {
            errorMessage = msg
            delay(4000); errorMessage = null
        }
    }

    fun showSuccess(msg: String) {
        scope.launch(Dispatchers.Main) {
            successMessage = msg
            delay(3000); successMessage = null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vault persistence
    // ─────────────────────────────────────────────────────────────────────────

    private fun persistVault() {
        try { vaultFile.writeText(json.encodeToString(vault)) } catch (_: Exception) {}
    }

    private fun loadVault() {
        try {
            if (vaultFile.exists()) {
                vault = json.decodeFromString(vaultFile.readText())
            }
        } catch (_: Exception) { vault = emptyList() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────────────────

enum class AppView { HOME, VAULT, SETTINGS }
enum class VaultViewMode { GRID, LIST }
