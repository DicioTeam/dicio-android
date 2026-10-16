package org.stypox.dicio.skills.locate_phone

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

object LocatePhoneInfo : SkillInfo("locate_phone") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_locate_phone)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_locate_phone)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.EmojiEmotions)

    override fun build(ctx: SkillContext): Skill<*>? {
        val data = Sentences.LocatePhone[ctx.sentencesLanguage] ?: return null
        return LocatePhoneSkill(this, data)
    }
}
