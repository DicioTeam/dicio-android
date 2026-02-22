package org.stypox.dicio.skills.alarm

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.skill.Skill
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Permission
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.util.PERMISSION_SET_ALARM

object AlarmInfo : SkillInfo("alarm") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_alarm)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_alarm)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Alarm)

    override val neededPermissions: List<Permission>
            = listOf(PERMISSION_SET_ALARM)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Alarm[ctx.sentencesLanguage] != null
                && ctx.parserFormatter != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return AlarmSkill(AlarmInfo, Sentences.Alarm[ctx.sentencesLanguage]!!)
    }
}
