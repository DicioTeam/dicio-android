package org.stypox.dicio.skills.flashlight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.error.UserAction
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.io.graphical.ReportButton
import org.stypox.dicio.util.getString

sealed interface FlashlightOutput : SkillOutput {
    data class Success(private val turnedOn: Boolean) : FlashlightOutput,
        HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            if (turnedOn) {
                ctx.getString(R.string.skill_flashlight_turned_on)
            } else {
                ctx.getString(R.string.skill_flashlight_turned_off)
            }
    }

    data class Error(private val throwable: Throwable?) : FlashlightOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_flashlight_error)

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Headline(text = getSpeechOutput(ctx))
                if (throwable != null) {
                    ReportButton(throwable, UserAction.FLASHLIGHT)
                }
            }
        }
    }
}
