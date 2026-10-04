package org.stypox.dicio.skills.sms

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Permission
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.util.PERMISSION_READ_CONTACTS
import org.stypox.dicio.util.PERMISSION_SEND_SMS

object SmsInfo : SkillInfo("sms") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_sms)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_sms)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Message)

    override val neededPermissions: List<Permission>
        = listOf(PERMISSION_READ_CONTACTS, PERMISSION_SEND_SMS)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Sms[ctx.sentencesLanguage] != null &&
                Sentences.UtilYesNo[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return SmsSkill(SmsInfo, Sentences.Sms[ctx.sentencesLanguage]!!)
    }
}
