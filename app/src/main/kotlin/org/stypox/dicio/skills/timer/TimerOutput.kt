package org.stypox.dicio.skills.timer

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import org.dicio.numbers.ParserFormatter
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
import java.time.Duration
import kotlin.math.absoluteValue

sealed interface TimerOutput : SkillOutput {
    class Set(
        private val milliseconds: Long,
    ) : TimerOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_timer_set, getFormattedDuration(ctx.parserFormatter!!, milliseconds, true))

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            val durationText = getFormattedDuration(
                ctx.parserFormatter!!,
                milliseconds,
                false,
            )
            Headline(text = ctx.getString(R.string.skill_timer_graphical_set, durationText))
        }
    }

    class SetAskDuration(
        private val onGotDuration: suspend (Duration) -> SkillOutput,
    ) : TimerOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_timer_how_much_time)

        override fun getInteractionPlan(ctx: SkillContext): InteractionPlan {
            val durationSkill = object : Skill<Duration?>(TimerInfo, Specificity.HIGH) {
                override fun score(
                    ctx: SkillContext,
                    input: String
                ): Pair<Score, Duration?> {
                    val duration = ctx.parserFormatter!!
                        .extractDuration(input)
                        .first
                        ?.toJavaDuration()

                    return Pair(
                        if (duration == null) AlwaysWorstScore else AlwaysBestScore,
                        duration
                    )
                }

                override suspend fun generateOutput(
                    ctx: SkillContext,
                    inputData: Duration?
                ): SkillOutput {
                    return if (inputData == null) {
                        // impossible situation, since AlwaysWorstScore was used above
                        throw RuntimeException("AlwaysWorstScore still triggered generateOutput")
                    } else {
                        onGotDuration(inputData)
                    }
                }
            }

            return InteractionPlan.StartSubInteraction(
                reopenMicrophone = true,
                nextSkills = listOf(durationSkill),
            )
        }
    }

    class OpeningClockApp : TimerOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_timer_opening_clock_app)
    }

    class NoClockApp : TimerOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_timer_no_clock_app)
    }
}

fun formatStringWithName(
    ctx: SkillContext,
    name: String?,
    milliseconds: Long,
    @StringRes stringWithoutName: Int,
    @StringRes stringWithName: Int
): String {
    val duration = getFormattedDuration(ctx.parserFormatter!!, milliseconds, true)
    return if (name == null) {
        ctx.getString(stringWithoutName, duration)
    } else {
        ctx.getString(stringWithName, name, duration)
    }
}

fun formatStringWithName(
    context: Context,
    name: String?,
    @StringRes stringWithoutName: Int,
    @StringRes stringWithName: Int
): String {
    return if (name == null) {
        context.getString(stringWithoutName)
    } else {
        context.getString(stringWithName, name)
    }
}

fun getFormattedDuration(
    parserFormatter: ParserFormatter,
    milliseconds: Long,
    speech: Boolean
): String {
    val niceDuration = parserFormatter
        .niceDuration(org.dicio.numbers.unit.Duration(Duration.ofMillis(milliseconds.absoluteValue)))
        .speech(speech)
        .get()

    return niceDuration
}
