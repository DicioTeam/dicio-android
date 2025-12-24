package org.stypox.dicio.skills.rng

import org.dicio.numbers.unit.Number
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences.Rng
import org.stypox.dicio.util.getString
import kotlin.random.Random

class RngSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<Rng>
) : StandardRecognizerSkill<Rng>(correspondingSkillInfo, data) {

    private fun extractNumber(ctx: SkillContext, text: String?): Int? {
        return text?.let {
            ctx.parserFormatter?.extractNumber(it)?.mixedWithText
                ?.filterIsInstance<Number>()
                ?.firstOrNull()
                ?.integerValue()
                ?.toInt()
        }
    }

    override suspend fun generateOutput(ctx: SkillContext, inputData: Rng): SkillOutput {
        return when (inputData) {
            is Rng.CoinFlip -> {
                val result = if (Random.nextBoolean()) {
                    ctx.getString(R.string.skill_rng_heads)
                } else {
                    ctx.getString(R.string.skill_rng_tails)
                }
                RngOutput.CoinFlip(result)
            }

            is Rng.DiceRoll -> {
                val sides = extractNumber(ctx, inputData.sides) ?: 6
                val amount = extractNumber(ctx, inputData.amount) ?: 1
                
                if (sides <= 0 || amount <= 0 || amount > 100) {
                    return RngOutput.InvalidRange
                }
                
                val results = List(amount) { Random.nextInt(1, sides + 1) }
                RngOutput.DiceRoll(results, sides)
            }

            is Rng.RandomNumber -> {
                val min = extractNumber(ctx, inputData.min) ?: 1
                val max = extractNumber(ctx, inputData.max) ?: 100
                
                if (min > max) {
                    return RngOutput.InvalidRange
                }
                
                val result = Random.nextInt(min, max + 1)
                RngOutput.RandomNumber(result, min, max)
            }
        }
    }
}
