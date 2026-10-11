package org.stypox.dicio.skills.sms

import org.dicio.numbers.unit.Number
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.AlwaysBestScore
import org.dicio.skill.skill.AlwaysWorstScore
import org.dicio.skill.skill.Score
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.skill.Specificity

class SmsContactChooserIndex internal constructor(
    private val contacts: List<Pair<String, String>>,
    private val messageText: String?
) : Skill<Int>(SmsInfo, Specificity.HIGH) {

    override fun score(
        ctx: SkillContext,
        input: String
    ): Pair<Score, Int> {
        val index = ctx.parserFormatter!!
            .extractNumber(input)
            .preferOrdinal(true)
            .mixedWithText
            .asSequence()
            .filter { obj -> (obj as? Number)?.isInteger == true }
            .map { obj -> (obj as Number).integerValue().toInt() }
            .firstOrNull() ?: 0
        return Pair(
            if (index <= 0 || index > contacts.size) AlwaysWorstScore else AlwaysBestScore,
            index
        )
    }

    override suspend fun generateOutput(ctx: SkillContext, inputData: Int): SkillOutput {
        if (inputData > 0 && inputData <= contacts.size) {
            val contact = contacts[inputData - 1]
            return if (messageText == null) {
                AskMessageOutput(contact.first, contact.second)
            } else {
                ConfirmSmsOutput(contact.first, contact.second, messageText)
            }
        } else {
            // impossible situation
            return SentSmsOutput(null, null, null)
        }
    }
}
