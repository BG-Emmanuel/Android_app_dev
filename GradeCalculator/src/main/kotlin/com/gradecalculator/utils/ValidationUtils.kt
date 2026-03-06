package com.gradecalculator.utils

import com.gradecalculator.models.StudentRecord
import com.gradecalculator.models.ValidationResult

object ValidationUtils {

    /** Validate a list of students and return all errors grouped by student */
    fun validateAll(students: List<StudentRecord>): Map<String, ValidationResult> =
        students.associate { it.id to it.validateScores() }

    /** Return only invalid students */
    fun invalidStudents(students: List<StudentRecord>): List<Pair<StudentRecord, ValidationResult>> =
        students.mapNotNull { s ->
            val r = s.validateScores()
            if (!r.isValid) s to r else null
        }

    /** Detect duplicate student IDs */
    fun detectDuplicateIds(students: List<StudentRecord>): List<String> {
        val seen = mutableSetOf<String>()
        return students.filter { !seen.add(it.id) }.map { it.id }
    }

    /** Find students with missing scores (score = 0 might be intentional, flag negatives/blanks) */
    fun detectMissingScores(students: List<StudentRecord>): List<StudentRecord> =
        students.filter { it.caScore == 0.0 && it.examScore == 0.0 }

    /** Detect statistical outliers using IQR method */
    fun detectOutliers(students: List<StudentRecord>): List<StudentRecord> {
        if (students.size < 4) return emptyList()
        val scores = students.map { it.finalScore }.sorted()
        val q1 = scores[scores.size / 4]
        val q3 = scores[3 * scores.size / 4]
        val iqr = q3 - q1
        val lower = q1 - 1.5 * iqr
        val upper = q3 + 1.5 * iqr
        return students.filter { it.finalScore < lower || it.finalScore > upper }
    }

    /** Validate a Google Sheets URL */
    fun isValidGoogleSheetsUrl(url: String): Boolean =
        url.contains("docs.google.com/spreadsheets")

    /** Validate file extension */
    fun isSupportedFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.').lowercase()
        return ext in listOf("xlsx", "xls", "csv")
    }
}
