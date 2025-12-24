package org.stypox.dicio.skills.random

import org.dicio.numbers.unit.Number
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences.Random
import org.stypox.dicio.util.getString
import kotlin.random.Random as KotlinRandom

class RandomSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<Random>
) : StandardRecognizerSkill<Random>(correspondingSkillInfo, data) {

    private fun extractNumber(ctx: SkillContext, text: String?): Int? {
        return text?.let {
            ctx.parserFormatter?.extractNumber(it)?.mixedWithText
                ?.filterIsInstance<Number>()
                ?.firstOrNull()
                ?.integerValue()
                ?.toInt()
        }
    }

    override suspend fun generateOutput(ctx: SkillContext, inputData: Random): SkillOutput {
        return when (inputData) {
            is Random.CoinFlip -> {
                val result = if (KotlinRandom.nextBoolean()) {
                    ctx.getString(R.string.skill_random_heads)
                } else {
                    ctx.getString(R.string.skill_random_tails)
                }
                RandomOutput.CoinFlip(result)
            }

            is Random.DiceRoll -> {
                val sides = extractNumber(ctx, inputData.sides) ?: 6
                val amount = extractNumber(ctx, inputData.amount) ?: 1
                
                if (sides <= 0 || amount <= 0 || amount > 100) {
                    return RandomOutput.InvalidRange
                }
                
                val results = List(amount) { KotlinRandom.nextInt(1, sides + 1) }
                RandomOutput.DiceRoll(results, sides)
            }

            is Random.RandomNumber -> {
                val min = extractNumber(ctx, inputData.min) ?: 1
                val max = extractNumber(ctx, inputData.max) ?: 100
                
                if (min > max) {
                    return RandomOutput.InvalidRange
                }
                
                val result = KotlinRandom.nextInt(min, max + 1)
                RandomOutput.RandomNumber(result, min, max)
            }
        }
    }
}
