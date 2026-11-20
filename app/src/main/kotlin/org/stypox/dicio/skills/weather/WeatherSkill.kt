package org.stypox.dicio.skills.weather

import kotlinx.coroutines.flow.first
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Weather
import org.stypox.dicio.skills.weather.WeatherInfo.weatherDataStore
import org.stypox.dicio.util.ConnectionUtils
import org.stypox.dicio.util.StringUtils
import java.io.FileNotFoundException
import java.util.Locale
import kotlin.math.roundToInt

class WeatherSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Weather>) :
    StandardRecognizerSkill<Weather>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Weather): SkillOutput {
        val prefs = ctx.android.weatherDataStore.data.first()
        val city = getCity(prefs, inputData) ?: return WeatherOutput.Failed(city = "")

        val language = ctx.locale.language.lowercase(Locale.getDefault())
        // OpenWeatherMap only knows the nominative case of city names, e.g. "москва" and not the
        // "москве" in "погода в москве", so try a few possible nominative forms
        val weatherData = cityNameCandidates(city, language).firstNotNullOfOrNull { candidate ->
            try {
                ConnectionUtils.getPageJson(
                    // always request in metric units (°C and m) and convert later
                    "$WEATHER_API_URL?APPID=$API_KEY&units=metric&lang=$language" +
                            "&q=" + ConnectionUtils.urlEncode(candidate)
                )
            } catch (_: FileNotFoundException) {
                null // city not found
            }
        } ?: return WeatherOutput.Failed(city = city)

        val weatherObject = weatherData.getJSONArray("weather").getJSONObject(0)
        val mainObject = weatherData.getJSONObject("main")
        val windObject = weatherData.getJSONObject("wind")

        val tempUnit = ResolvedTemperatureUnit.from(prefs)
        val temp = mainObject.getDouble("temp")
        val tempConverted = tempUnit.convert(temp)
        return WeatherOutput.Success(
            city = weatherData.getString("name"),
            description = weatherObject.getString("description")
                .apply { this[0].uppercaseChar() + this.substring(1) },
            iconUrl = ICON_BASE_URL + weatherObject.getString("icon") + ICON_FORMAT,
            temp = temp,
            tempMin = mainObject.getDouble("temp_min"),
            tempMax = mainObject.getDouble("temp_max"),
            tempString = ctx.parserFormatter
                ?.niceNumber(tempConverted.roundToInt().toDouble())?.speech(true)?.get()
                ?: (tempConverted.roundToInt().toString()),
            windSpeed = windObject.getDouble("speed"),
            temperatureUnit = tempUnit,
            lengthUnit = ResolvedLengthUnit.from(prefs),
        )
    }

    private fun getCity(prefs: SkillSettingsWeather, inputData: Weather): String? {
        var city = when (inputData) {
            is Weather.Current -> inputData.where
        }

        if (city.isNullOrEmpty()) {
            city = StringUtils.removePunctuation(prefs.defaultCity.trim { ch -> ch <= ' ' })
        }

        if (city.isEmpty()) {
            city = ConnectionUtils.getPageJson(IP_INFO_URL).getString("city")
        }

        if (city.isNullOrEmpty()) {
            return null
        }
        return city
    }

    companion object {
        /**
         * @return the city name itself, followed by guesses of its nominative form for languages
         * whose prepositions require other cases (e.g. "в москве" -> москва, "в твери" -> тверь,
         * "в нижнем новгороде" -> нижний новгород)
         */
        internal fun cityNameCandidates(city: String, language: String): List<String> {
            if (language != "ru" && language != "uk" && language != "be") {
                return listOf(city)
            }

            val words = city.trim().split(Regex("\\s+"))
            val last = words.last().lowercase()
            val lastOptions = when {
                // киеве, ростове -> киев, ростов; иванове -> иваново
                last.endsWith("ове") || last.endsWith("еве") -> listOf(last.dropLast(1), last.dropLast(1) + "о")
                last.endsWith("ии") -> listOf(last.dropLast(1) + "я") // германии -> германия
                last.endsWith("е") -> listOf(last.dropLast(1) + "а", last.dropLast(1), last.dropLast(1) + "я")
                last.endsWith("и") -> listOf(last.dropLast(1) + "ь") // твери, электростали
                last.endsWith("ах") || last.endsWith("ях") -> listOf(last.dropLast(2) + "и") // химках
                last.endsWith("у") -> listOf(last.dropLast(1)) // в крыму
                else -> listOf()
            }
            // adjectives before the noun, e.g. нижнем -> нижний, великом -> великий, новом -> новый
            val firstWords = words.dropLast(1).map { word ->
                val w = word.lowercase()
                when {
                    w.endsWith("ем") -> w.dropLast(2) + "ий"
                    w.endsWith("ком") || w.endsWith("гом") || w.endsWith("хом") -> w.dropLast(2) + "ий"
                    w.endsWith("ом") -> w.dropLast(2) + "ый"
                    w.endsWith("ой") -> w.dropLast(2) + "ая"
                    else -> w
                }
            }
            return (listOf(city) + lastOptions.map { (firstWords + it).joinToString(" ") })
                .distinct()
        }

        private const val IP_INFO_URL = "https://ipinfo.io/json"
        private const val WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather"
        private const val API_KEY = "061f24cf3cde2f60644a8240302983f2"
        private const val ICON_BASE_URL = "https://openweathermap.org/img/wn/"
        private const val ICON_FORMAT = "@2x.png"
    }
}
