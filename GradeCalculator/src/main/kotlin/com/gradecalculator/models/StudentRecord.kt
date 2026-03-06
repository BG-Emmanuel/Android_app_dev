package com.gradecalculator.models

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// Core Data Classes
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class StudentRecord(
    val id: String,
    val name: String,
    var caScore: Double = 0.0,
    var examScore: Double = 0.0,
    var finalScore: Double = 0.0,
    var grade: String = "",
    var comment: String = "",
    val originalData: Map<String, String> = emptyMap()
) {
    // ── Function 1: Validation ────────────────────────────────────────────────
    fun validateScores(): ValidationResult {
        val errors = mutableListOf<String>()

        if (caScore > 30.0) errors.add("CA score (${caScore}) exceeds maximum of 30")
        if (examScore > 70.0) errors.add("Exam score (${examScore}) exceeds maximum of 70")
        if (caScore + examScore > 100.0) errors.add("Total score exceeds 100")
        if (caScore < 0) errors.add("CA score cannot be negative")
        if (examScore < 0) errors.add("Exam score cannot be negative")
        if (name.isBlank()) errors.add("Student name cannot be empty")
        if (id.isBlank()) errors.add("Student ID cannot be empty")

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            studentName = this.name
        )
    }

    // ── Function 2: Formatting for Export ────────────────────────────────────
    fun formatForExport(format: ExportFormat): Map<String, Any> {
        val base = mapOf(
            "Student ID" to id,
            "Student Name" to name,
            "CA Score" to "%.2f".format(caScore),
            "Exam Score" to "%.2f".format(examScore),
            "Final Score" to "%.2f".format(finalScore),
            "Grade" to grade,
            "Status" to getGradeStatus()
        )
        return when (format) {
            ExportFormat.EXCEL -> base + mapOf("Comment" to comment)
            ExportFormat.CSV -> base
            ExportFormat.PDF -> base + mapOf(
                "Rank" to "",
                "Comment" to comment
            )
            ExportFormat.JSON -> base + mapOf(
                "comment" to comment,
                "originalData" to originalData.toString()
            )
        }
    }

    fun getGradeStatus(): String = when {
        finalScore >= 70 -> "Excellent"
        finalScore >= 60 -> "Very Good"
        finalScore >= 50 -> "Good"
        finalScore >= 40 -> "Pass"
        else -> "Fail"
    }

    fun hasPassed(): Boolean = finalScore >= 40.0
}

// ─────────────────────────────────────────────────────────────────────────────
// Higher-Order Function Demonstrations
// ─────────────────────────────────────────────────────────────────────────────

fun List<StudentRecord>.processWithRule(
    rule: (StudentRecord) -> Boolean,
    transformer: (StudentRecord) -> StudentRecord
): List<StudentRecord> = this.filter(rule).map(transformer)

fun List<StudentRecord>.calculatePassRate(): Double {
    if (isEmpty()) return 0.0
    return count { it.hasPassed() }.toDouble() / size * 100.0
}

fun List<StudentRecord>.topPerformers(n: Int = 5): List<StudentRecord> =
    sortedByDescending { it.finalScore }.take(n)

fun List<StudentRecord>.detectDuplicates(): List<String> {
    val seen = mutableSetOf<String>()
    val duplicates = mutableListOf<String>()
    forEach { student ->
        if (!seen.add(student.id)) duplicates.add(student.id)
    }
    return duplicates
}

fun demonstrateHigherOrderFunctions() {
    val students = listOf(
        StudentRecord("001", "John Doe", caScore = 25.0, examScore = 60.0, finalScore = 85.0),
        StudentRecord("002", "Jane Smith", caScore = 28.0, examScore = 65.0, finalScore = 93.0),
        StudentRecord("003", "Bob Jones", caScore = 10.0, examScore = 20.0, finalScore = 30.0)
    )

    // Lambda: filter passing students and mark them
    val passedStudents = students.processWithRule(
        rule = { it.finalScore >= 40 },
        transformer = { it.copy(grade = "PASS") }
    )
    println("Passed students: ${passedStudents.map { it.name }}")

    // Collection operations chained
    val topPerformers = students
        .filter { it.finalScore > 70 }
        .sortedByDescending { it.finalScore }
    topPerformers.forEach { println("Top: ${it.name} - ${it.finalScore}") }

    // Pass rate
    println("Pass rate: ${"%.1f".format(students.calculatePassRate())}%")
}

// ─────────────────────────────────────────────────────────────────────────────
// Supporting Types
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val studentName: String
)

@Serializable
data class ClassStatistics(
    val average: Double,
    val median: Double,
    val highest: Double,
    val lowest: Double,
    val passCount: Int,
    val failCount: Int,
    val totalStudents: Int,
    val standardDeviation: Double,
    val gradeDistribution: Map<String, Int>,
    val passRate: Double
)

enum class ExportFormat { EXCEL, CSV, PDF, JSON }
enum class FileType { EXCEL, CSV, GOOGLE_SHEET }
