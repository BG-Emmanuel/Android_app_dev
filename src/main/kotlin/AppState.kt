package state

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import models.*
import services.ExportService
import services.FileParserService
import utils.VaultManager
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppState {

    // ─── Navigation ───────────────────────────────────────────────────────────
    var currentView by mutableStateOf(AppView.HOME)

    // ─── Theme ────────────────────────────────────────────────────────────────
    var isDarkTheme by mutableStateOf(false)

    // ─── Home ─────────────────────────────────────────────────────────────────
    var importedStudents by mutableStateOf<List<StudentRecord>>(emptyList())
    var currentFileName  by mutableStateOf("")
    var isProcessed      by mutableStateOf(false)
    var isLoading        by mutableStateOf(false)
    var searchQuery      by mutableStateOf("")
    var sortColumn       by mutableStateOf(SortColumn.NAME)
    var sortAscending    by mutableStateOf(true)
    var currentPage      by mutableStateOf(0)
    val pageSize = 15

    // ─── Messages ─────────────────────────────────────────────────────────────
    var errorMessage   by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    // ─── Vault ────────────────────────────────────────────────────────────────
    var savedFiles       by mutableStateOf<List<ProcessedFile>>(emptyList())
    var selectedVaultFile by mutableStateOf<ProcessedFile?>(null)
    var vaultSearchQuery by mutableStateOf("")
    var vaultViewMode    by mutableStateOf("grid")

    // ─── Settings ─────────────────────────────────────────────────────────────
    var gradingScales       by mutableStateOf(listOf(DEFAULT_GRADING_SCALE, DISTINCTION_SCALE))
    var selectedScale       by mutableStateOf(DEFAULT_GRADING_SCALE)
    var defaultExportFormat by mutableStateOf(ExportFormat.EXCEL)
    var defaultSaveLocation by mutableStateOf(
        System.getProperty("user.home") + File.separator + "GradeCalculator"
    )

    // ─── Internal ─────────────────────────────────────────────────────────────
    private val scope        = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val vaultManager = VaultManager()
    private var messageJob: Job? = null

    init { loadVault() }

    // ─── Computed: Home ───────────────────────────────────────────────────────
    val filteredStudents: List<StudentRecord>
        get() {
            val q = searchQuery.lowercase().trim()
            var list = if (q.isEmpty()) importedStudents
                       else importedStudents.filter {
                           it.name.lowercase().contains(q) || it.id.lowercase().contains(q)
                       }
            list = when (sortColumn) {
                SortColumn.ID         -> if (sortAscending) list.sortedBy { it.id }         else list.sortedByDescending { it.id }
                SortColumn.NAME       -> if (sortAscending) list.sortedBy { it.name }       else list.sortedByDescending { it.name }
                SortColumn.CA_SCORE   -> if (sortAscending) list.sortedBy { it.caScore }    else list.sortedByDescending { it.caScore }
                SortColumn.EXAM_SCORE -> if (sortAscending) list.sortedBy { it.examScore }  else list.sortedByDescending { it.examScore }
                SortColumn.FINAL_SCORE-> if (sortAscending) list.sortedBy { it.finalScore } else list.sortedByDescending { it.finalScore }
                SortColumn.GRADE      -> if (sortAscending) list.sortedBy { it.grade }      else list.sortedByDescending { it.grade }
            }
            return list
        }

    val paginatedStudents: List<StudentRecord>
        get() = filteredStudents.drop(currentPage * pageSize).take(pageSize)

    val totalPages: Int
        get() = maxOf(1, (filteredStudents.size + pageSize - 1) / pageSize)

    val classStats: ClassStatistics?
        get() = if (importedStudents.isEmpty()) null else importedStudents.computeStatistics()

    // ─── Computed: Vault ──────────────────────────────────────────────────────
    val filteredVaultFiles: List<ProcessedFile>
        get() = if (vaultSearchQuery.isBlank()) savedFiles
                else savedFiles.filter { it.fileName.contains(vaultSearchQuery, ignoreCase = true) }

    // ─── Actions ──────────────────────────────────────────────────────────────

    fun importFile(file: File) {
        scope.launch {
            withContext(Dispatchers.Main) { isLoading = true; errorMessage = null }
            try {
                val students = FileParserService.parseFile(file)
                if (students.isEmpty()) throw IllegalStateException("No student records found in file.")
                withContext(Dispatchers.Main) {
                    importedStudents = students
                    currentFileName  = file.name
                    isProcessed      = false
                    currentPage      = 0
                    searchQuery      = ""
                    isLoading        = false
                    showSuccess("Imported ${students.size} students from '${file.name}'")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading    = false
                    errorMessage = "Import error: ${e.message}"
                }
            }
        }
    }

    fun processGrades() {
        if (importedStudents.isEmpty()) { errorMessage = "No students to process."; return }
        scope.launch {
            withContext(Dispatchers.Main) { isLoading = true }
            val processed = importedStudents.map { s ->
                val ca    = maxOf(0.0, minOf(s.caScore,   30.0))
                val exam  = maxOf(0.0, minOf(s.examScore, 70.0))
                val final = ca + exam
                val grade = determineGrade(final)
                s.copy(caScore = ca, examScore = exam, finalScore = final,
                       grade = grade, status = getStatus(final))
            }
            withContext(Dispatchers.Main) {
                importedStudents = processed
                isProcessed      = true
                isLoading        = false
                showSuccess("Grades processed for ${processed.size} students ✓")
            }
        }
    }

    fun saveToVault() {
        if (!isProcessed || importedStudents.isEmpty()) {
            errorMessage = "Process grades first before saving."
            return
        }
        val pf = ProcessedFile(
            id               = System.currentTimeMillis().toString(),
            fileName         = currentFileName.ifBlank { "Untitled" },
            importDateStr    = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
            students         = importedStudents,
            fileType         = if (currentFileName.endsWith("csv", true)) "CSV" else "EXCEL",
            gradingScaleUsed = selectedScale.name
        )
        savedFiles = listOf(pf) + savedFiles
        vaultManager.saveVault(savedFiles)
        showSuccess("'${pf.fileName}' saved to Vault ✓")
    }

    fun deleteFromVault(fileId: String) {
        savedFiles = savedFiles.filter { it.id != fileId }
        if (selectedVaultFile?.id == fileId) selectedVaultFile = null
        vaultManager.saveVault(savedFiles)
        showSuccess("File removed from Vault")
    }

    fun exportStudents(
        format: ExportFormat,
        students: List<StudentRecord> = importedStudents,
        fileName: String = currentFileName
    ) {
        if (students.isEmpty()) { errorMessage = "Nothing to export."; return }
        scope.launch {
            try {
                val out = ExportService.export(students, format, fileName, defaultSaveLocation)
                withContext(Dispatchers.Main) {
                    showSuccess("Exported as ${format.name} → ${out.absolutePath}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMessage = "Export failed: ${e.message}" }
            }
        }
    }

    fun toggleSort(column: SortColumn) {
        if (sortColumn == column) sortAscending = !sortAscending
        else { sortColumn = column; sortAscending = true }
        currentPage = 0
    }

    fun addOrUpdateScale(scale: GradingScale) {
        gradingScales = if (gradingScales.any { it.id == scale.id })
            gradingScales.map { if (it.id == scale.id) scale else it }
        else gradingScales + scale
    }

    fun setDefaultScale(scale: GradingScale) {
        selectedScale = scale
        gradingScales = gradingScales.map { it.copy(isDefault = it.id == scale.id) }
    }

    fun clearMessages() { errorMessage = null; successMessage = null }

    // ─── Private Helpers ──────────────────────────────────────────────────────
    private fun determineGrade(score: Double): String =
        selectedScale.ranges.find { score >= it.minScore && score <= it.maxScore }?.grade ?: "F"

    private fun getStatus(score: Double): String = when {
        score >= 70 -> "Excellent"
        score >= 60 -> "Very Good"
        score >= 50 -> "Good"
        score >= 40 -> "Pass"
        else        -> "Fail"
    }

    private fun loadVault() { savedFiles = vaultManager.loadVault() }

    private fun showSuccess(msg: String) {
        successMessage = msg
        messageJob?.cancel()
        messageJob = scope.launch {
            delay(4000)
            withContext(Dispatchers.Main) { successMessage = null }
        }
    }
}
