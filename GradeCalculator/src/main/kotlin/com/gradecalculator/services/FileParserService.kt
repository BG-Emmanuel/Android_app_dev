package com.gradecalculator.services

import com.gradecalculator.models.FileType
import com.gradecalculator.models.StudentRecord
import com.gradecalculator.utils.FormattingUtils
import com.opencsv.CSVReaderBuilder
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

// ─────────────────────────────────────────────────────────────────────────────
// File Parsing Service
// ─────────────────────────────────────────────────────────────────────────────

class FileParserService {

    data class ParseResult(
        val students: List<StudentRecord>,
        val warnings: List<String>,
        val fileType: FileType,
        val columnMapping: ColumnMapping
    )

    data class ColumnMapping(
        val idColumn: String?,
        val nameColumn: String?,
        val caColumn: String?,
        val examColumn: String?
    )

    // ── Column name aliases for auto-detection ────────────────────────────────
    private val idAliases   = listOf("id", "student id", "student_id", "reg no", "matric", "regno")
    private val nameAliases = listOf("name", "student name", "fullname", "full name", "student_name")
    private val caAliases   = listOf("ca", "ca score", "continuous assessment", "coursework", "ca_score", "test")
    private val examAliases = listOf("exam", "exam score", "examination", "exams", "exam_score", "final exam")

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun parseFile(file: File): ParseResult {
        return when (file.extension.lowercase()) {
            "xlsx", "xls" -> parseExcel(file)
            "csv"          -> parseCsv(file)
            else           -> throw IllegalArgumentException("Unsupported file type: ${file.extension}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Excel Parsing
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseExcel(file: File): ParseResult {
        val warnings = mutableListOf<String>()
        val students = mutableListOf<StudentRecord>()

        FileInputStream(file).use { fis ->
            val workbook = XSSFWorkbook(fis)
            val sheet    = workbook.getSheetAt(0)

            val headerRow = sheet.getRow(0)
                ?: throw IllegalArgumentException("Excel file has no header row")

            val headers = (0 until headerRow.lastCellNum).map { col ->
                headerRow.getCell(col)?.stringCellValue?.trim() ?: ""
            }

            val mapping = detectColumnMapping(headers)
            validateMapping(mapping, warnings)

            for (rowIdx in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIdx) ?: continue

                fun cellValue(colName: String?): String {
                    if (colName == null) return ""
                    val colIdx = headers.indexOf(colName)
                    if (colIdx < 0) return ""
                    val cell = row.getCell(colIdx) ?: return ""
                    return when (cell.cellType) {
                        CellType.NUMERIC -> cell.numericCellValue.toString()
                        CellType.STRING  -> cell.stringCellValue.trim()
                        CellType.BLANK   -> ""
                        else             -> cell.toString().trim()
                    }
                }

                val id   = cellValue(mapping.idColumn).ifBlank { "ROW_${rowIdx}" }
                val name = cellValue(mapping.nameColumn).ifBlank {
                    warnings.add("Row $rowIdx has no student name"); "Unknown_$rowIdx"
                }

                val caScore   = cellValue(mapping.caColumn).toDoubleOrNull()   ?: 0.0
                val examScore = cellValue(mapping.examColumn).toDoubleOrNull() ?: 0.0

                val originalData = headers.associate { h -> h to cellValue(h) }

                students.add(
                    StudentRecord(
                        id           = id,
                        name         = name,
                        caScore      = caScore,
                        examScore    = examScore,
                        originalData = originalData
                    )
                )
            }
            workbook.close()
        }

        val mapping = detectColumnMapping(
            students.firstOrNull()?.originalData?.keys?.toList() ?: emptyList()
        )

        return ParseResult(students, warnings, FileType.EXCEL, mapping)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CSV Parsing
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseCsv(file: File): ParseResult {
        val warnings = mutableListOf<String>()
        val students = mutableListOf<StudentRecord>()

        InputStreamReader(FileInputStream(file)).use { reader ->
            val csvReader = CSVReaderBuilder(reader).build()
            val allRows   = csvReader.readAll()

            if (allRows.isEmpty()) throw IllegalArgumentException("CSV file is empty")

            val headers = allRows[0].map { it.trim() }
            val mapping = detectColumnMapping(headers)
            validateMapping(mapping, warnings)

            for (rowIdx in 1 until allRows.size) {
                val row = allRows[rowIdx]
                if (row.all { it.isBlank() }) continue

                fun cell(colName: String?): String {
                    if (colName == null) return ""
                    val idx = headers.indexOf(colName)
                    return if (idx in row.indices) row[idx].trim() else ""
                }

                val id   = cell(mapping.idColumn).ifBlank { "ROW_$rowIdx" }
                val name = cell(mapping.nameColumn).ifBlank {
                    warnings.add("Row $rowIdx has no student name"); "Unknown_$rowIdx"
                }
                val caScore   = cell(mapping.caColumn).toDoubleOrNull()   ?: 0.0
                val examScore = cell(mapping.examColumn).toDoubleOrNull() ?: 0.0

                val originalData = headers.zip(row.toList()).toMap()

                students.add(
                    StudentRecord(
                        id           = id,
                        name         = name,
                        caScore      = caScore,
                        examScore    = examScore,
                        originalData = originalData
                    )
                )
            }
        }

        val mapping = detectColumnMapping(
            students.firstOrNull()?.originalData?.keys?.toList() ?: emptyList()
        )

        return ParseResult(students, warnings, FileType.CSV, mapping)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Auto-detect column names
    // ─────────────────────────────────────────────────────────────────────────

    private fun detectColumnMapping(headers: List<String>): ColumnMapping {
        fun findColumn(aliases: List<String>): String? =
            headers.find { h -> aliases.any { a -> h.lowercase().contains(a) } }

        return ColumnMapping(
            idColumn   = findColumn(idAliases),
            nameColumn = findColumn(nameAliases),
            caColumn   = findColumn(caAliases),
            examColumn = findColumn(examAliases)
        )
    }

    private fun validateMapping(mapping: ColumnMapping, warnings: MutableList<String>) {
        if (mapping.nameColumn == null) warnings.add("Could not detect 'Name' column — using row index as name")
        if (mapping.caColumn   == null) warnings.add("Could not detect 'CA Score' column — defaulting to 0")
        if (mapping.examColumn == null) warnings.add("Could not detect 'Exam Score' column — defaulting to 0")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // In-memory demo data (no file required)
    // ─────────────────────────────────────────────────────────────────────────

    fun generateSampleData(count: Int = 20): List<StudentRecord> {
        val names = listOf("Alice", "Bob", "Carol", "David", "Eve", "Frank",
            "Grace", "Hank", "Iris", "Jack", "Karen", "Leo",
            "Mona", "Nick", "Olivia", "Paul", "Quinn", "Rose", "Sam", "Tina")
        return (1..count).map { i ->
            val ca   = (5..30).random().toDouble()
            val exam = (15..70).random().toDouble()
            StudentRecord(
                id        = "S%03d".format(i),
                name      = names[(i - 1) % names.size] + " ${('A' + (i / names.size)).uppercaseChar()}",
                caScore   = ca,
                examScore = exam
            )
        }
    }
}
