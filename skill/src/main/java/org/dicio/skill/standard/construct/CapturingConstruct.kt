package org.dicio.skill.standard.construct

import org.dicio.skill.standard.StandardScore
import org.dicio.skill.standard.capture.StringCapture
import org.dicio.skill.standard.util.MatchHelper
import org.dicio.skill.standard.util.normalizeMemToEnd

data class CapturingConstruct(
    private val name: String,
    // By default, don't assign extra points for having matched a capturing group: the fact that the
    // capturing group can match any part of the input already gives enough points from the words
    // that were actually matched. Also, if there is an optional word next to a capturing group that
    // matches with an input word, this will ensure that it is not included in the capturing group
    // but is instead matched.
    private val weight: Float = 0.0f,
) : Construct {
    override fun matchToEnd(memToEnd: Array<StandardScore>, helper: MatchHelper) {
        val cumulativeWeight = helper.cumulativeWeight
        val cumulativeWhitespace = helper.cumulativeWhitespace
        val originalMemToEnd = memToEnd.clone()

        var lastCapturingGroupEnd: Int = helper.userInput.length
        for (start in helper.userInput.indices.reversed()) {
            val userWeight = cumulativeWeight[lastCapturingGroupEnd] - cumulativeWeight[start]
            val hasOnlyWhitespace = (lastCapturingGroupEnd - start) ==
                (cumulativeWhitespace[lastCapturingGroupEnd] - cumulativeWhitespace[start])

            val ifSkippingCapturingGroup = memToEnd[start].plus(refWeight = weight)
            if (hasOnlyWhitespace) {
                // don't create any capture, since it would only contain spaces
                memToEnd[start] = ifSkippingCapturingGroup
                continue
            }

            // using originalMemToEnd because memToEnd[lastCapturingGroupEnd] will already
            // have been changed
            val ifContinuingCapturingGroup = originalMemToEnd[lastCapturingGroupEnd].plus(
                userMatched = userWeight,
                userWeight = userWeight,
                refMatched = weight,
                refWeight = weight,
                capturingGroup = StringCapture(name, start, lastCapturingGroupEnd),
            )

            if (ifContinuingCapturingGroup.score() >= ifSkippingCapturingGroup.score()) {
                memToEnd[start] = ifContinuingCapturingGroup

                val ifCreatingNewCapturingGroup = originalMemToEnd[start].plus(
                    refMatched = weight,
                    refWeight = weight,
                )
                if (ifCreatingNewCapturingGroup.score() > ifContinuingCapturingGroup.score()) {
                    lastCapturingGroupEnd = start
                }
            } else {
                lastCapturingGroupEnd = start
                memToEnd[start] = ifSkippingCapturingGroup
            }
        }

        memToEnd[helper.userInput.length] = memToEnd[helper.userInput.length].plus(refWeight = weight)
        normalizeMemToEnd(memToEnd, helper.cumulativeWeight)
    }

    override fun toString(): String {
        return ".$name."
    }
}
