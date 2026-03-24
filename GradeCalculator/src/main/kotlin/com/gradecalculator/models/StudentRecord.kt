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

fun List<StudentRecord>.withRankings(): Map<String, Int> {
    return sortedByDescending { it.finalScore }
        .mapIndexed { index, student -> student.id to (index + 1) }
        .toMap()
}

fun List<StudentRecord>.calculateClassStatistics(): ClassStatistics {
    if (isEmpty()) return ClassStatistics(0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0.0, emptyMap(), 0.0)
    
    val scores = map { it.finalScore }.sorted()
    val mean = scores.average()
    val variance = scores.map { (it - mean) * (it - mean) }.average()
    val stdDev = kotlin.math.sqrt(variance)
    
    val median = if (scores.size % 2 == 0) {
        (scores[scores.size / 2 - 1] + scores[scores.size / 2]) / 2.0
    } else {
        scores[scores.size / 2]
    }
    
    val pass = count { it.hasPassed() }
    val fail = size - pass
    val gradeDistribution = groupingBy { it.grade.ifBlank { "Ungraded" } }.eachCount()
    
    return ClassStatistics(
        average = mean,
        median = median,
        highest = scores.maxOrNull() ?: 0.0,
        lowest = scores.minOrNull() ?: 0.0,
        passCount = pass,
        failCount = fail,
        totalStudents = size,
        standardDeviation = stdDev,
        gradeDistribution = gradeDistribution,
        passRate = calculatePassRate()
    )
}

fun List<StudentRecord>.identifyAtRiskStudents(): List<StudentRecord> {
    if (size < 5) return emptyList()
    val sorted = sortedBy { it.finalScore }
    val bottomThreshold = (size * 0.2).toInt().coerceAtLeast(1)
    return sorted.take(bottomThreshold)
}

fun List<StudentRecord>.getPercentile(student: StudentRecord): Double {
    if (isEmpty()) return 0.0
    val better = count { it.finalScore > student.finalScore }
    return (100.0 * (size - better)) / size
}

fun List<StudentRecord>.demonstrateHigherOrderFunctions() {
    // Lambda: filter passing students and mark them
    val passedStudents = this.processWithRule(
        rule = { it.finalScore >= 40 },
        transformer = { it.copy(grade = "PASS") }
    )
    println("Passed students: ${passedStudents.map { it.name }}")

    // Collection operations chained
    val topPerformers = this
        .filter { it.finalScore > 70 }
        .sortedByDescending { it.finalScore }
    topPerformers.forEach { println("Top: ${it.name} - ${it.finalScore}") }

    // Pass rate
    println("Pass rate: ${"%.1f".format(this.calculatePassRate())}%")
}

fun demonstrateHigherOrderFunctions() {
    val students = listOf(
        StudentRecord("001", "John Doe", caScore = 25.0, examScore = 60.0, finalScore = 85.0),
        StudentRecord("002", "Jane Smith", caScore = 28.0, examScore = 65.0, finalScore = 93.0),
        StudentRecord("003", "Bob Jones", caScore = 10.0, examScore = 20.0, finalScore = 30.0)
    )
    students.demonstrateHigherOrderFunctions()
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
