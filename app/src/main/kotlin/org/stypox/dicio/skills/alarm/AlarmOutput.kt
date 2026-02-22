package org.stypox.dicio.skills.alarm

import androidx.compose.runtime.Composable
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.AlwaysBestScore
import org.dicio.skill.skill.AlwaysWorstScore
import org.dicio.skill.skill.InteractionPlan
import org.dicio.skill.skill.Score
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.skill.Specificity
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.util.getString
import java.time.LocalDateTime

sealed interface AlarmOutput : SkillOutput {
    class Set(
        private val hour: Int,
        private val minute: Int,
    ) : AlarmOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            val timeString = formatTime(hour, minute)
            return ctx.getString(R.string.skill_alarm_set, timeString)
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            val timeString = formatTime(hour, minute)
            Headline(text = ctx.getString(R.string.skill_alarm_graphical_set, timeString))
        }

        private fun formatTime(hour: Int, minute: Int): String {
            val period = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format("%d:%02d %s", displayHour, minute, period)
        }
    }

    class SetAskTime(
        private val onGotTime: suspend (LocalDateTime) -> SkillOutput,
    ) : AlarmOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_alarm_what_time)

        override fun getInteractionPlan(ctx: SkillContext): InteractionPlan {
            val timeSkill = object : Skill<LocalDateTime?>(AlarmInfo, Specificity.HIGH) {
                override fun score(
                    ctx: SkillContext,
                    input: String
                ): Pair<Score, LocalDateTime?> {
                    val time = ctx.parserFormatter!!
                        .extractDateTime(input)
                        .first

                    return Pair(
                        if (time == null) AlwaysWorstScore else AlwaysBestScore,
                        time
                    )
                }

                override suspend fun generateOutput(
                    ctx: SkillContext,
                    inputData: LocalDateTime?
                ): SkillOutput {
                    return if (inputData == null) {
                        throw RuntimeException("AlwaysWorstScore still triggered generateOutput")
                    } else {
                        onGotTime(inputData)
                    }
                }
            }

            return InteractionPlan.StartSubInteraction(
                reopenMicrophone = true,
                nextSkills = listOf(timeSkill),
            )
        }
    }

    class OpeningClockApp : AlarmOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_alarm_opening_clock_app)
    }

    class NoClockApp : AlarmOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_alarm_no_clock_app)
    }
}
