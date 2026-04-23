package org.stypox.dicio.skills.calendar

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Permission
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.util.PERMISSION_READ_CALENDAR

object CalendarInfo : SkillInfo("calendar") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_calendar)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_calendar)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Event)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Calendar[ctx.sentencesLanguage] != null
    }

    override val neededPermissions: List<Permission> = listOf(PERMISSION_READ_CALENDAR)

    override fun build(ctx: SkillContext): Skill<*> {
        return CalendarSkill(CalendarInfo, Sentences.Calendar[ctx.sentencesLanguage]!!)
    }
}
