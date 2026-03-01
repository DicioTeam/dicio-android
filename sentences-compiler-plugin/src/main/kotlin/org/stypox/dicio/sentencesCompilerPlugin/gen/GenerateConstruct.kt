package org.stypox.dicio.sentencesCompilerPlugin.gen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import org.dicio.sentences_compiler.construct.CapturingGroup
import org.dicio.sentences_compiler.construct.Construct
import org.dicio.sentences_compiler.construct.OptionalConstruct
import org.dicio.sentences_compiler.construct.OrList
import org.dicio.sentences_compiler.construct.SentenceConstructList
import org.dicio.sentences_compiler.construct.Word
import org.dicio.sentences_compiler.construct.WordWithVariations
import org.stypox.dicio.sentencesCompilerPlugin.data.SentenceDefinition
import org.stypox.dicio.sentencesCompilerPlugin.util.SentencesCompilerPluginException

fun generateConstruct(construct: Construct, sentence: SentenceDefinition): CodeBlock {
    return when (construct) {
        is Word -> generateWord(construct)
        is WordWithVariations -> generateWordWithVariations(construct)
        is OrList -> generateOrList(construct, sentence)
        is OptionalConstruct -> generateOptionalConstruct()
        is CapturingGroup -> generateCapturingGroup(construct, sentence)
        is SentenceConstructList -> generateSentenceConstructList(construct, sentence)
        else -> throw SentencesCompilerPluginException(
            "Unexpected construct obtained from sentences compiler: type=${
                construct::class.simpleName
            }, value=\"$construct\""
        )
    }
}

fun generateWord(word: Word): CodeBlock {
    return CodeBlock.of(
        "%T(%S, %L, %L)",
        ClassName("org.dicio.skill.standard.construct", "WordConstruct"),
        word.normalizedValue,
        /* isRegex = */ false,
        word.isDiacriticsSensitive,
        // TODO allow specifying weight
    )
}

fun generateWordWithVariations(word: WordWithVariations): CodeBlock {
    return CodeBlock.of(
        "%T(%S, %L, %L)",
        ClassName("org.dicio.skill.standard.construct", "WordConstruct"),
        word.toJavaRegex(),
        /* isRegex = */ true,
        word.isDiacriticsSensitive,
        // TODO allow specifying weight
    )
}

fun generateOrList(orList: OrList, sentence: SentenceDefinition): CodeBlock {
    return CodeBlock.of(
        "%T(listOf(${"%L,".repeat(orList.constructs.size)}))",
        ClassName("org.dicio.skill.standard.construct", "OrConstruct"),
        *orList.constructs.map { generateConstruct(it, sentence) }.toTypedArray(),
    )
}

fun generateOptionalConstruct(): CodeBlock {
    return CodeBlock.of(
        "%T()",
        ClassName("org.dicio.skill.standard.construct", "OptionalConstruct")
    )
}

fun generateCapturingGroup(capturingGroup: CapturingGroup, sentence: SentenceDefinition): CodeBlock {
    val captureDef = sentence.captures.firstOrNull { it.id == capturingGroup.name }
        ?: throw SentencesCompilerPluginException("BUG in sentences compiler plugin: capturing " +
            "group named ${capturingGroup.name} not found among captures for sentence ${sentence.id}")
    val params = getParams(captureDef)
    return CodeBlock.of(
        "%T(%S" + params.joinToString { (name, spec, _) -> ",$name=$spec" } + ")",
        getCapturingGroupClassName(captureDef),
        capturingGroup.name,
        *params.map { (_, _, value) -> value }.toTypedArray()
    )
}

fun generateSentenceConstructList(
    sentenceConstructList: SentenceConstructList,
    sentence: SentenceDefinition
): CodeBlock {
    return CodeBlock.of(
        "%T(listOf(${"%L,".repeat(sentenceConstructList.constructs.size)}))",
        ClassName("org.dicio.skill.standard.construct", "CompositeConstruct"),
        *sentenceConstructList.constructs.map { generateConstruct(it, sentence) }.toTypedArray(),
    )
}
