package com.gradecalculator.services

import com.gradecalculator.models.ProcessedFile
import com.gradecalculator.models.StudentRecord
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// PDF Report Generation (iText 7)
// ─────────────────────────────────────────────────────────────────────────────

class PDFGenerationService {

    private val blueColor  = DeviceRgb(33, 150, 243)
    private val violetColor = DeviceRgb(156, 39, 176)
    private val greyLight  = DeviceRgb(245, 245, 245)

    fun generateReport(processedFile: ProcessedFile, destination: File) {
        val pdfWriter   = PdfWriter(destination)
        val pdfDocument = PdfDocument(pdfWriter)
        val document    = Document(pdfDocument)

        // ── Title ─────────────────────────────────────────────────────────────
        document.add(
            Paragraph("Grade Report")
                .setFontSize(24f)
                .setBold()
                .setFontColor(blueColor)
                .setTextAlignment(TextAlignment.CENTER)
        )

        // ── Metadata ──────────────────────────────────────────────────────────
        document.add(
            Paragraph("File: ${processedFile.fileName}  |  " +
                "Date: ${processedFile.formattedDate()}  |  " +
                "Scale: ${processedFile.gradingScaleUsed}")
                .setFontSize(10f)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(12f)
        )

        // ── Statistics summary ────────────────────────────────────────────────
        val stats = processedFile.statistics
        if (stats != null) {
            document.add(
                Paragraph("Class Statistics")
                    .setFontSize(14f)
                    .setBold()
                    .setFontColor(violetColor)
                    .setMarginTop(8f)
            )

            val statsTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f)))
                .useAllAvailableWidth()
                .setMarginBottom(16f)

            listOf(
                "Total Students" to stats.totalStudents.toString(),
                "Average"        to "%.2f".format(stats.average),
                "Highest"        to "%.2f".format(stats.highest),
                "Pass Rate"      to "${"%.1f".format(stats.passRate)}%"
            ).forEach { (label, value) ->
                statsTable.addCell(
                    Cell().add(Paragraph(label).setFontSize(9f).setFontColor(ColorConstants.GRAY))
                          .add(Paragraph(value).setFontSize(13f).setBold())
                          .setBorder(null)
                          .setBackgroundColor(greyLight)
                          .setPadding(8f)
                )
            }
            document.add(statsTable)
        }

        // ── Grade Distribution ────────────────────────────────────────────────
        if (stats != null && stats.gradeDistribution.isNotEmpty()) {
            document.add(
                Paragraph("Grade Distribution")
                    .setFontSize(14f).setBold().setFontColor(violetColor)
            )
            val distTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
                .useAllAvailableWidth().setMarginBottom(16f)
            distTable.addHeaderCell(headerCell("Grade"))
            distTable.addHeaderCell(headerCell("Count"))
            stats.gradeDistribution.entries
                .sortedBy { it.key }
                .forEach { (grade, count) ->
                    distTable.addCell(Cell().add(Paragraph(grade)))
                    distTable.addCell(Cell().add(Paragraph(count.toString())))
                }
            document.add(distTable)
        }

        // ── Student Table ─────────────────────────────────────────────────────
        document.add(
            Paragraph("Student Results")
                .setFontSize(14f).setBold().setFontColor(violetColor)
        )

        val table = Table(UnitValue.createPercentArray(floatArrayOf(1.2f, 3f, 1f, 1f, 1.2f, 0.8f, 1.5f)))
            .useAllAvailableWidth()

        listOf("ID", "Name", "CA", "Exam", "Final", "Grade", "Status")
            .forEach { table.addHeaderCell(headerCell(it)) }

        processedFile.students.forEachIndexed { idx, student ->
            val bgColor = if (idx % 2 == 0) greyLight else DeviceRgb(255, 255, 255)
            addStudentRow(table, student, bgColor)
        }

        document.add(table)

        // ── Footer ────────────────────────────────────────────────────────────
        document.add(
            Paragraph("Generated by GradeCalculator  •  ${java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            )}")
                .setFontSize(8f)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(24f)
        )

        document.close()
    }

    private fun headerCell(text: String): Cell =
        Cell().add(Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
              .setBackgroundColor(blueColor)
              .setPadding(6f)

    private fun addStudentRow(table: Table, s: StudentRecord, bg: DeviceRgb) {
        listOf(s.id, s.name, "%.1f".format(s.caScore), "%.1f".format(s.examScore),
               "%.1f".format(s.finalScore), s.grade, s.getGradeStatus())
            .forEach { value ->
                table.addCell(
                    Cell().add(Paragraph(value).setFontSize(9f))
                          .setBackgroundColor(bg)
                          .setPadding(4f)
                )
            }
    }
}
