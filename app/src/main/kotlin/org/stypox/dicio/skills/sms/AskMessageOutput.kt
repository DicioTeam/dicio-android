package org.stypox.dicio.skills.sms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.AlwaysBestScore
import org.dicio.skill.skill.AlwaysWorstScore
import org.dicio.skill.skill.InteractionPlan
import org.dicio.skill.skill.Score
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.skill.Specificity
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Body
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.util.getString

class AskMessageOutput(
    private val name: String,
    private val number: String
) : SkillOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
        ctx.getString(R.string.skill_sms_what_message)

    override fun getInteractionPlan(ctx: SkillContext): InteractionPlan {
        val messageSkill = object : Skill<String>(SmsInfo, Specificity.HIGH) {
            override fun score(
                ctx: SkillContext,
                input: String
            ): Pair<Score, String> {
                val trimmedInput = input.trim()
                return Pair(
                    if (trimmedInput.isEmpty()) AlwaysWorstScore else AlwaysBestScore,
                    trimmedInput
                )
            }

            override suspend fun generateOutput(
                ctx: SkillContext,
                inputData: String
            ): SkillOutput {
                return ConfirmSmsOutput(name, number, inputData)
            }
        }

        return InteractionPlan.StartSubInteraction(
            reopenMicrophone = true,
            nextSkills = listOf(messageSkill),
        )
    }

    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        Column {
            Headline(text = getSpeechOutput(ctx))
            Spacer(modifier = Modifier.height(4.dp))
            Body(text = "$name ($number)")
        }
    }
}
