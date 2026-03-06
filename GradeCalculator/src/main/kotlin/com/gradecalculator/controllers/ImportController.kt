package com.gradecalculator.controllers

import com.gradecalculator.models.*
import com.gradecalculator.services.FileParserService
import com.gradecalculator.services.GoogleSheetsService
import com.gradecalculator.utils.FormattingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

// ─────────────────────────────────────────────────────────────────────────────
// Import Controller — orchestrates file import pipeline
// ─────────────────────────────────────────────────────────────────────────────

class ImportController(
    private val parserService: FileParserService     = FileParserService(),
    private val gradeController: GradeController     = GradeController(),
    private val sheetsService: GoogleSheetsService   = GoogleSheetsService()
) {

    data class ImportResult(
        val processedFile: ProcessedFile,
        val warnings: List<String>,
        val invalidStudents: List<String>
    )

    /** Full import pipeline — runs on IO dispatcher */
    suspend fun importFile(
        file: File,
        gradingScale: GradingScale = GradingScale.default()
    ): ImportResult = withContext(Dispatchers.IO) {

        // Step 1: Parse file
        val parseResult = parserService.parseFile(file)

        // Step 2: Process / cap / calculate grades
        gradeController.updateGradingScale(gradingScale)
        val processed = gradeController.processStudents(parseResult.students)

        // Step 3: Validate
        val invalid = processed
            .map { it.validateScores() }
            .filter { !it.isValid }
            .map { "${it.studentName}: ${it.errors.joinToString()}" }

        // Step 4: Statistics
        val stats = gradeController.calculateClassStatistics(processed)

        // Step 5: Build ProcessedFile
        val pf = ProcessedFile(
            id               = FormattingUtils.generateId(),
            fileName         = file.name,
            importDate       = LocalDateTime.now(),
            students         = processed,
            fileType         = parseResult.fileType,
            gradingScaleUsed = gradingScale.name,
            statistics       = stats
        )

        ImportResult(pf, parseResult.warnings, invalid)
    }

    /** Import from Google Sheets URL */
    suspend fun importGoogleSheet(
        url: String,
        gradingScale: GradingScale = GradingScale.default()
    ): ImportResult = withContext(Dispatchers.IO) {
        val students = sheetsService.fetchStudents(url)
        gradeController.updateGradingScale(gradingScale)
        val processed = gradeController.processStudents(students)
        val stats     = gradeController.calculateClassStatistics(processed)

        val invalid = processed
            .map { it.validateScores() }
            .filter { !it.isValid }
            .map { "${it.studentName}: ${it.errors.joinToString()}" }

        val pf = ProcessedFile(
            id               = FormattingUtils.generateId(),
            fileName         = "GoogleSheet_${System.currentTimeMillis()}",
            importDate       = LocalDateTime.now(),
            students         = processed,
            fileType         = FileType.GOOGLE_SHEET,
            gradingScaleUsed = gradingScale.name,
            statistics       = stats
        )

        ImportResult(pf, emptyList(), invalid)
    }

    /** Load sample/demo data */
    suspend fun importSampleData(
        gradingScale: GradingScale = GradingScale.default()
    ): ImportResult = withContext(Dispatchers.IO) {
        val raw       = parserService.generateSampleData(25)
        gradeController.updateGradingScale(gradingScale)
        val processed = gradeController.processStudents(raw)
        val stats     = gradeController.calculateClassStatistics(processed)

        val pf = ProcessedFile(
            id               = FormattingUtils.generateId(),
            fileName         = "SampleData.demo",
            importDate       = LocalDateTime.now(),
            students         = processed,
            fileType         = FileType.CSV,
            gradingScaleUsed = gradingScale.name,
            statistics       = stats
        )
        ImportResult(pf, listOf("This is demo data — no real file was imported."), emptyList())
    }
}
