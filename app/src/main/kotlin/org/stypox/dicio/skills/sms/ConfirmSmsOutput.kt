package org.stypox.dicio.skills.sms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.InteractionPlan
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Body
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.util.RecognizeYesNoSkill
import org.stypox.dicio.util.getString

class ConfirmSmsOutput(
    private val name: String,
    private val number: String,
    private val message: String
) : SkillOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
        ctx.getString(R.string.skill_sms_confirm, message, name)

    override fun getInteractionPlan(ctx: SkillContext): InteractionPlan {
        val yesNoSentences = Sentences.UtilYesNo[ctx.sentencesLanguage]!!
        val confirmYesNoSkill = object : RecognizeYesNoSkill(SmsInfo, yesNoSentences) {
            override suspend fun generateOutput(
                ctx: SkillContext,
                inputData: Boolean
            ): SkillOutput {
                return if (inputData) {
                    SmsSkill.sendSms(number, message)
                    SentSmsOutput(name, number, message)
                } else {
                    SentSmsOutput(null, null, null)
                }
            }
        }

        return InteractionPlan.ReplaceSubInteraction(
            reopenMicrophone = true,
            nextSkills = listOf(confirmYesNoSkill),
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
