package org.stypox.dicio.skills.checklist

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.InteractionPlan
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.util.RecognizeYesNoSkill
import org.stypox.dicio.util.getString

/**
 * A [SkillOutput] where the graphical output is just a headline text with the speech output.
 */
abstract class ChecklistYesNoSkillOutput(private val literal: String) : HeadlineSpeechSkillOutput {
    override fun getSpeechOutput(ctx: SkillContext): String = literal

    abstract suspend fun onYes(ctx: SkillContext): SkillOutput

    override fun getInteractionPlan(ctx: SkillContext): InteractionPlan =
        InteractionPlan.ReplaceSubInteraction(
            true, listOf(
                object : RecognizeYesNoSkill(
                    ChecklistInfo,
                    Sentences.UtilYesNo[ctx.sentencesLanguage]!!
                ) {
                    override suspend fun generateOutput(
                        ctx: SkillContext,
                        inputData: Boolean
                    ): SkillOutput =
                        if (inputData) {
                            onYes(ctx)
                        } else {
                            ChecklistSkillOutput(
                                ctx.getString(R.string.skill_checklist_unrecognized),
                                InteractionPlan.FinishInteraction
                            )
                        }
                }
            ))
}
