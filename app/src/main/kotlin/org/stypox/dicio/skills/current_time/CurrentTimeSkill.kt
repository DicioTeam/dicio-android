package org.stypox.dicio.skills.current_time

import android.util.Log
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.CurrentTime
import org.stypox.dicio.skills.weather.WeatherSkill
import java.io.IOException
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class CurrentTimeSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<CurrentTime>)
    : StandardRecognizerSkill<CurrentTime>(correspondingSkillInfo, data) {
    override suspend fun generateOutput(ctx: SkillContext, inputData: CurrentTime): SkillOutput {
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(ctx.locale)

        val where = when (inputData) {
            is CurrentTime.Query -> inputData.where?.trim()
        }
        if (where.isNullOrEmpty()) {
            return CurrentTimeOutput.Here(LocalTime.now().format(formatter))
        }

        // the time zone of a city is found through OpenWeatherMap, which is already used by the
        // weather skill and returns the UTC offset of the city
        val cityData = try {
            WeatherSkill.fetchCurrentWeather(where, ctx.locale.language.lowercase(Locale.ROOT))
        } catch (e: IOException) {
            Log.w(TAG, "Can't get the time zone of $where", e)
            null
        } ?: return CurrentTimeOutput.CityNotFound(where)

        val offset = ZoneOffset.ofTotalSeconds(cityData.getInt("timezone"))
        return CurrentTimeOutput.InCity(
            city = cityData.optString("name").ifEmpty { where },
            timeStr = OffsetDateTime.now(offset).toLocalTime().format(formatter),
        )
    }

    companion object {
        private val TAG = CurrentTimeSkill::class.simpleName
    }
}
