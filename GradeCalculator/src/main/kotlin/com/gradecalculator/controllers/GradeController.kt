package com.gradecalculator.controllers

import com.gradecalculator.models.*
import com.gradecalculator.utils.FormattingUtils

// ─────────────────────────────────────────────────────────────────────────────
// Grade Calculation Engine
// ─────────────────────────────────────────────────────────────────────────────

class GradeController(private var gradingScale: GradingScale = GradingScale.default()) {

    fun updateGradingScale(scale: GradingScale) { gradingScale = scale }

    /** Calculate and assign the final grade for a single student */
    fun calculateFinalGrade(student: StudentRecord): StudentRecord {
        val capped = student.copy(
            caScore   = minOf(student.caScore, 30.0).coerceAtLeast(0.0),
            examScore = minOf(student.examScore, 70.0).coerceAtLeast(0.0)
        )
        val finalScore = capped.caScore + capped.examScore
        val grade      = gradingScale.findGrade(finalScore)
        return capped.copy(finalScore = finalScore, grade = grade)
    }

    /** Process an entire class list */
    fun processStudents(students: List<StudentRecord>): List<StudentRecord> =
        students.map { calculateFinalGrade(it) }

    /** Compute class-level statistics */
    fun calculateClassStatistics(students: List<StudentRecord>): ClassStatistics =
        FormattingUtils.computeStatistics(students)

    /** Apply a grade curve and recalculate grades */
    fun applyGradeCurve(students: List<StudentRecord>, curvePoints: Double): List<StudentRecord> {
        val curved = FormattingUtils.applyCurve(students, curvePoints)
        return curved.map { s ->
            s.copy(grade = gradingScale.findGrade(s.finalScore))
        }
    }

    /** Rank students by final score */
    fun rankStudents(students: List<StudentRecord>): List<Pair<Int, StudentRecord>> =
        students.sortedByDescending { it.finalScore }
                .mapIndexed { idx, s -> (idx + 1) to s }
}
