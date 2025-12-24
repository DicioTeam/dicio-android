package org.stypox.dicio.skills.random

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Body
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.io.graphical.Subtitle
import org.stypox.dicio.util.getString

sealed interface RandomOutput : SkillOutput {
    
    data class CoinFlip(private val result: String) : RandomOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_random_coin_flip, result)

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Headline(text = result.uppercase())
        }
    }

    data class DiceRoll(private val results: List<Int>, private val sides: Int) : RandomOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            val total = results.sum()
            return if (results.size == 1) {
                ctx.getString(R.string.skill_random_dice_roll, results[0], sides)
            } else {
                ctx.getString(R.string.skill_random_dice_roll_multiple, total, results.size, sides)
            }
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Headline(
                    text = if (results.size == 1) results[0].toString() else results.sum().toString()
                )
                if (results.size == 1) {
                    Subtitle(text = ctx.getString(R.string.skill_random_dice_sides, sides))
                } else {
                    Subtitle(text = ctx.getString(R.string.skill_random_dice_sides, sides))
                    Body(text = results.joinToString(" + ") + " = ${results.sum()}")
                }
            }
        }
    }

    data class RandomNumber(
        private val result: Int,
        private val min: Int,
        private val max: Int
    ) : RandomOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_random_random_number, result, min, max)

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Headline(text = result.toString())
                Subtitle(text = ctx.getString(R.string.skill_random_range, min, max))
            }
        }
    }

    data object InvalidRange : RandomOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_random_invalid_range)

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Headline(text = ctx.getString(R.string.skill_random_invalid_range))
        }
    }
}
