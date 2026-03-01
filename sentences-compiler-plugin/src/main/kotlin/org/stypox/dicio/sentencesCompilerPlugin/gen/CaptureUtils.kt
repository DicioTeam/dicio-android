package org.stypox.dicio.sentencesCompilerPlugin.gen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import org.stypox.dicio.sentencesCompilerPlugin.data.CaptureDefinition

fun getTypeName(captureDefinition: CaptureDefinition): TypeName {
    return when (captureDefinition) {
        is CaptureDefinition.StringCapture -> String::class.asTypeName()
        is CaptureDefinition.NumberCapture -> ClassName("org.dicio.numbers.unit", "Number")
        is CaptureDefinition.DurationCapture -> ClassName("org.dicio.numbers.unit", "Duration")
        is CaptureDefinition.DateTimeCapture -> ClassName("java.time", "LocalDateTime")
    }
}

fun getCapturingGroupClassName(captureDefinition: CaptureDefinition): TypeName {
    return when (captureDefinition) {
        is CaptureDefinition.StringCapture -> ClassName("org.dicio.skill.standard.construct", "CapturingConstruct")
        is CaptureDefinition.NumberCapture -> ClassName("org.dicio.skill.standard.construct", "NumberConstruct")
        is CaptureDefinition.DurationCapture -> ClassName("org.dicio.skill.standard.construct", "DurationConstruct")
        is CaptureDefinition.DateTimeCapture -> ClassName("org.dicio.skill.standard.construct", "DateTimeConstruct")
    }
}

fun getParams(captureDefinition: CaptureDefinition): List<Triple<String, String, Any>> {
    val res = ArrayList<Triple<String, String, Any>>()
    captureDefinition.weight?.let { res.add(Triple("weight", "%Lf", it)) }
    when (captureDefinition) {
        is CaptureDefinition.StringCapture -> {}
        is CaptureDefinition.NumberCapture -> {
            captureDefinition.bonusIfLargestPossible?.let { res.add(Triple("bonusIfLargestPossible", "%Lf", it)) }
            captureDefinition.shortScale?.let { res.add(Triple("shortScale", "%L", it)) }
            captureDefinition.preferOrdinal?.let { res.add(Triple("preferOrdinal", "%L", it)) }
            captureDefinition.integerOnly?.let { res.add(Triple("integerOnly", "%L", it)) }
        }
        is CaptureDefinition.DurationCapture -> {
            captureDefinition.bonusIfLargestPossible?.let { res.add(Triple("bonusIfLargestPossible", "%Lf", it)) }
            captureDefinition.shortScale?.let { res.add(Triple("shortScale", "%L", it)) }
        }
        is CaptureDefinition.DateTimeCapture -> {
            captureDefinition.bonusIfLargestPossible?.let { res.add(Triple("bonusIfLargestPossible", "%Lf", it)) }
            captureDefinition.shortScale?.let { res.add(Triple("shortScale", "%L", it)) }
            captureDefinition.preferMonthBeforeDay?.let { res.add(Triple("preferMonthBeforeDay", "%L", it)) }
        }
    }
    return res
}
