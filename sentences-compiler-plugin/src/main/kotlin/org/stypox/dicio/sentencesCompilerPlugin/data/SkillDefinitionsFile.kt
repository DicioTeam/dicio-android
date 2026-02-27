package org.stypox.dicio.sentencesCompilerPlugin.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue

data class SkillDefinitionsFile(
    val skills: List<SkillDefinition>
)

data class SkillDefinition(
    val id: String,
    val specificity: Specificity,
    val sentences: List<SentenceDefinition>
)

enum class Specificity(@JsonValue val serializedValue: String) {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low"),
}

data class SentenceDefinition(
    val id: String,
    val captures: List<CaptureDefinition> = listOf(),
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CaptureDefinition.StringCapture::class, name = "string"),
    JsonSubTypes.Type(value = CaptureDefinition.NumberCapture::class, name = "number"),
    JsonSubTypes.Type(value = CaptureDefinition.DurationCapture::class, name = "duration"),
    JsonSubTypes.Type(value = CaptureDefinition.DateTimeCapture::class, name = "date_time"),
)
sealed interface CaptureDefinition {
    val id: String
    val weight: Float?

    data class StringCapture(
        override val id: String,
        override val weight: Float?,
    ) : CaptureDefinition

    data class NumberCapture(
        override val id: String,
        override val weight: Float?,
        @param:JsonProperty("bonus_if_largest_possible")
        val bonusIfLargestPossible: Float?,
        @param:JsonProperty("short_scale")
        val shortScale: Boolean?,
        @param:JsonProperty("prefer_ordinal")
        val preferOrdinal: Boolean?,
        @param:JsonProperty("integer_only")
        val integerOnly: Boolean?,
    ) : CaptureDefinition

    data class DurationCapture(
        override val id: String,
        override val weight: Float?,
        @param:JsonProperty("bonus_if_largest_possible")
        val bonusIfLargestPossible: Float?,
        @param:JsonProperty("short_scale")
        val shortScale: Boolean?,
    ) : CaptureDefinition

    data class DateTimeCapture(
        override val id: String,
        override val weight: Float?,
        @param:JsonProperty("bonus_if_largest_possible")
        val bonusIfLargestPossible: Float?,
        @param:JsonProperty("short_scale")
        val shortScale: Boolean?,
        @param:JsonProperty("prefer_month_before_day")
        val preferMonthBeforeDay: Boolean?,
    ) : CaptureDefinition
}
