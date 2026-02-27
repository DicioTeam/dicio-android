package org.dicio.skill.standard.construct

import org.dicio.numbers.parser.param.ParserParams
import org.dicio.skill.standard.StandardScore
import org.dicio.skill.standard.capture.ParsedDataCapture
import org.dicio.skill.standard.util.MatchHelper
import org.dicio.skill.standard.util.addRefWeightEverywhere
import org.dicio.skill.standard.util.normalizeMemToEnd

/**
 * Like a [CapturingConstruct] but only captures objects of type [T] extracted using dicio-numbers,
 * e.g. numbers, durations and date/times. Will only include in the captures the words that belong
 * to a valid textual representation of an object of [T], and will extract the corresponding
 * computer-interpretable representation (i.e. the actual object of type [T]).
 */
abstract class RangesConstruct<T>(
    private val name: String,
    private val weight: Float = DEFAULT_WEIGHT,
    private val bonusIfLargestPossible: Float = DEFAULT_BONUS_IF_LARGEST_POSSIBLE,
) : Construct {

    /**
     * @return null if `dicio-numbers` cannot parse objects of type [T] in the current language
     */
    abstract fun parserParams(helper: MatchHelper): ParserParams<T>?

    /**
     * @return A unique identifier for the intervals computed by
     * [parserParams]`(helper).parsePossibleIntervals()` used as a key for caching. Make sure to
     * update this when adding new parameter fields to classes overriding [RangesConstruct]!
     */
    abstract fun uniqueKeyForCaching(): String

    // TODO add tests
    override fun matchToEnd(memToEnd: Array<StandardScore>, helper: MatchHelper) {
        val possibleRanges = helper.getOrTokenize(uniqueKeyForCaching()) { helper ->
            parserParams(helper)?.parsePossibleIntervals()
        }

        if (possibleRanges == null) {
            // dicio-numbers does not support T for this language, so paths that cross this
            // capturing group should not be considered at all during matches!
            addRefWeightEverywhere(memToEnd, refWeight = 1e9f) // <- large number
            return
        } else if (possibleRanges.isEmpty()) {
            // this capturing group simply does not match anywhere, add its refWeight
            addRefWeightEverywhere(memToEnd, refWeight = weight)
            return
        }

        // we modify memToEnd in-place and use originalMemToEnd to get original values
        val originalMemToEnd = memToEnd.clone()
        addRefWeightEverywhere(memToEnd, refWeight = weight)
        val cumulativeWeight = helper.cumulativeWeight

        // for all possible ranges, see whether using them could improve the score
        for ((start, end, parsedData, largestPossible) in possibleRanges) {
            val userWeight = cumulativeWeight[end] - cumulativeWeight[start]
            val refWeight = weight + (if (largestPossible) bonusIfLargestPossible else 0.0f)
            memToEnd[start] = StandardScore.keepBest(
                originalMemToEnd[end].plus(
                    userMatched = userWeight,
                    userWeight = userWeight,
                    refMatched = refWeight,
                    refWeight = refWeight,
                    capturingGroup = ParsedDataCapture(name, start, end, parsedData)
                ),
                memToEnd[start]
            )
        }

        normalizeMemToEnd(memToEnd, cumulativeWeight)
    }

    override fun toString(): String {
        return ".$name:${this::class.simpleName?.removeSuffix("Construct")}."
    }

    companion object {
        // TODO these seem sensible default values after playing around a bit, but they haven't been
        //  fine-tuned very well
        protected const val DEFAULT_WEIGHT = 1.0f
        protected const val DEFAULT_BONUS_IF_LARGEST_POSSIBLE = 0.5f
    }
}
