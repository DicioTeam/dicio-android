package org.stypox.dicio.skills.notify

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Permission
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.util.PERMISSION_NOTIFICATION_LISTENER

object NotifyInfo: SkillInfo("notify") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_notify)

    override fun sentenceExample(context: Context): String =
        context.getString(R.string.skill_sentence_example_notify)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.NotificationsActive)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Notify[ctx.sentencesLanguage] != null
    }

    override val neededPermissions: List<Permission>
            = listOf(PERMISSION_NOTIFICATION_LISTENER)

    override fun build(ctx: SkillContext): Skill<*> {
        return NotifySkill(NotifyInfo, Sentences.Notify[ctx.sentencesLanguage]!!)
    }
}