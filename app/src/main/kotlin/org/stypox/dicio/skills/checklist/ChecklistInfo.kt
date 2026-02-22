package org.stypox.dicio.skills.checklist

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

object ChecklistInfo : SkillInfo("checklist") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_checklist)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_checklist)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Checklist)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Checklist[ctx.sentencesLanguage] != null &&
                Sentences.UtilYesNo[ctx.sentencesLanguage] != null &&
                ctx.parserFormatter != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return ChecklistSkill(ChecklistInfo, Sentences.Checklist[ctx.sentencesLanguage]!!)
    }

    // no need to use Hilt injection here, let DataStore take care of handling the singleton itself
    internal val Context.checklistDataStore by dataStore(
        fileName = "checklist.pb",
        serializer = SkillSettingsChecklistSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            SkillSettingsChecklistSerializer.defaultValue
        },
    )
}
