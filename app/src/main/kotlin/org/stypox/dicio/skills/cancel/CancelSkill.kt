package org.stypox.dicio.skills.cancel

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Cancel

class CancelSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<Cancel>
) : StandardRecognizerSkill<Cancel>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Cancel): SkillOutput {
        return CancelOutput()
    }
}
