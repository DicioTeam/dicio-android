package org.stypox.dicio.skills.media

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

object MediaInfo : SkillInfo("media") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_media)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_media)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.AutoMirrored.Filled.QueueMusic)

    override fun build(ctx: SkillContext): Skill<*>? {
        val data = Sentences.Media[ctx.sentencesLanguage] ?: return null
        return MediaSkill(MediaInfo, data)
    }
}
