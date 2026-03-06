package com.gradecalculator.models

import kotlinx.serialization.Serializable

@Serializable
data class GradingScale(
    val id: String,
    val name: String,
    val ranges: List<GradeRange>,
    val isDefault: Boolean = false
) {
    companion object {
        /** Built-in default grading scale */
        fun default(): GradingScale = GradingScale(
            id = "default",
            name = "Standard Scale",
            isDefault = true,
            ranges = listOf(
                GradeRange(70.0, 100.0, "A", "#4CAF50"),
                GradeRange(60.0, 69.9, "B", "#8BC34A"),
                GradeRange(50.0, 59.9, "C", "#FFC107"),
                GradeRange(40.0, 49.9, "D", "#FF9800"),
                GradeRange(0.0,  39.9, "F", "#F44336")
            )
        )

        fun weighted(): GradingScale = GradingScale(
            id = "weighted",
            name = "Weighted Scale",
            isDefault = false,
            ranges = listOf(
                GradeRange(75.0, 100.0, "A", "#4CAF50"),
                GradeRange(65.0, 74.9,  "B", "#8BC34A"),
                GradeRange(55.0, 64.9,  "C", "#FFC107"),
                GradeRange(45.0, 54.9,  "D", "#FF9800"),
                GradeRange(0.0,  44.9,  "F", "#F44336")
            )
        )
    }

    fun findGrade(score: Double): String =
        ranges.find { score >= it.minScore && score <= it.maxScore }?.grade ?: "F"

    fun isValid(): Boolean {
        if (ranges.isEmpty()) return false
        // Ranges should cover 0–100 without gaps or overlaps (basic check)
        val sorted = ranges.sortedBy { it.minScore }
        return sorted.first().minScore <= 0.0 && sorted.last().maxScore >= 100.0
    }
}

@Serializable
data class GradeRange(
    val minScore: Double,
    val maxScore: Double,
    val grade: String,
    val colorCode: String = "#000000"
)
