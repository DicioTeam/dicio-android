package org.stypox.dicio.sentencesCompilerPlugin.data

import org.stypox.dicio.sentencesCompilerPlugin.util.SentencesCompilerPluginException

fun correspondingSentenceDefinition(
    parsedSkill: ParsedSkill,
    sentence: ParsedSentence
): SentenceDefinition {
    return parsedSkill.sentenceDefinitions
        .firstOrNull { definition -> definition.id == sentence.id }
        ?: throw SentencesCompilerPluginException(
            "BUG in sentences compiler plugin: could not find definition corresponding to " +
                    "sentence id ${sentence.id} for skill ${parsedSkill.id}"
        )
}

fun checkSentences(parsedSkill: ParsedSkill) {
    for (sentence in parsedSkill.languageToSentences.map { it.second }.flatten()) {
        val definition = correspondingSentenceDefinition(parsedSkill, sentence)
        val expectedCapturingGroups = definition.captures.map { it.id }.toSet()
        val actualCapturingGroups = sentence.constructs.capturingGroupNames
        if (!expectedCapturingGroups.containsAll(actualCapturingGroups)) {
            throw SentencesCompilerPluginException(
                "Unknown capturing groups ${actualCapturingGroups - expectedCapturingGroups
                } (valid ones are: ${expectedCapturingGroups
                }) found in sentence '${sentence.rawConstructs}' under id '${sentence.id
                }' in skill sentences file ${sentence.file.parentFile.name}/${sentence.file.name
                }: ${sentence.file.absolutePath}"
            )
        }
    }
}
