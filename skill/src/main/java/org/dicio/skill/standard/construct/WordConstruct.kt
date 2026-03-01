package org.dicio.skill.standard.construct

import org.dicio.skill.standard.StandardScore
import org.dicio.skill.standard.util.MatchHelper
import org.dicio.skill.standard.util.normalizeMemToEnd


data class WordConstruct(
    private val text: String,
    private val isRegex: Boolean,
    private val isDiacriticsSensitive: Boolean,
    private val weight: Float = 1.0f,
) : Construct {
    private val compiledRegex = if (isRegex) Regex(text) else null

    override fun matchToEnd(memToEnd: Array<StandardScore>, helper: MatchHelper) {
        val cumulativeWeight = helper.cumulativeWeight

        for (start in memToEnd.indices) {
            val wordIndex = helper.splitWordsIndices[start]
            if (wordIndex >= 0) {
                val word = helper.splitWords[wordIndex]
                val wordText = if (isDiacriticsSensitive)
                    word.originalText
                else
                    word.nfkdNormalizedText

                if ((compiledRegex?.matches(wordText)) ?: (text == wordText)) {
                    val userWeight = cumulativeWeight[word.end] - cumulativeWeight[start]
                    memToEnd[start] = StandardScore.keepBest(
                        memToEnd[word.end].plus(
                            userMatched = userWeight,
                            userWeight = userWeight,
                            refMatched = weight,
                            refWeight = weight,
                        ),
                        memToEnd[start].plus(refWeight = weight),
                    )
                    continue
                }
            }

            memToEnd[start] = memToEnd[start].plus(refWeight = weight)
        }

        normalizeMemToEnd(memToEnd, cumulativeWeight)
    }

    override fun toString(): String {
        return text
    }
}
