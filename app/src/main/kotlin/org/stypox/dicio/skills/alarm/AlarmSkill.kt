package org.stypox.dicio.skills.alarm

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Alarm
import java.time.LocalDateTime

class AlarmSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Alarm>) :
    StandardRecognizerSkill<Alarm>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Alarm): SkillOutput {
        return when (inputData) {
            is Alarm.Set -> {
                val time = inputData.time?.let {
                    ctx.parserFormatter?.extractDateTime(it)?.first
                }
                if (time == null) {
                    AlarmOutput.SetAskTime { setAlarm(ctx, it) }
                } else {
                    setAlarm(ctx, time)
                }
            }
            is Alarm.Show -> {
                showClockApp(ctx)
            }
        }
    }

    private fun setAlarm(
        ctx: SkillContext,
        time: LocalDateTime,
    ): SkillOutput {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, time.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, time.minute)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.android.startActivity(intent)
            
            return AlarmOutput.Set(time.hour, time.minute)
        } catch (e: ActivityNotFoundException) {
            return AlarmOutput.NoClockApp()
        }
    }

    private fun showClockApp(ctx: SkillContext): SkillOutput {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.android.startActivity(intent)
            
            return AlarmOutput.OpeningClockApp()
        } catch (e: ActivityNotFoundException) {
            return AlarmOutput.NoClockApp()
        }
    }
}
