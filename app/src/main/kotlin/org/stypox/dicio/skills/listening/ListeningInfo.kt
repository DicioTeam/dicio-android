package org.stypox.dicio.skills.listening

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.datastore.core.DataStore
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.settings.datastore.UserSettings

class ListeningInfo(
    val dataStore: DataStore<UserSettings>,
) : SkillInfo("listening") {
    override val categoryNameRes = R.string.category_utilities

    override fun name(context: Context) =
        context.getString(R.string.skill_name_listening)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_listening)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Hearing)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Listening[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return ListeningSkill(this, Sentences.Listening[ctx.sentencesLanguage]!!)
    }
}
