package org.stypox.dicio.skills.current_time

import org.dicio.skill.context.SkillContext
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.util.getString

sealed interface CurrentTimeOutput : HeadlineSpeechSkillOutput {
    class Here(private val timeStr: String) : CurrentTimeOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_time_current_time, timeStr)
    }

    class InCity(private val city: String, private val timeStr: String) : CurrentTimeOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_time_current_time_city, city, timeStr)
    }

    class CityNotFound(private val city: String) : CurrentTimeOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_time_city_not_found, city)
    }
}
