package org.stypox.dicio.skills.telephone

import android.Manifest
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.fragment.app.Fragment
import org.dicio.skill.skill.Skill
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Permission
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.util.PERMISSION_CALL_PHONE
import org.stypox.dicio.util.PERMISSION_READ_CONTACTS

object TelephoneInfo : SkillInfo("telephone") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_telephone)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_telephone)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Call)

    override val neededPermissions: List<Permission>
            = listOf(PERMISSION_READ_CONTACTS, PERMISSION_CALL_PHONE)

    override fun build(ctx: SkillContext): Skill<*>? {
        val data = Sentences.Telephone[ctx.sentencesLanguage] ?: return null
        val yesNoData = Sentences.UtilYesNo[ctx.sentencesLanguage] ?: return null
        return TelephoneSkill(TelephoneInfo, data, yesNoData)
    }
}
