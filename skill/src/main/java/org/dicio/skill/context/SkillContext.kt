package org.dicio.skill.context

import android.content.Context
import org.dicio.numbers.ParserFormatter
import org.dicio.skill.skill.SkillOutput
import java.util.Locale
import org.dicio.skill.standard.util.MatchHelper

/**
 * An interface for providing access to various services and information to skills. Contains the
 * Android context, the user locale, the parser/formatter and the speech output device.
 */
interface SkillContext {
    /**
     * The Android context, useful for example to get resources, etc.
     */
    val android: Context

    /**
     * The current user locale, useful for example to customize web requests to get the correct
     * language or country.
     */
    val locale: Locale

    /**
     * The currently active language identifier to use to select recognition resources, e.g. used
     * in Dicio to select a [org.dicio.skill.standard.StandardRecognizerData] of a specific skill
     * among the ones available in various languages. Will often act as a key in
     * language-to-resources hashmaps and will usually be equal to [locale]`.language`.
     */
    val sentencesLanguage: String

    /**
     * The number parser formatter for the current locale, useful for example to format a
     * number to show to the user or extract numbers from an utterance. Is set to `null` if
     * the current user language is not supported by any [ParserFormatter].
     * @see ParserFormatter
     */
    val parserFormatter: ParserFormatter?

    /**
     * The [SpeechOutputDevice] that should be used for skill speech output.
     */
    val speechOutputDevice: SpeechOutputDevice

    /**
     * The previous output belonging to the same interaction.
     */
    val previousOutput: SkillOutput?

    /**
     * Used by skills that use [org.dicio.skill.standard.StandardRecognizerData] to cache scoring
     * information across different skills. Will be `!= null` only while the input is being
     * processed, i.e. inside the body of [org.dicio.skill.skill.Skill.score].
     */
    val standardMatchHelper: MatchHelper?
}
