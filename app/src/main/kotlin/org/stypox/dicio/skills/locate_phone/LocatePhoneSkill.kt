package org.stypox.dicio.skills.locate_phone

import org.stypox.dicio.sentences.Sentences.LocatePhone
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill

class LocatePhoneSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<LocatePhone>
) : StandardRecognizerSkill<LocatePhone>(correspondingSkillInfo, data) {
    override suspend fun generateOutput(ctx: SkillContext, inputData: LocatePhone): SkillOutput {
        return LocatePhoneOutput
    }
}
