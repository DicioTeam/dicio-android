package org.stypox.dicio.util

import org.dicio.skill.standard.util.nfkdNormalizeWord
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.max

object StringUtils {
    private val PUNCTUATION_PATTERN = Pattern.compile("\\p{Punct}")
    private val WORD_DELIMITERS_PATTERN = Pattern.compile("[^\\p{L}\\d]")
    const val DEFAULT_SEPARATOR = " • "

    /**
     * Joins strings using delimiter
     * @param separator what to put in between strings
     * @param strings a list of strings to join; only the non-blank ones will be considered
     * @return `string1 + separator + string2 + separator + ... + separator + stringN`
     */
    fun joinNonBlank(vararg strings: String?, separator: CharSequence = DEFAULT_SEPARATOR): String {
        return strings.filter { !it.isNullOrBlank() }.joinToString(separator)
    }

    /**
     * Removes the punctuation in a string
     * @param string a string to remove punctuation from
     * @return e.g. for "hello, how are you? " returns "hello how are you "
     */
    fun removePunctuation(string: String): String {
        return RegexUtils.replaceAll(PUNCTUATION_PATTERN, string, "")
    }

    private fun cleanStringForDistance(s: String): String {
        return WORD_DELIMITERS_PATTERN.matcher(
            nfkdNormalizeWord(s.lowercase(Locale.getDefault()))
        ).replaceAll("")
    }

    /**
     * Returns the dynamic programming memory obtained when calculating the Levenshtein distance.
     * The solution lies at `memory[a.length()][b.length()]`. This memory can be used to find
     * the set of actions (insertion, deletion or substitution) to be done on the two strings to
     * turn one into the other. TODO this can be optimized to work with O(n) memory.
     * @param a the first string, maybe cleaned with [cleanStringForDistance]
     * @param b the second string, maybe cleaned with [cleanStringForDistance]
     * @return the memory of size `(a.length()+1) x (b.length()+1)`
     */
    private fun levenshteinDistanceMemory(a: String, b: String): Array<IntArray> {
        // memory already filled with zeros, as it's the default value for int
        val memory = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) {
            memory[i][0] = i
        }
        for (j in 0..b.length) {
            memory[0][j] = j
        }

        for (i in a.indices) {
            for (j in b.indices) {
                val substitutionCost = if (a[i].lowercaseChar() == b[j].lowercaseChar()) 0 else 1
                memory[i + 1][j + 1] = minOf(
                    memory[i][j + 1] + 1,
                    memory[i + 1][j] + 1,
                    memory[i][j] + substitutionCost
                )
            }
        }
        return memory
    }

    private fun pathInLevenshteinMemory(
        a: String, b: String, memory: Array<IntArray>
    ): List<LevenshteinMemoryPos> {
        // follow the path from bottom right (score==distance) to top left (where score==0)
        val positions: MutableList<LevenshteinMemoryPos> = ArrayList()
        var i = a.length - 1
        var j = b.length - 1
        while (i >= 0 && j >= 0) {
            val iOld = i
            val jOld = j
            var match = false
            if (memory[i + 1][j + 1] == memory[i][j + 1] + 1) {
                // the path goes up
                --i
            } else if (memory[i + 1][j + 1] == memory[i + 1][j] + 1) {
                // the path goes left
                --j
            } else {
                // the path goes up-left diagonally (surely either
                // memory[i+1][j+1] == memory[i][j] or memory[i+1][j+1] == memory[i][j] + 1)
                match = memory[i + 1][j + 1] == memory[i][j]
                --i
                --j
            }
            positions.add(LevenshteinMemoryPos(iOld, jOld, match))
        }
        return positions
    }

    /**
     * Finds the
     * [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance)
     * between two strings, that is the number of characters that need to be changed to turn one
     * string into the other. The two strings will be cleaned with [cleanStringForDistance] before calculating the distance. Use [customStringDistance] for better results when e.g. comparing app names.
     * @see customStringDistance
     * @param aNotCleaned the first string
     * @param bNotCleaned the second string
     * @return the Levenshtein distance between the two cleaned strings, lower is better, values are
     * always greater than or equal to 0
     */
    fun levenshteinDistance(aNotCleaned: String, bNotCleaned: String): Int {
        val a = cleanStringForDistance(aNotCleaned)
        val b = cleanStringForDistance(bNotCleaned)
        return levenshteinDistanceMemory(a, b)[a.length][b.length]
    }

    /**
     * Calculates some statistics about the
     * [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance) between
     * the two strings. Follows the path chosen by the dynamic programming algorithm to obtain the
     * total number of matched characters and the maximum number of roughly subsequent characters
     * matched.
     *
     * @param a the first string, maybe cleaned with [cleanStringForDistance]
     * @param b the second string, maybe cleaned with [cleanStringForDistance]
     * @return a triple of Levenshtein distance, max subsequent chars and matching char count
     */
    private fun stringDistanceStats(a: String, b: String): StringDistanceStats {
        val memory = levenshteinDistanceMemory(a, b)
        var matchingCharCount = 0
        var subsequentChars = 0
        var maxSubsequentChars = 0
        for (pos in pathInLevenshteinMemory(a, b, memory)) {
            if (pos.match) {
                ++matchingCharCount
                ++subsequentChars
                maxSubsequentChars = max(maxSubsequentChars, subsequentChars)
            } else {
                subsequentChars = max(0, subsequentChars - 1)
            }
        }
        return StringDistanceStats(
            memory[a.length][b.length], maxSubsequentChars,
            matchingCharCount
        )
    }

    /**
     * Calculates a custom string distance between the two provided strings, based on the statistics
     * drawn by [stringDistanceStats]. Seems to work well when matching
     * names of objects, where the difference in length between the two strings counts by some
     * factor, but max subsequent chars and matching char count also play a big role.
     *
     * @param aNotCleaned the first string
     * @param bNotCleaned the second string
     * @return the custom string distance between the two cleaned strings, lower is better, values
     * can be lower than 0, values are always less than or equal to the [levenshteinDistance] between the two strings
     */
    fun customStringDistance(aNotCleaned: String, bNotCleaned: String): Int {
        val a = cleanStringForDistance(aNotCleaned)
        val b = cleanStringForDistance(bNotCleaned)
        return customStringDistanceCleaned(a, b)
    }

    /**
     * See [customStringDistance]; this one assumes already clean strings.
     */
    fun customStringDistanceCleaned(aCleaned: String, bCleaned: String): Int {
        val stats = stringDistanceStats(aCleaned, bCleaned)
        return stats.levenshteinDistance - stats.maxSubsequentChars - stats.matchingCharCount
    }

    /**
     * Calculates a custom string distance between the two provided strings, based on the statistics
     * drawn by [stringDistanceStats]. Seems to work well when matching
     * contact names, where the difference in length between the two strings is mostly irrelevant,
     * and what mostly counts are max subsequent chars and matching char count.
     *
     * @param aNotCleaned the first string
     * @param bNotCleaned the second string
     * @return the custom string distance between the two cleaned strings, lower is better, values
     * will always be lower than or equal to 0
     */
    fun contactStringDistance(aNotCleaned: String, bNotCleaned: String): Int {
        val a = cleanStringForDistance(aNotCleaned)
        val b = cleanStringForDistance(bNotCleaned)
        val stats = stringDistanceStats(a, b)
        return -stats.maxSubsequentChars - stats.matchingCharCount
    }

    private class LevenshteinMemoryPos(
        val i: Int,
        val j: Int,
        val match: Boolean
    )

    private class StringDistanceStats(
        val levenshteinDistance: Int,
        val maxSubsequentChars: Int,
        val matchingCharCount: Int
    )
}

/**
 * @param locale the current user locale
 * @return the whole [this] lowercase, with only the first letter uppercase.
 */
fun String.lowercaseCapitalized(locale: Locale): String {
    return lowercase(locale).replaceFirstChar { it.titlecase(locale) }
}
