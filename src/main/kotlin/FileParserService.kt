package services

import com.opencsv.CSVReaderBuilder
import models.StudentRecord
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileReader

object FileParserService {

    /** Auto-detects file type and delegates to the appropriate parser. */
    fun parseFile(file: File): List<StudentRecord> = when {
        file.extension.lowercase() in listOf("xlsx", "xls") -> parseExcel(file)
        file.extension.lowercase() == "csv"                  -> parseCsv(file)
        else -> throw IllegalArgumentException("Unsupported file type: .${file.extension}")
    }

    // ─── CSV ──────────────────────────────────────────────────────────────────
    fun parseCsv(file: File): List<StudentRecord> {
        val reader = CSVReaderBuilder(FileReader(file)).withSkipLines(0).build()
        val rows = reader.readAll()
        reader.close()

        if (rows.isEmpty()) throw IllegalStateException("CSV file is empty.")

        val headers = rows.first().map { it.trim().lowercase() }
        val idIdx     = findColumnIndex(headers, listOf("id", "student_id", "studentid", "no", "number", "sn"))
        val nameIdx   = findColumnIndex(headers, listOf("name", "student_name", "studentname", "full_name", "fullname"))
        val caIdx     = findColumnIndex(headers, listOf("ca", "ca_score", "cascore", "continuous", "coursework", "test"))
        val examIdx   = findColumnIndex(headers, listOf("exam", "exam_score", "examscore", "examination", "final_exam"))

        if (nameIdx < 0) throw IllegalStateException("Could not detect 'Name' column. Headers found: ${rows.first().toList()}")
        if (caIdx   < 0 || examIdx < 0) throw IllegalStateException(
            "Could not detect CA and/or Exam columns. Headers found: ${rows.first().toList()}"
        )

        return rows.drop(1).mapIndexedNotNull { index, row ->
            if (row.all { it.isBlank() }) return@mapIndexedNotNull null
            val id   = if (idIdx >= 0 && idIdx < row.size) row[idIdx].trim() else (index + 1).toString()
            val name = if (nameIdx < row.size) row[nameIdx].trim() else "Student ${index + 1}"
            val ca   = row.getOrNull(caIdx)?.trim()?.toDoubleOrNull() ?: 0.0
            val exam = row.getOrNull(examIdx)?.trim()?.toDoubleOrNull() ?: 0.0
            StudentRecord(id = id, name = name, caScore = ca, examScore = exam)
        }
    }

    // ─── Excel ────────────────────────────────────────────────────────────────
    fun parseExcel(file: File): List<StudentRecord> {
        val workbook = WorkbookFactory.create(file)
        val sheet    = workbook.getSheetAt(0)
        val rows     = sheet.toList()
        workbook.close()

        if (rows.isEmpty()) throw IllegalStateException("Excel sheet is empty.")

        val headerRow = rows.first()
        val headers   = (0 until headerRow.lastCellNum).map {
            headerRow.getCell(it)?.toString()?.trim()?.lowercase() ?: ""
        }

        val idIdx   = findColumnIndex(headers, listOf("id", "student_id", "no", "number", "sn"))
        val nameIdx = findColumnIndex(headers, listOf("name", "student_name", "full_name"))
        val caIdx   = findColumnIndex(headers, listOf("ca", "ca_score", "continuous", "coursework", "test"))
        val examIdx = findColumnIndex(headers, listOf("exam", "exam_score", "examination"))

        if (nameIdx < 0) throw IllegalStateException("Could not detect 'Name' column in Excel. Headers: $headers")
        if (caIdx   < 0 || examIdx < 0) throw IllegalStateException(
            "Could not detect CA and/or Exam columns in Excel. Headers: $headers"
        )

        return rows.drop(1).mapIndexedNotNull { index, row ->
            val nameCell = row.getCell(nameIdx)
            if (nameCell == null || nameCell.toString().isBlank()) return@mapIndexedNotNull null

            val id   = if (idIdx >= 0) row.getCell(idIdx)?.toString()?.trim() ?: (index + 1).toString()
                       else (index + 1).toString()
            val name = nameCell.toString().trim()
            val ca   = getCellDouble(row, caIdx)
            val exam = getCellDouble(row, examIdx)

            StudentRecord(id = id, name = name, caScore = ca, examScore = exam)
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private fun findColumnIndex(headers: List<String>, candidates: List<String>): Int {
        for (c in candidates) {
            val i = headers.indexOfFirst { it == c || it.contains(c) }
            if (i >= 0) return i
        }
        return -1
    }

    private fun getCellDouble(row: org.apache.poi.ss.usermodel.Row, idx: Int): Double {
        if (idx < 0) return 0.0
        val cell = row.getCell(idx) ?: return 0.0
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING  -> cell.stringCellValue.trim().toDoubleOrNull() ?: 0.0
            else             -> 0.0
        }
    }
}
