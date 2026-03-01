package org.stypox.dicio.skills.flashlight

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

object FlashlightInfo : SkillInfo("flashlight") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_flashlight)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_flashlight)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.FlashlightOn)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Flashlight[ctx.sentencesLanguage] != null &&
                ctx.android.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    @SuppressLint("NewApi") // since build() is not called if isAvailable() returned false
    override fun build(ctx: SkillContext): Skill<*> {
        return FlashlightSkill(FlashlightInfo, Sentences.Flashlight[ctx.sentencesLanguage]!!)
    }
}
