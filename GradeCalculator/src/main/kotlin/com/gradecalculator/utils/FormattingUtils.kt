package com.gradecalculator.utils

import com.gradecalculator.models.ClassStatistics
import com.gradecalculator.models.StudentRecord
import kotlin.math.sqrt

object FormattingUtils {

    fun Double.toScoreString(): String = "%.2f".format(this)
    fun Double.toPercentString(): String = "%.1f%%".format(this)

    /** Compute full class statistics from a list of students */
    fun computeStatistics(students: List<StudentRecord>): ClassStatistics {
        if (students.isEmpty()) return ClassStatistics(
            average = 0.0, median = 0.0, highest = 0.0, lowest = 0.0,
            passCount = 0, failCount = 0, totalStudents = 0,
            standardDeviation = 0.0, gradeDistribution = emptyMap(), passRate = 0.0
        )

        val scores = students.map { it.finalScore }
        val sorted = scores.sorted()
        val avg = scores.average()
        val median = if (sorted.size % 2 == 0)
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        else
            sorted[sorted.size / 2].toDouble()

        val variance = scores.map { (it - avg) * (it - avg) }.average()
        val stdDev = sqrt(variance)

        val passCount = students.count { it.finalScore >= 40.0 }

        return ClassStatistics(
            average            = avg,
            median             = median,
            highest            = sorted.last(),
            lowest             = sorted.first(),
            passCount          = passCount,
            failCount          = students.size - passCount,
            totalStudents      = students.size,
            standardDeviation  = stdDev,
            gradeDistribution  = students.groupBy { it.grade }.mapValues { it.value.size },
            passRate           = passCount.toDouble() / students.size * 100.0
        )
    }

    /** Format a file size in bytes to a readable string */
    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }

    /** Generate a unique ID based on timestamp + random suffix */
    fun generateId(): String {
        val ts   = System.currentTimeMillis().toString(36)
        val rand = (Math.random() * 0xFFFFF).toLong().toString(36)
        return "$ts-$rand"
    }

    /** Apply an optional grade curve to all students */
    fun applyCurve(students: List<StudentRecord>, curvePoints: Double): List<StudentRecord> =
        students.map { s ->
            val curved = minOf(100.0, s.finalScore + curvePoints)
            s.copy(finalScore = curved)
        }
}
