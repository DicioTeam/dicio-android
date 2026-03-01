package org.dicio.skill.standard

import org.dicio.skill.skill.Skill
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Score
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.Specificity

abstract class StandardRecognizerSkill<T>(
    correspondingSkillInfo: SkillInfo,
    private val data: StandardRecognizerData<T>,
    // do not manually specify a specificity here, but rather let it be taken from `data`,
    // unless you want to override the one in `data` for some specific reason
    specificity: Specificity = data.specificity,
) : Skill<T>(correspondingSkillInfo, specificity) {

    override fun score(ctx: SkillContext, input: String): Pair<Score, T> {
        return data.score(ctx, input)
    }
}
