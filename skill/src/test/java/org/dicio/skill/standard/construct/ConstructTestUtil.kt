package org.dicio.skill.standard.construct

import io.kotest.assertions.withClue
import io.kotest.matchers.EqualityMatcherResult
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import org.dicio.skill.standard.StandardScore
import org.dicio.skill.standard.capture.Capture
import org.dicio.skill.standard.capture.StringCapture
import org.dicio.skill.standard.util.MatchHelper
import org.dicio.skill.standard.util.cumulativeWeight
import org.dicio.skill.standard.util.initialMemToEnd
import org.dicio.skill.standard.util.normalizeMemToEnd
import kotlin.math.abs

fun Construct.withInput(userInput: String): Pair<Construct, String> {
    return Pair(this, userInput)
}

fun Pair<Construct, String>.withStartingMemToEnd(
    vararg startingMemToEnd: StandardScore
): Triple<Construct, String, Array<StandardScore>> {
    val (construct, userInput) = this
    startingMemToEnd shouldHaveSize userInput.length+1

    val normalizedStartingMemToEnd = arrayOf(*startingMemToEnd)
    normalizeMemToEnd(normalizedStartingMemToEnd, cumulativeWeight(userInput))
    withClue("startingMemToEnd is supposed to be already normalized") {
        startingMemToEnd should beEqualToPlusOrMinus(*normalizedStartingMemToEnd)
    }

    return Triple(construct, userInput, arrayOf(*startingMemToEnd))
}

fun Pair<Construct, String>.withStartingZeroedMemToEnd():
        Triple<Construct, String, Array<StandardScore>> {
    val (construct, userInput) = this
    return Triple(construct, userInput, Array(userInput.length+1) { s(0f,0f,0f,0f) })
}

fun Pair<Construct, String>.withStartingInitialMemToEnd():
        Triple<Construct, String, Array<StandardScore>> {
    val (construct, userInput) = this
    val memToEnd = initialMemToEnd(cumulativeWeight(userInput))
    return Triple(construct, userInput, memToEnd)
}

fun Triple<Construct, String, Array<StandardScore>>.shouldChangeMemToEndInto(
    vararg changedMemToEnd: StandardScore
) {
    val (construct, userInput, startingMemToEnd) = this
    changedMemToEnd shouldHaveSize userInput.length+1

    val normalizedChangedMemToEnd = arrayOf(*changedMemToEnd)
    normalizeMemToEnd(normalizedChangedMemToEnd, cumulativeWeight(userInput))
    withClue("changedMemToEnd is supposed to be already normalized") {
        changedMemToEnd should beEqualToPlusOrMinus(*normalizedChangedMemToEnd)
    }

    val helper = MatchHelper(null, userInput)
    construct.matchToEnd(startingMemToEnd, helper)

    // checking that changedMemToEnd is normalized above
    // also ensures that startingMemToEnd is normalized
    startingMemToEnd should beEqualToPlusOrMinus(*changedMemToEnd)
}

fun Triple<Construct, String, Array<StandardScore>>.shouldNotMatchAnything(
    additionalRefWeight: Float,
) {
    val (construct, userInput, startingMemToEnd) = this
    val changedMemToEnd = startingMemToEnd
        .map { it.plus(refWeight = additionalRefWeight) }
        .toTypedArray()

    val helper = MatchHelper(null, userInput)
    construct.matchToEnd(startingMemToEnd, helper)

    startingMemToEnd should beEqualToPlusOrMinus(*changedMemToEnd)
}

fun s(
    userMatched: Float,
    userWeight: Float,
    refMatched: Float,
    refWeight: Float,
    vararg capturingGroups: Capture
): StandardScore {
    var result = StandardScore(
        userMatched = userMatched,
        userWeight = userWeight,
        refMatched = refMatched,
        refWeight = refWeight,
        capturingGroups = null,
    )

    for (capturingGroup in capturingGroups) {
        result = result.plus(capturingGroup = capturingGroup)
    }

    return result
}

fun capt(name: String, start: Int, end: Int): StringCapture {
    return StringCapture(name, start, end)
}

fun flattenCapturingGroups(node: Any?): Set<Capture> {
    return when (node) {
        null ->
            setOf()

        is Pair<*, *> ->
            flattenCapturingGroups(node.first) +
                    flattenCapturingGroups(node.second)

        is Capture ->
            setOf(node)

        else ->
            throw IllegalArgumentException(
                "Unexpected type found in capturing groups tree: type=${
                    node::class.simpleName
                }, value=$node"
            )
    }
}

fun beEqualToPlusOrMinus(vararg expected: StandardScore) = object : Matcher<Array<out StandardScore>> {
    override fun test(value: Array<out StandardScore>): MatcherResult {
        if (expected.size != value.size) {
            return EqualityMatcherResult.invoke(
                passed = false,
                actual = value,
                expected = expected,
                failureMessageFn = { "arrays have different lengths (expected size = ${
                    expected.size}, actual size = ${value.size})" },
                negatedFailureMessageFn = { "arrays are the same" }
            )
        }

        fun generateError(i: Int, field: String): EqualityMatcherResult {
            return EqualityMatcherResult.invoke(
                passed = false,
                actual = value,
                expected = expected,
                failureMessageFn = { "arrays differ in $field at position $i (expected = ${
                    expected[i]}, actual = ${value[i]})" },
                negatedFailureMessageFn = { "arrays are the same" }
            )
        }
        for (i in expected.indices) {
            if (abs(expected[i].userMatched - value[i].userMatched) > 0.0001f) {
                return generateError(i, "userMatched")
            }
            if (abs(expected[i].userWeight - value[i].userWeight) > 0.0001f) {
                return generateError(i, "userWeight")
            }
            if (abs(expected[i].refMatched - value[i].refMatched) > 0.0001f) {
                return generateError(i, "refMatched")
            }
            if (abs(expected[i].refWeight - value[i].refWeight) > 0.0001f) {
                return generateError(i, "refWeight")
            }
            if (flattenCapturingGroups(expected[i].capturingGroups) !=
                flattenCapturingGroups(value[i].capturingGroups)) {
                return generateError(i, "capturingGroups")
            }
        }

        return EqualityMatcherResult.invoke(
            passed = true,
            actual = value,
            expected = expected,
            failureMessageFn = { "arrays are different" },
            negatedFailureMessageFn = { "arrays are the same" }
        )
    }
}
