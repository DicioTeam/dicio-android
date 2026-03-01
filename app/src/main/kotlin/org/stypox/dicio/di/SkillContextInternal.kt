package org.stypox.dicio.di

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.util.MatchHelper

/**
 * Allows some fields in [SkillContext] to be modifiable by the evaluation infrastructure,
 * but not by skills who only get access to [SkillContext] (assuming they don't cast).
 */
interface SkillContextInternal : SkillContext {
    override var previousOutput: SkillOutput?
    override var standardMatchHelper: MatchHelper?
}