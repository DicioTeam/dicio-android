package org.dicio.skill.standard

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import org.dicio.skill.skill.Specificity
import org.dicio.skill.standard.construct.WordConstruct
import org.dicio.skill.standard.construct.CompositeConstruct
import org.dicio.skill.standard.util.MatchHelper

/**
 * Regression test: StandardRecognizerData.score(MatchHelper, input) must work
 * without ctx.standardMatchHelper being set. This is needed when skills call
 * score() during generateOutput (e.g. CalculatorSkill matching operators).
 */
class StandardRecognizerDataTest : StringSpec({

    "score with explicit MatchHelper does not require ctx" {
        // Build a simple recognizer that matches the word "plus"
        val construct = CompositeConstruct(
            listOf(WordConstruct("plus", false, false, 1.0f))
        )
        val data = StandardRecognizerData(
            specificity = Specificity.HIGH,
            converter = { input, sentenceId, _ -> sentenceId },
            sentencesWithId = listOf(Pair("plus_sentence", construct)),
        )

        val helper = MatchHelper(parserFormatter = null, userInput = "plus")
        val (score, result) = data.score(helper, "plus")

        score shouldNotBe null
        result shouldNotBe null
    }

    "score with explicit MatchHelper returns low score for non-matching input" {
        val construct = CompositeConstruct(
            listOf(WordConstruct("plus", false, false, 1.0f))
        )
        val data = StandardRecognizerData(
            specificity = Specificity.HIGH,
            converter = { input, sentenceId, _ -> sentenceId },
            sentencesWithId = listOf(Pair("plus_sentence", construct)),
        )

        val helper = MatchHelper(parserFormatter = null, userInput = "banana")
        val (score, _) = data.score(helper, "banana")

        // should score poorly since "banana" doesn't match "plus"
        assert(score.scoreIn01Range() < 0.5f)
    }
})
