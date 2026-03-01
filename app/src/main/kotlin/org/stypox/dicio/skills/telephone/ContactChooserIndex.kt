package org.stypox.dicio.skills.telephone

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.AlwaysBestScore
import org.dicio.skill.skill.AlwaysWorstScore
import org.dicio.skill.skill.Score
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.skill.Specificity

class ContactChooserIndex internal constructor(private val contacts: List<Pair<String, String>>) :
    Skill<Int>(TelephoneInfo, Specificity.HIGH) {

    override fun score(
        ctx: SkillContext,
        input: String
    ): Pair<Score, Int> {
        val index = ctx.parserFormatter!!
            .extractNumber(input)
            .preferOrdinal(true)
            .integerOnly(true)
            .parseFirstIfInteger() ?: 0
        return Pair(
            if (index <= 0 || index > contacts.size) AlwaysWorstScore else AlwaysBestScore,
            index.toInt()
        )
    }

    override suspend fun generateOutput(ctx: SkillContext, inputData: Int): SkillOutput {
        if (inputData > 0 && inputData <= contacts.size) {
            val contact = contacts[inputData - 1]
            return ConfirmCallOutput(contact.first, contact.second)
        } else {
            // impossible situation
            return ConfirmedCallOutput(null)
        }
    }
}
