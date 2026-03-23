package models

import kotlinx.serialization.Serializable

// ─── Core Data Classes ────────────────────────────────────────────────────────

@Serializable
data class StudentRecord(
    val id: String,
    val name: String,
    var caScore: Double = 0.0,
    var examScore: Double = 0.0,
    var finalScore: Double = 0.0,
    var grade: String = "",
    var status: String = "",
    var hasError: Boolean = false,
    var errorMessage: String = ""
)

@Serializable
data class GradeRange(
    val minScore: Double,
    val maxScore: Double,
    val grade: String,
    val colorHex: String = "#000000"
)

@Serializable
data class GradingScale(
    val id: String,
    val name: String,
    val ranges: List<GradeRange>,
    val isDefault: Boolean = false
)

@Serializable
data class ProcessedFile(
    val id: String,
    val fileName: String,
    val importDateStr: String,
    val students: List<StudentRecord>,
    val fileType: String,
    val gradingScaleUsed: String
) {
    val totalStudents: Int get() = students.size
    val passCount: Int get() = students.count { it.finalScore >= 40.0 }
    val passRate: Double get() = if (totalStudents > 0) (passCount.toDouble() / totalStudents) * 100 else 0.0
    val average: Double get() = if (students.isEmpty()) 0.0 else students.map { it.finalScore }.average()
}

data class ClassStatistics(
    val average: Double,
    val median: Double,
    val highest: Double,
    val lowest: Double,
    val passCount: Int,
    val failCount: Int,
    val totalStudents: Int,
    val standardDeviation: Double,
    val gradeDistribution: Map<String, Int>
) {
    val passRate: Double get() = if (totalStudents > 0) (passCount.toDouble() / totalStudents) * 100 else 0.0
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val studentName: String
)

// ─── Enums ────────────────────────────────────────────────────────────────────

enum class FileType { EXCEL, CSV, GOOGLE_SHEET }
enum class ExportFormat { EXCEL, CSV, PDF, JSON }
enum class AppView { HOME, VAULT, SETTINGS }
enum class SortColumn { ID, NAME, CA_SCORE, EXAM_SCORE, FINAL_SCORE, GRADE }

// ─── Default Grading Scales ───────────────────────────────────────────────────

val DEFAULT_GRADING_SCALE = GradingScale(
    id = "standard",
    name = "Standard (A–F)",
    isDefault = true,
    ranges = listOf(
        GradeRange(70.0, 100.0, "A", "#4CAF50"),
        GradeRange(60.0, 69.99, "B", "#8BC34A"),
        GradeRange(50.0, 59.99, "C", "#FF9800"),
        GradeRange(40.0, 49.99, "D", "#FF5722"),
        GradeRange(0.0,  39.99, "F", "#F44336")
    )
)

val DISTINCTION_SCALE = GradingScale(
    id = "distinction",
    name = "Distinction Scale",
    ranges = listOf(
        GradeRange(80.0, 100.0, "Distinction",  "#2196F3"),
        GradeRange(65.0, 79.99, "Credit",        "#4CAF50"),
        GradeRange(50.0, 64.99, "Merit",         "#FF9800"),
        GradeRange(40.0, 49.99, "Pass",          "#FF5722"),
        GradeRange(0.0,  39.99, "Fail",          "#F44336")
    )
)

// ─── Extension Functions ──────────────────────────────────────────────────────

/** Validates raw CA / Exam scores before processing. */
fun StudentRecord.validateScores(): ValidationResult {
    val errors = mutableListOf<String>()
    if (caScore > 30.0)                errors.add("CA score (${caScore}) exceeds maximum of 30")
    if (examScore > 70.0)              errors.add("Exam score (${examScore}) exceeds maximum of 70")
    if (caScore < 0 || examScore < 0)  errors.add("Scores cannot be negative")
    return ValidationResult(isValid = errors.isEmpty(), errors = errors, studentName = name)
}

/** Formats a student record into a string map for export. */
fun StudentRecord.formatForExport(format: ExportFormat): Map<String, String> = when (format) {
    ExportFormat.EXCEL, ExportFormat.CSV -> mapOf(
        "Student ID"   to id,
        "Student Name" to name,
        "CA Score"     to "%.2f".format(caScore),
        "Exam Score"   to "%.2f".format(examScore),
        "Final Score"  to "%.2f".format(finalScore),
        "Grade"        to grade,
        "Status"       to status
    )
    ExportFormat.JSON -> mapOf(
        "id" to id, "name" to name,
        "ca_score" to caScore.toString(), "exam_score" to examScore.toString(),
        "final_score" to finalScore.toString(), "grade" to grade, "status" to status
    )
    ExportFormat.PDF -> mapOf(
        "ID" to id, "Name" to name, "CA" to "%.1f".format(caScore),
        "Exam" to "%.1f".format(examScore), "Final" to "%.1f".format(finalScore),
        "Grade" to grade, "Status" to status
    )
}

/** Higher-order function: filter + transform a list of students. */
fun List<StudentRecord>.processWithRule(
    rule: (StudentRecord) -> Boolean,
    transformer: (StudentRecord) -> StudentRecord
): List<StudentRecord> = filter(rule).map(transformer)

/** Compute class statistics for a list of processed students. */
fun List<StudentRecord>.computeStatistics(): ClassStatistics {
    if (isEmpty()) return ClassStatistics(0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0.0, emptyMap())
    val scores = map { it.finalScore }
    val avg = scores.average()
    val sorted = scores.sorted()
    val median = if (sorted.size % 2 == 0)
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
    else sorted[sorted.size / 2]
    val variance = scores.map { (it - avg) * (it - avg) }.average()
    return ClassStatistics(
        average = avg,
        median = median,
        highest = scores.max(),
        lowest = scores.min(),
        passCount = count { it.finalScore >= 40.0 },
        failCount = count { it.finalScore < 40.0 },
        totalStudents = size,
        standardDeviation = Math.sqrt(variance),
        gradeDistribution = groupBy { it.grade }.mapValues { it.value.size }
    )
}
