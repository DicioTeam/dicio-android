package org.stypox.dicio.sentencesCompilerPlugin.data

import org.gradle.api.logging.Logger
import org.stypox.dicio.sentencesCompilerPlugin.util.SKILL_DEFINITIONS_FILE
import org.stypox.dicio.sentencesCompilerPlugin.util.SentencesCompilerPluginException
import org.stypox.dicio.sentencesCompilerPlugin.util.YML_EXT
import java.io.File

fun extractDataFromFiles(logger: Logger, inputDirFile: File): ExtractedData {
    val skills = parseYamlFile<SkillDefinitionsFile>(File(inputDirFile, SKILL_DEFINITIONS_FILE))
        .skills
        .map { Pair(it, HashMap<String, ArrayList<RawSentence>>()) }
    val languages = ArrayList<String>()

    // sorted() to ensure this executes deterministically, so the APK builds reproduciblyg
    for (lang in inputDirFile.listFiles { file -> file.isDirectory }!!.sorted()) {
        var langHasSentence = false
        for ((skill, sentences) in skills) {
            val file = File(lang, skill.id + YML_EXT)
            if (!file.exists()) {
                continue
            }

            val parsedSentences: Map<String, Any?> = parseYamlFile(file)
            val expectedSentenceIds = skill.sentences.map { it.id }.toSet()
            if (!parsedSentences.keys.containsAll(expectedSentenceIds)) {
                throw SentencesCompilerPluginException(
                    "Skill sentences file ${lang.name}/${
                        file.name
                    } is missing these sentence ids ${
                        expectedSentenceIds - parsedSentences.keys
                    }: ${file.absolutePath}"
                )
            } else if (!expectedSentenceIds.containsAll(parsedSentences.keys)) {
                throw SentencesCompilerPluginException(
                    "Skill sentences file ${lang.name}/${
                        file.name
                    } has these superfluous sentence ids ${
                        parsedSentences.keys - expectedSentenceIds
                    }: ${file.absolutePath}"
                )
            }

            val emptySentenceIds = ArrayList<String>()
            for ((sentenceId, parsedSentencesWithoutId) in parsedSentences) {
                if ((parsedSentencesWithoutId as? String)?.isBlank() == true
                    || (parsedSentencesWithoutId as? List<*>)?.isEmpty() == true
                ) {
                    emptySentenceIds.add(sentenceId)
                    continue // a warning is raised below
                } else if ((parsedSentencesWithoutId as? List<*>)?.all { it is String } != true) {
                    throw SentencesCompilerPluginException(
                        "Skill sentences file ${lang.name}/${
                            file.name
                        } contains an invalid value for sentence id ${
                            sentenceId
                        }: ${file.absolutePath}"
                    )
                }

                // only mark as true if there actually is a sentence for this language
                langHasSentence = true

                for (sentence in parsedSentencesWithoutId) {
                    sentences
                        .getOrPut(lang.name) { ArrayList() }
                        .add(
                            RawSentence(
                                id = sentenceId,
                                file = file,
                                rawConstructs = sentence as String,
                            )
                        )
                }
            }

            if (emptySentenceIds.isNotEmpty()) {
                logger.warn(
                    "[Warning] Skill sentences file ${lang.name}/${
                        file.name
                    } has no sentence definitions for these sentence ids ${
                        emptySentenceIds
                    }: ${file.absolutePath}"
                )
            }
        }

        if (langHasSentence) {
            languages.add(lang.name)
        }

        // issue a warning for unknown files
        for (file in lang.listFiles()!!) {
            if (skills.all { (skill, _) -> skill.id + YML_EXT != file.name }) {
                logger.error(
                    "[Warning] Skill sentences file ${lang.name}/${
                        file.name
                    } does not correspond to any skill: ${file.absolutePath}"
                )
            }
        }
    }

    return ExtractedData(
        skills = skills
            .map { (skill, languageToSentences) ->
                ExtractedSkill(
                    id = skill.id,
                    specificity = skill.specificity,
                    sentenceDefinitions = skill.sentences,
                    // sort here to ensure that the code is generated deterministically
                    languageToSentences = languageToSentences.toList().sortedBy { it.first },
                )
            },
        languages = languages,
    )
}
