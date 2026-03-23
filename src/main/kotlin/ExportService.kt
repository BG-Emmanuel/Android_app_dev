package services

import models.ExportFormat
import models.StudentRecord
import models.computeStatistics
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.awt.Color
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ExportService {

    fun export(
        students: List<StudentRecord>,
        format: ExportFormat,
        baseName: String,
        saveDir: String
    ): File {
        val dir = File(saveDir).also { it.mkdirs() }
        val ts  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val clean = baseName.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9_\\-]"), "_")

        return when (format) {
            ExportFormat.EXCEL -> exportExcel(students, File(dir, "${clean}_${ts}.xlsx"))
            ExportFormat.CSV   -> exportCsv(students, File(dir, "${clean}_${ts}.csv"))
            ExportFormat.PDF   -> exportPdf(students, File(dir, "${clean}_${ts}.pdf"), baseName)
            ExportFormat.JSON  -> exportJson(students, File(dir, "${clean}_${ts}.json"))
        }
    }

    // ─── Excel Export ─────────────────────────────────────────────────────────
    private fun exportExcel(students: List<StudentRecord>, file: File): File {
        val wb = XSSFWorkbook()

        // --- Sheet 1: Grades ---
        val sheet  = wb.createSheet("Grades")
        val helper = wb.creationHelper

        // Header style
        val headerStyle = wb.createCellStyle().apply {
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = wb.createFont().also {
                it.bold  = true
                it.color = IndexedColors.WHITE.index
            }
            setFont(font)
            alignment = HorizontalAlignment.CENTER
        }
        val headers = listOf("Student ID", "Student Name", "CA Score (/30)", "Exam Score (/70)", "Final Score (/100)", "Grade", "Status")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply { setCellValue(h); cellStyle = headerStyle }
        }

        // Grade colour styles
        val gradeStyles = mapOf(
            "A" to IndexedColors.GREEN.index,
            "B" to IndexedColors.LIGHT_GREEN.index,
            "C" to IndexedColors.GOLD.index,
            "D" to IndexedColors.ORANGE.index,
            "F" to IndexedColors.RED.index
        )

        students.forEachIndexed { idx, s ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(s.id)
            row.createCell(1).setCellValue(s.name)
            row.createCell(2).setCellValue(s.caScore)
            row.createCell(3).setCellValue(s.examScore)
            row.createCell(4).setCellValue(s.finalScore)
            row.createCell(5).apply {
                setCellValue(s.grade)
                val cs = wb.createCellStyle().apply {
                    fillForegroundColor = gradeStyles[s.grade.take(1).uppercase()] ?: IndexedColors.RED.index
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                    val f = wb.createFont().also { it.bold = true }
                    setFont(f)
                    alignment = HorizontalAlignment.CENTER
                }
                cellStyle = cs
            }
            row.createCell(6).setCellValue(s.status)
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        // --- Sheet 2: Statistics ---
        val stats     = students.computeStatistics()
        val statSheet = wb.createSheet("Statistics")
        val boldStyle = wb.createCellStyle().apply {
            val f = wb.createFont().also { it.bold = true }; setFont(f)
        }
        fun statRow(r: Int, label: String, value: String) {
            statSheet.createRow(r).apply {
                createCell(0).apply { setCellValue(label); cellStyle = boldStyle }
                createCell(1).setCellValue(value)
            }
        }
        statRow(0, "Total Students",  stats.totalStudents.toString())
        statRow(1, "Average Score",   "%.2f".format(stats.average))
        statRow(2, "Median Score",    "%.2f".format(stats.median))
        statRow(3, "Highest Score",   "%.2f".format(stats.highest))
        statRow(4, "Lowest Score",    "%.2f".format(stats.lowest))
        statRow(5, "Std Deviation",   "%.2f".format(stats.standardDeviation))
        statRow(6, "Pass Count",      stats.passCount.toString())
        statRow(7, "Fail Count",      stats.failCount.toString())
        statRow(8, "Pass Rate",       "%.1f%%".format(stats.passRate))
        var r = 10
        statRow(r++, "Grade Distribution", "")
        stats.gradeDistribution.entries.sortedBy { it.key }.forEach { (g, c) ->
            statRow(r++, "  Grade $g", "$c students")
        }
        listOf(0, 1).forEach { statSheet.autoSizeColumn(it) }

        file.outputStream().use { wb.write(it) }
        wb.close()
        return file
    }

    // ─── CSV Export ───────────────────────────────────────────────────────────
    private fun exportCsv(students: List<StudentRecord>, file: File): File {
        FileWriter(file).use { w ->
            w.appendLine("Student ID,Student Name,CA Score,Exam Score,Final Score,Grade,Status")
            students.forEach { s ->
                w.appendLine("\"${s.id}\",\"${s.name}\",${s.caScore},${s.examScore},${s.finalScore},${s.grade},${s.status}")
            }
        }
        return file
    }

    // ─── JSON Export ──────────────────────────────────────────────────────────
    private fun exportJson(students: List<StudentRecord>, file: File): File {
        val stats = students.computeStatistics()
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"exportDate\": \"${LocalDateTime.now()}\",")
        sb.appendLine("  \"statistics\": {")
        sb.appendLine("    \"totalStudents\": ${stats.totalStudents},")
        sb.appendLine("    \"average\": ${stats.average},")
        sb.appendLine("    \"highest\": ${stats.highest},")
        sb.appendLine("    \"lowest\": ${stats.lowest},")
        sb.appendLine("    \"passRate\": ${stats.passRate}")
        sb.appendLine("  },")
        sb.appendLine("  \"students\": [")
        students.forEachIndexed { i, s ->
            val comma = if (i < students.lastIndex) "," else ""
            sb.appendLine("    {\"id\":\"${s.id}\",\"name\":\"${s.name}\",\"caScore\":${s.caScore},\"examScore\":${s.examScore},\"finalScore\":${s.finalScore},\"grade\":\"${s.grade}\",\"status\":\"${s.status}\"}$comma")
        }
        sb.appendLine("  ]")
        sb.append("}")
        file.writeText(sb.toString())
        return file
    }

    // ─── PDF Export ───────────────────────────────────────────────────────────
    private fun exportPdf(students: List<StudentRecord>, file: File, title: String): File {
        val doc  = PDDocument()
        val bold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val reg  = PDType1Font(Standard14Fonts.FontName.HELVETICA)
        val stats = students.computeStatistics()

        fun newPage(): Pair<PDPage, PDPageContentStream> {
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            val cs = PDPageContentStream(doc, page)
            return page to cs
        }

        var (page, cs) = newPage()
        val pageW = page.mediaBox.width
        val margin = 50f
        var y = page.mediaBox.height - margin

        fun nextPage() {
            cs.close()
            val r = newPage(); page = r.first; cs = r.second
            y = page.mediaBox.height - margin
        }

        fun text(t: String, x: Float, yPos: Float, font: PDType1Font, size: Float) {
            cs.beginText()
            cs.setFont(font, size)
            cs.newLineAtOffset(x, yPos)
            cs.showText(t)
            cs.endText()
        }

        // Title
        text("Grade Report – $title", margin, y, bold, 16f); y -= 22f
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
        text("Generated: $ts", margin, y, reg, 10f); y -= 20f

        // Stats summary
        cs.setLineWidth(0.5f)
        cs.moveTo(margin, y); cs.lineTo(pageW - margin, y); cs.stroke(); y -= 14f
        text("Total: ${stats.totalStudents}  |  Average: ${"%.1f".format(stats.average)}  |  Highest: ${"%.1f".format(stats.highest)}  |  Lowest: ${"%.1f".format(stats.lowest)}  |  Pass Rate: ${"%.1f".format(stats.passRate)}%", margin, y, bold, 9f)
        y -= 14f
        cs.moveTo(margin, y); cs.lineTo(pageW - margin, y); cs.stroke(); y -= 18f

        // Table header
        val cols = listOf("ID" to 50f, "Name" to 155f, "CA" to 45f, "Exam" to 50f, "Final" to 50f, "Grade" to 45f, "Status" to 75f)
        var xOff = margin
        cols.forEach { (h, w) -> text(h, xOff, y, bold, 9f); xOff += w }
        y -= 4f
        cs.moveTo(margin, y); cs.lineTo(pageW - margin, y); cs.stroke(); y -= 14f

        // Rows
        students.forEach { s ->
            if (y < margin + 30) nextPage()
            xOff = margin
            val cells = listOf(s.id, s.name, "%.1f".format(s.caScore), "%.1f".format(s.examScore),
                "%.1f".format(s.finalScore), s.grade, s.status)
            cells.zip(cols).forEach { (v, col) ->
                val (_, w) = col
                val display = if (v.length > 22) v.take(21) + "…" else v
                text(display, xOff, y, reg, 8.5f)
                xOff += w
            }
            y -= 14f
        }

        cs.close()
        doc.save(file)
        doc.close()
        return file
    }
}
