package org.dicio.skill.standard.util

import org.dicio.numbers.ParserFormatter

data class MatchHelper(
    val parserFormatter: ParserFormatter?,
    val userInput: String,
) {
    val splitWords = splitWords(userInput)
    val splitWordsIndices = splitWordsIndices(userInput, splitWords)
    val cumulativeWeight = cumulativeWeight(userInput, splitWords)
    val cumulativeWhitespace = cumulativeWhitespace(userInput)
    val tokenizations: MutableMap<String, Any?> = HashMap()

    // TODO add tests
    inline fun <reified T> getOrTokenize(key: String, tokenizer: (MatchHelper) -> T): T {
        val result = tokenizations.getOrDefault(key, Unit)
        if (result !is Unit) {
            return result as T
        }

        val tokenization = tokenizer(this)
        tokenizations[key] = tokenization
        return tokenization
    }
}
