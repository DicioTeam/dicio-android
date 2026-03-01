package org.stypox.dicio.sentencesCompilerPlugin.gen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.stypox.dicio.sentencesCompilerPlugin.data.ParsedData
import org.stypox.dicio.sentencesCompilerPlugin.data.ParsedSkill
import org.stypox.dicio.sentencesCompilerPlugin.util.CLASS_NAME
import org.stypox.dicio.sentencesCompilerPlugin.util.FILE_COMMENT
import org.stypox.dicio.sentencesCompilerPlugin.util.PACKAGE_NAME
import java.io.File
import org.stypox.dicio.sentencesCompilerPlugin.data.correspondingSentenceDefinition

fun generateSkillSentencesKt(parsedData: ParsedData, outputDirFile: File) {
    val baseObj = TypeSpec.objectBuilder(CLASS_NAME)
        .addAnnotation(
            AnnotationSpec.builder(Suppress::class)
                .addMember("%S", "UNUSED_PARAMETER")
                .build()
        )

    // add languages property
    baseObj.addProperty(
        PropertySpec
            .builder(
                "languages",
                List::class.asClassName().parameterizedBy(String::class.asClassName())
            )
            .initializer(
                 "listOf(${"%S,".repeat(parsedData.languages.size)})",
                 args = parsedData.languages.toTypedArray()
             )
             .build()
    )

    // add all skill objects
    for (skill in parsedData.skills) {
        baseObj.addType(generateSkillObject(skill))
    }

    FileSpec.builder(PACKAGE_NAME, CLASS_NAME)
        .addFileComment(FILE_COMMENT)
        .addType(baseObj.build())
        .build()
        .writeTo(outputDirFile)
}

private fun generateSkillObject(skill: ParsedSkill): TypeSpec {
    val skillInterfaceName = skill.id.toPascalCase()
    val skillInterfaceClassName = ClassName("", skillInterfaceName)
    return TypeSpec.interfaceBuilder(skillInterfaceName)
        .addModifiers(KModifier.SEALED)
        .addTypes(generateResultTypes(skill, skillInterfaceClassName))
        .addType(
            TypeSpec.companionObjectBuilder()
                .addFunction(generateResultFromMatchFunction(skill, skillInterfaceClassName))
                .addProperty(generateLanguageToDataProperty(skill, skillInterfaceClassName))
                .addFunction(generateGetOperator(skillInterfaceClassName))
                .build()
        )
        .build()
}

private fun generateResultTypes(skill: ParsedSkill, superinterface: ClassName): List<TypeSpec> {
    val resultTypes = ArrayList<TypeSpec>()

    for (definition in skill.sentenceDefinitions) {
        val definitionResultCls = if (definition.captures.isEmpty()) {
            TypeSpec.objectBuilder(definition.id.toPascalCase())
        } else {
            TypeSpec.classBuilder(definition.id.toPascalCase())
                .addModifiers(KModifier.DATA)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameters(definition.captures.map {
                            ParameterSpec.builder(it.id.toCamelCase(), getTypeName(it).copy(nullable = true)).build()
                        })
                        .build()
                )
                .addProperties(definition.captures.map {
                    PropertySpec
                        .builder(it.id.toCamelCase(), getTypeName(it).copy(nullable = true))
                        .initializer(it.id.toCamelCase())
                        .build()
                })
        }

        definitionResultCls.addSuperinterface(superinterface)
        resultTypes.add(definitionResultCls.build())
    }

    return resultTypes
}

private fun generateResultFromMatchFunction(skill: ParsedSkill, returnType: ClassName): FunSpec {
    val fromStandardResultFun = FunSpec.builder("fromStandardScore")
        .addParameter("input", String::class)
        .addParameter("sentenceId", String::class)
        .addParameter("score", ClassName("org.dicio.skill.standard", "StandardScore"))
        .returns(returnType)
        .beginControlFlow("return when(sentenceId)")

    for (definition in skill.sentenceDefinitions) {
        val definitionClassName = definition.id.toPascalCase()
        if (definition.captures.isEmpty()) {
            fromStandardResultFun.addStatement(
                "%S -> $definitionClassName",
                definition.id
            )
        } else {
            fromStandardResultFun.addStatement(
                "%S -> $definitionClassName(${
                    "score.getCapturingGroup<%T>(input, %S),"
                        .repeat(definition.captures.size)
                })",
                definition.id,
                *definition.captures.flatMap { sequenceOf(getTypeName(it), it.id) }.toTypedArray()
            )
        }
    }

    fromStandardResultFun.addStatement($$"""else -> throw IllegalArgumentException("Unknown sentence id $sentenceId")""")
    fromStandardResultFun.endControlFlow()
    return fromStandardResultFun.build()
}

private fun generateLanguageToDataProperty(skill: ParsedSkill, resultType: ClassName): PropertySpec {
    val standardRecognizerDataClassName =
        ClassName("org.dicio.skill.standard", "StandardRecognizerData")
            .parameterizedBy(resultType)
    val dataProp = PropertySpec
        .builder(
            "languageToData",
            Map::class.asTypeName().parameterizedBy(
                String::class.asTypeName(),
                Lazy::class.asTypeName().parameterizedBy(
                    standardRecognizerDataClassName
                )
            )
        )
        .addModifiers(KModifier.PRIVATE)
        .initializer(
            "mapOf(${"%S to lazy { %L },".repeat(skill.languageToSentences.size)})",
            *skill.languageToSentences.flatMap { (language, sentences) ->
                sequenceOf(
                    language,
                    CodeBlock.of(
                        "%T(%T.%L, %L, listOf(${"Pair(%S, %L),".repeat(sentences.size)}))",
                        standardRecognizerDataClassName,
                        ClassName("org.dicio.skill.skill", "Specificity"),
                        skill.specificity.name,
                        "::fromStandardScore",
                        *sentences.flatMap { sentence ->
                            val definition = correspondingSentenceDefinition(skill, sentence)
                            sequenceOf(
                                sentence.id,
                                generateConstruct(sentence.constructs, definition),
                            )
                        }.toTypedArray()
                    )
                )
            }.toTypedArray()
        )

    return dataProp.build()
}

private fun generateGetOperator(resultType: ClassName): FunSpec {
    return FunSpec.builder("get")
        .addModifiers(KModifier.OPERATOR)
        .addParameter("language", String::class)
        .returns(
            ClassName("org.dicio.skill.standard", "StandardRecognizerData")
                .parameterizedBy(resultType)
                .copy(nullable = true)
        )
        .addCode("return languageToData[language]?.value")
        .build()
}

/**
 * e.g. "current_time" would turn into "CurrentTime"
 */
private fun String.toPascalCase(): String {
    return split("_", "-").joinToString(separator = "") { it.uppercaseFirstChar() }
}

/**
 * e.g. "time_or_duration" would turn into "timeOrDuration"
 */
private fun String.toCamelCase(): String {
    return toPascalCase().replaceFirstChar { it.lowercaseChar() }
}
