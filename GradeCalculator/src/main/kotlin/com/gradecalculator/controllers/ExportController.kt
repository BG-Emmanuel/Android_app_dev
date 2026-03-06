package com.gradecalculator.controllers

import com.gradecalculator.models.*
import com.gradecalculator.services.PDFGenerationService
import com.opencsv.CSVWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

// ─────────────────────────────────────────────────────────────────────────────
// Export Controller — handles all export operations
// ─────────────────────────────────────────────────────────────────────────────

class ExportController {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    // ─────────────────────────────────────────────────────────────────────────
    // Dispatch by format
    // ─────────────────────────────────────────────────────────────────────────

    fun export(
        processedFile: ProcessedFile,
        format: ExportFormat,
        destination: File
    ) {
        when (format) {
            ExportFormat.EXCEL -> exportToExcel(processedFile, destination)
            ExportFormat.CSV   -> exportToCsv(processedFile, destination)
            ExportFormat.PDF   -> PDFGenerationService().generateReport(processedFile, destination)
            ExportFormat.JSON  -> exportToJson(processedFile, destination)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Excel export — multi-sheet workbook
    // ─────────────────────────────────────────────────────────────────────────

    private fun exportToExcel(file: ProcessedFile, dest: File) {
        val workbook = XSSFWorkbook()

        // ── Sheet 1: Grades ───────────────────────────────────────────────────
        val gradesSheet = workbook.createSheet("Grades")
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern         = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont().also {
                it.color = IndexedColors.WHITE.index; it.bold = true
            }
            setFont(font)
        }

        val headers = listOf("Student ID", "Student Name", "CA Score", "Exam Score", "Final Score", "Grade", "Status", "Comment")
        val headerRow = gradesSheet.createRow(0)
        headers.forEachIndexed { col, h ->
            headerRow.createCell(col).also { it.setCellValue(h); it.cellStyle = headerStyle }
        }

        file.students.forEachIndexed { idx, s ->
            val row = gradesSheet.createRow(idx + 1)
            row.createCell(0).setCellValue(s.id)
            row.createCell(1).setCellValue(s.name)
            row.createCell(2).setCellValue(s.caScore)
            row.createCell(3).setCellValue(s.examScore)
            row.createCell(4).setCellValue(s.finalScore)
            row.createCell(5).setCellValue(s.grade)
            row.createCell(6).setCellValue(s.getGradeStatus())
            row.createCell(7).setCellValue(s.comment)
        }
        headers.indices.forEach { gradesSheet.autoSizeColumn(it) }

        // ── Sheet 2: Statistics ──────────────────────────────────────────────
        val stats     = file.statistics
        val statsSheet = workbook.createSheet("Statistics")
        if (stats != null) {
            val statRows = listOf(
                "Total Students"     to stats.totalStudents.toString(),
                "Average Score"      to "%.2f".format(stats.average),
                "Median Score"       to "%.2f".format(stats.median),
                "Highest Score"      to "%.2f".format(stats.highest),
                "Lowest Score"       to "%.2f".format(stats.lowest),
                "Standard Deviation" to "%.2f".format(stats.standardDeviation),
                "Pass Count"         to stats.passCount.toString(),
                "Fail Count"         to stats.failCount.toString(),
                "Pass Rate"          to "${"%.1f".format(stats.passRate)}%"
            )
            statRows.forEachIndexed { i, (label, value) ->
                val r = statsSheet.createRow(i)
                r.createCell(0).setCellValue(label)
                r.createCell(1).setCellValue(value)
            }

            // Grade distribution
            var rowIdx = statRows.size + 1
            statsSheet.createRow(rowIdx++).createCell(0).setCellValue("Grade Distribution")
            stats.gradeDistribution.forEach { (grade, cnt) ->
                val r = statsSheet.createRow(rowIdx++)
                r.createCell(0).setCellValue(grade)
                r.createCell(1).setCellValue(cnt.toDouble())
            }
        }
        statsSheet.autoSizeColumn(0); statsSheet.autoSizeColumn(1)

        FileOutputStream(dest).use { workbook.write(it) }
        workbook.close()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CSV export
    // ─────────────────────────────────────────────────────────────────────────

    private fun exportToCsv(file: ProcessedFile, dest: File) {
        FileWriter(dest).use { fw ->
            CSVWriter(fw).use { writer ->
                writer.writeNext(arrayOf("Student ID","Student Name","CA Score","Exam Score","Final Score","Grade","Status"))
                file.students.forEach { s ->
                    writer.writeNext(arrayOf(
                        s.id, s.name,
                        s.caScore.toString(), s.examScore.toString(),
                        s.finalScore.toString(), s.grade, s.getGradeStatus()
                    ))
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON export
    // ─────────────────────────────────────────────────────────────────────────

    private fun exportToJson(file: ProcessedFile, dest: File) {
        dest.writeText(json.encodeToString(file))
    }
}
