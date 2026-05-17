package org.stypox.dicio.skills.music

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.fragment.app.Fragment
import org.dicio.skill.skill.Skill
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.skills.open.OpenSkill

object MusicInfo : SkillInfo("music") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_music)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_music)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.AutoMirrored.Filled.QueueMusic)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Music[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return MusicSkill(MusicInfo, Sentences.Music[ctx.sentencesLanguage]!!)
    }
}
