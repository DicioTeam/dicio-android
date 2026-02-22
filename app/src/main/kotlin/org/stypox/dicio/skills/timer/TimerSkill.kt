package org.stypox.dicio.skills.timer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences.Timer
import org.stypox.dicio.util.getString
import java.time.Duration

class TimerSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Timer>) :
    StandardRecognizerSkill<Timer>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Timer): SkillOutput {
        return when (inputData) {
            is Timer.Set -> {
                val duration = inputData.duration?.let {
                    ctx.parserFormatter?.extractDuration(it)?.first?.toJavaDuration()
                }
                if (duration == null) {
                    TimerOutput.SetAskDuration { setTimer(ctx, it) }
                } else {
                    setTimer(ctx, duration)
                }
            }
            is Timer.Query -> {
                showClockApp(ctx)
            }
            is Timer.Cancel -> {
                showClockApp(ctx)
            }
        }
    }

    private fun setTimer(
        ctx: SkillContext,
        duration: Duration,
    ): SkillOutput {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, duration.seconds.toInt())
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.android.startActivity(intent)
            
            return TimerOutput.Set(duration.toMillis())
        } catch (e: ActivityNotFoundException) {
            return TimerOutput.NoClockApp()
        }
    }

    private fun showClockApp(ctx: SkillContext): SkillOutput {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.android.startActivity(intent)
            
            return TimerOutput.OpeningClockApp()
        } catch (e: ActivityNotFoundException) {
            return TimerOutput.NoClockApp()
        }
    }
}
