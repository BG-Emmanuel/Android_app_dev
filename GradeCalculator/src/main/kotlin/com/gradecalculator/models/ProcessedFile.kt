package com.gradecalculator.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class ProcessedFile(
    val id: String,
    val fileName: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val importDate: LocalDateTime,
    val students: List<StudentRecord>,
    val fileType: FileType,
    val gradingScaleUsed: String,
    val statistics: ClassStatistics? = null
) {
    fun formattedDate(): String =
        importDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm"))

    fun summary(): String =
        "${students.size} students  |  Avg: ${"%.1f".format(statistics?.average ?: 0.0)}  |  Pass: ${statistics?.passCount ?: 0}"
}

// Custom serializer for LocalDateTime
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) =
        encoder.encodeString(value.format(formatter))
    override fun deserialize(decoder: Decoder): LocalDateTime =
        LocalDateTime.parse(decoder.decodeString(), formatter)
}
