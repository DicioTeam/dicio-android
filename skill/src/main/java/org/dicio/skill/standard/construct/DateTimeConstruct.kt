package org.dicio.skill.standard.construct

import java.time.LocalDateTime
import org.dicio.numbers.parser.param.ParserParams
import org.dicio.skill.standard.util.MatchHelper

/**
 * For [now], [shortScale] and [preferMonthBeforeDay] parameter descriptions, see
 * [org.dicio.numbers.parser.param.ExtractDateTimeParams].
 */
class DateTimeConstruct(
    name: String,
    weight: Float = DEFAULT_WEIGHT,
    bonusIfLargestPossible: Float = DEFAULT_BONUS_IF_LARGEST_POSSIBLE,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
    private val shortScale: Boolean = true,
    private val preferMonthBeforeDay: Boolean = false,
) : RangesConstruct<LocalDateTime>(name, weight, bonusIfLargestPossible) {

    override fun parserParams(helper: MatchHelper): ParserParams<LocalDateTime>? {
        return helper.parserFormatter
            ?.extractDateTime(helper.userInput)
            ?.now(now())
            ?.shortScale(shortScale)
            ?.preferMonthBeforeDay(preferMonthBeforeDay)
    }

    override fun uniqueKeyForCaching(): String {
        return "${this::class.simpleName}: now=$now, shortScale=$shortScale, " +
                "preferMonthBeforeDay=$preferMonthBeforeDay"
    }
}
