package org.stypox.dicio.skills.homeassistant

import android.content.Context
import org.dicio.numbers.ParserFormatter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.context.SpeechOutputDevice
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.util.MatchHelper
import java.util.Locale

class TestSkillContext(input: String) : SkillContext {
    override var standardMatchHelper: MatchHelper? = MatchHelper(null, input)
    override val parserFormatter: ParserFormatter? = null
    override val android: Context get() = throw NotImplementedError()
    override val locale: Locale get() = throw NotImplementedError()
    override val sentencesLanguage: String get() = throw NotImplementedError()
    override val speechOutputDevice: SpeechOutputDevice get() = throw NotImplementedError()
    override val previousOutput: SkillOutput get() = throw NotImplementedError()
}
