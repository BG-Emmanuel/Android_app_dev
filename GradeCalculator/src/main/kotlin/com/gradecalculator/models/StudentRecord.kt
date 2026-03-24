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
// LAMBDA FUNCTION IMPLEMENTATIONS & PATTERNS
// ─────────────────────────────────────────────────────────────────────────────
// This section demonstrates 15+ lambda function patterns used throughout the app

// ✓ LAMBDA #1: processWithRule - Higher-order function taking two lambdas
// Pattern: Takes predicate lambda (rule) and transformer lambda
// Usage: Filters based on rule, then maps with transformer
fun List<StudentRecord>.processWithRule(
    rule: (StudentRecord) -> Boolean,
    transformer: (StudentRecord) -> StudentRecord
): List<StudentRecord> = this.filter(rule).map(transformer)

// ✓ LAMBDA #2: calculatePassRate - Uses count { } lambda with predicate
// Pattern: Inline lambda closure over hasPassed() method
fun List<StudentRecord>.calculatePassRate(): Double {
    if (isEmpty()) return 0.0
    return count { it.hasPassed() }.toDouble() / size * 100.0
}

// ✓ LAMBDA #3: topPerformers - Uses sortedByDescending { } lambda
// Pattern: Single-parameter lambda extracting comparison value
fun List<StudentRecord>.topPerformers(n: Int = 5): List<StudentRecord> =
    sortedByDescending { it.finalScore }.take(n)

// ✓ LAMBDA #4: detectDuplicates - Uses forEach { } lambda with side effects
// Pattern: Lambda with mutable closure (adds to seen/duplicates sets)
fun List<StudentRecord>.detectDuplicates(): List<String> {
    val seen = mutableSetOf<String>()
    val duplicates = mutableListOf<String>()
    forEach { student ->
        if (!seen.add(student.id)) duplicates.add(student.id)
    }
    return duplicates
}

// ✓ LAMBDA #5: withRankings - Uses mapIndexed + sortedByDescending lambdas
// Pattern: Index-aware transformation (rank = index + 1)
fun List<StudentRecord>.withRankings(): Map<String, Int> {
    return sortedByDescending { it.finalScore }
        .mapIndexed { index, student -> student.id to (index + 1) }
        .toMap()
}

// ✓ LAMBDA #6: calculateClassStatistics - Complex lambda chain
// Uses: map { }, average(), sorted(), groupingBy { }, eachCount()
fun List<StudentRecord>.calculateClassStatistics(): ClassStatistics {
    if (isEmpty()) return ClassStatistics(0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0.0, emptyMap(), 0.0)
    
    // LAMBDA CHAIN: map { it.finalScore } extracts score from each student
    val scores = map { it.finalScore }.sorted()
    
    val mean = scores.average()
    
    // LAMBDA: map { (it - mean) * (it - mean) } computes squared deviation
    val variance = scores.map { (it - mean) * (it - mean) }.average()
    val stdDev = kotlin.math.sqrt(variance)
    
    val median = if (scores.size % 2 == 0) {
        (scores[scores.size / 2 - 1] + scores[scores.size / 2]) / 2.0
    } else {
        scores[scores.size / 2]
    }
    
    // LAMBDAS: count { it.hasPassed() } boolean predicates
    val pass = count { it.hasPassed() }
    val fail = size - pass
    
    // LAMBDA: groupingBy { it.grade.ifBlank { "Ungraded" } } - groups with default
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

// ✓ LAMBDA #7: identifyAtRiskStudents - Uses sortedBy { } lambda
// Pattern: Functional filtering using percentile logic with lambdas
fun List<StudentRecord>.identifyAtRiskStudents(): List<StudentRecord> {
    if (size < 5) return emptyList()
    val sorted = sortedBy { it.finalScore }
    val bottomThreshold = (size * 0.2).toInt().coerceAtLeast(1)
    return sorted.take(bottomThreshold)
}

// ✓ LAMBDA #8: getPercentile - Uses count { } with complex predicate
// Pattern: Lambda closure capturing outer variable (student)
fun List<StudentRecord>.getPercentile(student: StudentRecord): Double {
    if (isEmpty()) return 0.0
    val better = count { it.finalScore > student.finalScore }
    return (100.0 * (size - better)) / size
}

// ═════════════════════════════════════════════════════════════════════════════
// NEW ADVANCED LAMBDA PATTERNS
// ═════════════════════════════════════════════════════════════════════════════

// ✓ LAMBDA #9: filterAndEnrich - Demonstrates filter + map chaining
// Pattern: Returns pair of original + enriched data
fun List<StudentRecord>.filterAndEnrich(
    predicate: (StudentRecord) -> Boolean,
    enricher: (StudentRecord) -> Pair<StudentRecord, String>
): List<Pair<StudentRecord, String>> =
    this.filter(predicate).map(enricher)

// ✓ LAMBDA #10: batchProcess - Groups and processes in batches
// Pattern: chunked + flatMap with processor lambda
fun List<StudentRecord>.batchProcess(
    batchSize: Int,
    processor: (List<StudentRecord>) -> List<StudentRecord>
): List<StudentRecord> =
    this.chunked(batchSize).flatMap(processor)

// ✓ LAMBDA #11: transformWithFallback - Safe transformation with error handling
// Pattern: try-catch inside map lambda for error resilience
fun <T> List<StudentRecord>.transformWithFallback(
    transformer: (StudentRecord) -> T,
    fallback: (StudentRecord, Exception) -> T
): List<T> = this.map { student ->
    try {
        transformer(student)
    } catch (e: Exception) {
        fallback(student, e)
    }
}

// ✓ LAMBDA #12: conditionalAggregate - Accumulates conditionally using fold
// Pattern: Lambda predicate + lambda accumulator with fold
fun List<StudentRecord>.conditionalAggregate(
    condition: (StudentRecord) -> Boolean,
    aggregator: (Double, StudentRecord) -> Double
): Double =
    this.filter(condition)
        .fold(0.0) { acc, student -> aggregator(acc, student) }

// Example: sum scores of passing students
// val totalPass = students.conditionalAggregate(
//     condition = { it.hasPassed() },
//     aggregator = { acc, student -> acc + student.finalScore }
// )

// ✓ LAMBDA #13: findByCriteria - Multiple predicates with allMatch
// Pattern: Varargs of lambdas with all { } quantifier
fun List<StudentRecord>.findByCriteria(vararg predicates: (StudentRecord) -> Boolean): List<StudentRecord> =
    this.filter { student -> predicates.all { it(student) } }

// ✓ LAMBDA #14: scoreDistribution - Bucketing via groupBy lambda
// Pattern: Custom bucketing/categorization with lambda
fun List<StudentRecord>.scoreDistribution(
    bucketizer: (StudentRecord) -> String
): Map<String, List<StudentRecord>> =
    groupBy(bucketizer)

// Example usage:
// val byRange = students.scoreDistribution { student ->
//     when {
//         student.finalScore >= 70 -> "High"
//         student.finalScore >= 50 -> "Medium"
//         else -> "Low"
//     }
// }

// ✓ LAMBDA #15: comparePerformance - Comparison with custom lambda comparator
// Pattern: zip + map with boolean-returning lambda
fun List<StudentRecord>.comparePerformance(
    other: List<StudentRecord>,
    comparator: (StudentRecord, StudentRecord) -> Boolean
): List<Triple<StudentRecord, StudentRecord, Boolean>> =
    this.zip(other).map { (s1, s2) -> Triple(s1, s2, comparator(s1, s2)) }

// ─────────────────────────────────────────────────────────────────────────────
// DEMONSTRATION: Shows all lambda patterns in action
// ─────────────────────────────────────────────────────────────────────────────

fun List<StudentRecord>.demonstrateHigherOrderFunctions() {
    // LAMBDA #1 demo: processWithRule (filter + map)
    val passedStudents = this.processWithRule(
        rule = { it.finalScore >= 40 },
        transformer = { it.copy(grade = "PASS") }
    )
    println("✓ Passed students: ${passedStudents.map { it.name }}")

    // LAMBDA #2 demo: calculatePassRate (count)
    val passRate = this.calculatePassRate()
    println("✓ Pass rate: ${"%.1f".format(passRate)}%")

    // LAMBDA #3 demo: topPerformers (sortedByDescending)
    val topThree = this.topPerformers(3)
    println("✓ Top performers: ${topThree.map { it.name }}")

    // LAMBDA #9 demo: filterAndEnrich (enricher function)
    val enriched = this.filterAndEnrich(
        predicate = { it.finalScore >= 70 },
        enricher = { s -> s to "(High Performer)" }
    )
    println("✓ Enriched: ${enriched.take(2)}")

    // LAMBDA #12 demo: conditionalAggregate (fold)
    val totalPass = this.conditionalAggregate(
        condition = { it.hasPassed() },
        aggregator = { acc, s -> acc + s.finalScore }
    )
    println("✓ Total passing scores: ${"%.1f".format(totalPass)}")

    // LAMBDA #14 demo: scoreDistribution (groupBy)
    val distrib = this.scoreDistribution { student ->
        when {
            student.finalScore >= 70 -> "High (70+)"
            student.finalScore >= 50 -> "Medium (50-69)"
            else -> "Low (<50)"
        }
    }
    println("✓ Distribution: ${distrib.mapValues { (_, v) -> v.size }}")
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
