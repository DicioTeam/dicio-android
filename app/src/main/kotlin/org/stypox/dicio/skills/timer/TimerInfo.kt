package org.stypox.dicio.skills.timer

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import kotlinx.coroutines.launch
import org.dicio.skill.skill.Skill
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.settings.ui.ListSetting

object TimerInfo : SkillInfo("timer") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_timer)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_timer)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Timer)

    override fun build(ctx: SkillContext): Skill<*>? {
        val data = Sentences.Timer[ctx.sentencesLanguage] ?: return null
        val yesNoData = Sentences.UtilYesNo[ctx.sentencesLanguage] ?: return null
        if (ctx.parserFormatter == null) return null
        return TimerSkill(TimerInfo, data, yesNoData)
    }

    // no need to use Hilt injection here, let DataStore take care of handling the singleton itself
    internal val Context.timerDataStore by dataStore(
        fileName = "skill_settings_timer.pb",
        serializer = SkillSettingsTimerSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            SkillSettingsTimerSerializer.defaultValue
        },
    )

    override val renderSettings: @Composable () -> Unit get() = @Composable {
        val dataStore = LocalContext.current.timerDataStore
        val data by dataStore.data.collectAsState(SkillSettingsTimerSerializer.defaultValue)
        val scope = rememberCoroutineScope()

        ListSetting(
            title = stringResource(R.string.pref_timer_app),
            possibleValues = listOf(
                ListSetting.Value(
                    value = TimerApp.TIMER_APP_CLOCK,
                    name = stringResource(R.string.pref_timer_app_clock),
                    description = stringResource(R.string.pref_timer_app_clock_description),
                ),
                ListSetting.Value(
                    value = TimerApp.TIMER_APP_DICIO,
                    name = stringResource(R.string.pref_timer_app_dicio),
                    description = stringResource(R.string.pref_timer_app_dicio_description),
                ),
            ),
        ).Render(
            value = when (val app = data.app) {
                TimerApp.UNRECOGNIZED -> TimerApp.TIMER_APP_CLOCK
                else -> app
            },
            onValueChange = { app ->
                scope.launch {
                    dataStore.updateData { settings -> settings.toBuilder().setApp(app).build() }
                }
            },
        )
    }
}
