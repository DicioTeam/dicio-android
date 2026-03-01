package org.stypox.dicio.skills.notify

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Notify

class NotifySkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Notify>) :
    StandardRecognizerSkill<Notify>(correspondingSkillInfo, data) {
    override suspend fun generateOutput(ctx: SkillContext, inputData: Notify): SkillOutput {
        return NotifyOutput(NotifyHandler.Instance?.getActiveNotificationsList())
    }
}