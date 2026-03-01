package org.stypox.dicio.util

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Score
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.standard.StandardRecognizerData
import org.stypox.dicio.sentences.Sentences.UtilYesNo

abstract class RecognizeYesNoSkill(
    correspondingSkillInfo: SkillInfo,
    private val data: StandardRecognizerData<UtilYesNo>,
) : Skill<Boolean>(correspondingSkillInfo, data.specificity) {
    override fun score(
        ctx: SkillContext,
        input: String
    ): Pair<Score, Boolean> {
        return data.score(ctx, input).let { (score, standardResult) ->
            Pair(score, standardResult is UtilYesNo.Yes)
        }
    }
}
