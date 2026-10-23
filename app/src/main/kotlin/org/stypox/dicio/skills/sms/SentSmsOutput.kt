package org.stypox.dicio.skills.sms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Body
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.util.getString

class SentSmsOutput(
    private val name: String?,
    private val number: String?,
    private val message: String?
) : SkillOutput {
    override fun getSpeechOutput(ctx: SkillContext): String = if (name == null) {
        ctx.getString(R.string.skill_sms_not_sending)
    } else {
        "" // do not speak anything since the message was sent
    }

    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        if (name == null) {
            Headline(text = stringResource(R.string.skill_sms_not_sending))
        } else {
            Column {
                Headline(text = stringResource(R.string.skill_sms_sent, name))
                Spacer(modifier = Modifier.height(4.dp))
                Body(text = number ?: "")
                if (message != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Body(text = "\"$message\"")
                }
            }
        }
    }
}
