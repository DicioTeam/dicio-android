package org.stypox.dicio.skills.sms

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.AlwaysBestScore
import org.dicio.skill.skill.AlwaysWorstScore
import org.dicio.skill.skill.Score
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.skill.Specificity
import org.stypox.dicio.util.StringUtils

class SmsContactChooserName internal constructor(
    private val contacts: List<Pair<String, String>>,
    private val messageText: String?
) : Skill<Pair<String, String>?>(SmsInfo, Specificity.LOW) {

    override fun score(
        ctx: SkillContext,
        input: String
    ): Pair<Score, Pair<String, String>?> {
        val trimmedInput = input.trim { it <= ' ' }

        val bestContact = contacts
            .map { nameNumberPair ->
                Pair(
                    nameNumberPair,
                    StringUtils.contactStringDistance(trimmedInput, nameNumberPair.first)
                )
            }
            .filter { pair -> pair.second < -7 }
            .minByOrNull { a -> a.second }
            ?.first

        return Pair(
            if (bestContact == null) AlwaysWorstScore else AlwaysBestScore,
            bestContact
        )
    }

    override suspend fun generateOutput(ctx: SkillContext, inputData: Pair<String, String>?): SkillOutput {
        return inputData?.let {
            if (messageText == null) {
                AskMessageOutput(it.first, it.second)
            } else {
                ConfirmSmsOutput(it.first, it.second, messageText)
            }
        }
            // impossible situation
            ?: SentSmsOutput(null, null, null)
    }
}
