package org.stypox.dicio.skills.timer

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LongState
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
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.util.RecognizeYesNoSkill
import org.stypox.dicio.util.getString
import java.text.DecimalFormatSymbols
import java.time.Duration
import kotlin.math.absoluteValue

sealed interface TimerOutput : SkillOutput {
    class Set(
        private val milliseconds: Long,
        private val lastTickMillis: LongState,
        private val name: String?
    ) : TimerOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = formatStringWithName(
            ctx, name, milliseconds, R.string.skill_timer_set, R.string.skill_timer_set_name
        )

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Headline(
                text = getFormattedDuration(
                    ctx.parserFormatter!!,
                    lastTickMillis.longValue,
                    false,
                ),
            )
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
                        .parseFirst()
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

    class Cancel(
        private val speechOutput: String,
    ) : TimerOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = speechOutput
    }

    class ConfirmCancel(
        private val onConfirm: () -> SkillOutput,
    ) : TimerOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_timer_confirm_cancel)

        override fun getInteractionPlan(ctx: SkillContext): InteractionPlan {
            val yesNoSentences = Sentences.UtilYesNo[ctx.sentencesLanguage]!!
            val confirmYesNoSkill = object : RecognizeYesNoSkill(TimerInfo, yesNoSentences) {
                override suspend fun generateOutput(
                    ctx: SkillContext,
                    inputData: Boolean
                ): SkillOutput {
                    return if (inputData) {
                        onConfirm()
                    } else {
                        Cancel(ctx.getString(R.string.skill_timer_none_canceled))
                    }
                }
            }

            return InteractionPlan.StartSubInteraction(
                reopenMicrophone = true,
                nextSkills = listOf(confirmYesNoSkill),
            )
        }
    }

    class Query(
        private val speechOutput: String,
    ) : TimerOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = speechOutput
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

    return if (speech) {
        niceDuration // no need to speak milliseconds
    } else {
        (if (milliseconds < 0) "-" else "") +
                niceDuration +
                DecimalFormatSymbols.getInstance().decimalSeparator +
                // show only the first decimal place, rounded to the nearest one
                ((milliseconds.absoluteValue + 50) / 100) % 10
    }
}
