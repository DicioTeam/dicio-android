package org.stypox.dicio.skills.unit_conversion

import android.content.res.Resources
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import java.util.Currency
import java.util.Locale

enum class UnitType {
    LENGTH, MASS, TEMPERATURE, VOLUME, AREA, SPEED, TIME, 
    DIGITAL_STORAGE, ENERGY, POWER, PRESSURE, ANGLE, CURRENCY,
    FREQUENCY, ACCELERATION, CONSUMPTION, DURATION
}

enum class Unit(val type: UnitType) {
    // LENGTH
    PICOMETER(UnitType.LENGTH), NANOMETER(UnitType.LENGTH), MICROMETER(UnitType.LENGTH),
    MILLIMETER(UnitType.LENGTH), CENTIMETER(UnitType.LENGTH), DECIMETER(UnitType.LENGTH),
    METER(UnitType.LENGTH), KILOMETER(UnitType.LENGTH), 
    INCH(UnitType.LENGTH), FOOT(UnitType.LENGTH), YARD(UnitType.LENGTH), 
    FATHOM(UnitType.LENGTH), FURLONG(UnitType.LENGTH), MILE(UnitType.LENGTH), 
    MILE_SCANDINAVIAN(UnitType.LENGTH), NAUTICAL_MILE(UnitType.LENGTH),
    LIGHT_YEAR(UnitType.LENGTH), ASTRONOMICAL_UNIT(UnitType.LENGTH), PARSEC(UnitType.LENGTH),

    // MASS
    MICROGRAM(UnitType.MASS), MILLIGRAM(UnitType.MASS), GRAM(UnitType.MASS), 
    KILOGRAM(UnitType.MASS), METRIC_TON(UnitType.MASS), TONNE(UnitType.MASS),
    OUNCE(UnitType.MASS), POUND(UnitType.MASS), STONE(UnitType.MASS), 
    TON(UnitType.MASS), OUNCE_TROY(UnitType.MASS), CARAT(UnitType.MASS),

    // TEMPERATURE
    CELSIUS(UnitType.TEMPERATURE), FAHRENHEIT(UnitType.TEMPERATURE), KELVIN(UnitType.TEMPERATURE),

    // VOLUME
    MILLILITER(UnitType.VOLUME), CENTILITER(UnitType.VOLUME), DECILITER(UnitType.VOLUME),
    LITER(UnitType.VOLUME), HECTOLITER(UnitType.VOLUME), MEGALITER(UnitType.VOLUME),
    CUBIC_CENTIMETER(UnitType.VOLUME), CUBIC_METER(UnitType.VOLUME), CUBIC_KILOMETER(UnitType.VOLUME),
    CUBIC_INCH(UnitType.VOLUME), CUBIC_FOOT(UnitType.VOLUME), CUBIC_YARD(UnitType.VOLUME), CUBIC_MILE(UnitType.VOLUME),
    TEASPOON(UnitType.VOLUME), TABLESPOON(UnitType.VOLUME), FLUID_OUNCE(UnitType.VOLUME),
    CUP(UnitType.VOLUME), CUP_METRIC(UnitType.VOLUME), 
    PINT(UnitType.VOLUME), PINT_METRIC(UnitType.VOLUME), 
    QUART(UnitType.VOLUME), GALLON(UnitType.VOLUME), GALLON_IMPERIAL(UnitType.VOLUME),
    BUSHEL(UnitType.VOLUME), ACRE_FOOT(UnitType.VOLUME),

    // AREA
    SQUARE_CENTIMETER(UnitType.AREA), SQUARE_METER(UnitType.AREA), SQUARE_KILOMETER(UnitType.AREA), 
    HECTARE(UnitType.AREA), SQUARE_INCH(UnitType.AREA), SQUARE_FOOT(UnitType.AREA), 
    SQUARE_YARD(UnitType.AREA), ACRE(UnitType.AREA), SQUARE_MILE(UnitType.AREA),

    // SPEED
    METER_PER_SECOND(UnitType.SPEED), KILOMETER_PER_HOUR(UnitType.SPEED),
    MILE_PER_HOUR(UnitType.SPEED), KNOT(UnitType.SPEED),

    // TIME
    NANOSECOND(UnitType.TIME), MICROSECOND(UnitType.TIME), MILLISECOND(UnitType.TIME), 
    SECOND(UnitType.TIME), MINUTE(UnitType.TIME), HOUR(UnitType.TIME), 
    DAY(UnitType.TIME), WEEK(UnitType.TIME), MONTH(UnitType.TIME), 
    YEAR(UnitType.TIME), DECADE(UnitType.TIME), CENTURY(UnitType.TIME),

    // DIGITAL STORAGE
    BIT(UnitType.DIGITAL_STORAGE), BYTE(UnitType.DIGITAL_STORAGE), 
    KILOBIT(UnitType.DIGITAL_STORAGE), KILOBYTE(UnitType.DIGITAL_STORAGE),
    MEGABIT(UnitType.DIGITAL_STORAGE), MEGABYTE(UnitType.DIGITAL_STORAGE),
    GIGABIT(UnitType.DIGITAL_STORAGE), GIGABYTE(UnitType.DIGITAL_STORAGE),
    TERABIT(UnitType.DIGITAL_STORAGE), TERABYTE(UnitType.DIGITAL_STORAGE),
    PETABYTE(UnitType.DIGITAL_STORAGE),

    // ENERGY
    JOULE(UnitType.ENERGY), KILOJOULE(UnitType.ENERGY), 
    CALORIE(UnitType.ENERGY), KILOCALORIE(UnitType.ENERGY), FOODCALORIE(UnitType.ENERGY),
    KILOWATT_HOUR(UnitType.ENERGY),

    // POWER
    MILLIWATT(UnitType.POWER), WATT(UnitType.POWER), KILOWATT(UnitType.POWER), 
    MEGAWATT(UnitType.POWER), GIGAWATT(UnitType.POWER), HORSEPOWER(UnitType.POWER),

    // PRESSURE
    HECTOPASCAL(UnitType.PRESSURE), MILLIBAR(UnitType.PRESSURE),
    ATMOSPHERE(UnitType.PRESSURE), POUND_PER_SQUARE_INCH(UnitType.PRESSURE),
    INCH_HG(UnitType.PRESSURE), MILLIMETER_OF_MERCURY(UnitType.PRESSURE),

    // ANGLE
    ARC_SECOND(UnitType.ANGLE), ARC_MINUTE(UnitType.ANGLE), 
    DEGREE(UnitType.ANGLE), RADIAN(UnitType.ANGLE), REVOLUTION_ANGLE(UnitType.ANGLE),

    // FREQUENCY
    HERTZ(UnitType.FREQUENCY), KILOHERTZ(UnitType.FREQUENCY), 
    MEGAHERTZ(UnitType.FREQUENCY), GIGAHERTZ(UnitType.FREQUENCY),

    // ACCELERATION
    METER_PER_SECOND_SQUARED(UnitType.ACCELERATION), G_FORCE(UnitType.ACCELERATION),

    // CURRENCY
    USD(UnitType.CURRENCY), EUR(UnitType.CURRENCY), GBP(UnitType.CURRENCY),
    JPY(UnitType.CURRENCY), CHF(UnitType.CURRENCY), CAD(UnitType.CURRENCY),
    AUD(UnitType.CURRENCY), NZD(UnitType.CURRENCY), CNY(UnitType.CURRENCY),
    INR(UnitType.CURRENCY), KRW(UnitType.CURRENCY), BRL(UnitType.CURRENCY),
    ZAR(UnitType.CURRENCY), MXN(UnitType.CURRENCY), SGD(UnitType.CURRENCY),
    HKD(UnitType.CURRENCY), SEK(UnitType.CURRENCY), NOK(UnitType.CURRENCY),
    DKK(UnitType.CURRENCY), PLN(UnitType.CURRENCY), CZK(UnitType.CURRENCY),
    HUF(UnitType.CURRENCY), RON(UnitType.CURRENCY), BGN(UnitType.CURRENCY),
    TRY(UnitType.CURRENCY), ILS(UnitType.CURRENCY), THB(UnitType.CURRENCY),
    IDR(UnitType.CURRENCY), MYR(UnitType.CURRENCY), PHP(UnitType.CURRENCY),
    ISK(UnitType.CURRENCY);

    /** Returns ISO 4217 currency code or null for non-currency units. */
    val currencyCode: String? get() = if (type == UnitType.CURRENCY) name else null

    /** Returns corresponding MeasureUnit or null for currency units. */
    val measureUnit: MeasureUnit? get() = if (type == UnitType.CURRENCY) null else try {
        MeasureUnit::class.java.getField(name).get(null) as? MeasureUnit
    } catch (e: Exception) { null }

    companion object {
        fun findUnit(text: String, resources: Resources): Unit? {
            val normalizedText = text.lowercase().trim()
            val locale = ULocale.forLocale(resources.configuration.locales[0])
            
            return values().firstOrNull { unit ->
                // Try MeasureUnit matching (non-currency)
                unit.measureUnit?.let { measureUnit ->
                    listOf(
                        MeasureFormat.FormatWidth.SHORT,
                        MeasureFormat.FormatWidth.NARROW,
                        MeasureFormat.FormatWidth.WIDE
                    ).any { width ->
                        val format = MeasureFormat.getInstance(locale, width)
                        val unitName = format.getUnitDisplayName(measureUnit).lowercase()
                        
                        // Try exact match first
                        if (unitName == normalizedText) {
                            return@any true
                        }
                        
                        // Check if unit name appears as a word in the text
                        val regex = "\\b${Regex.escape(unitName)}\\b".toRegex()
                        if (regex.containsMatchIn(normalizedText)) {
                            return@any true
                        }
                        
                        // Also check formatted measures for singular and plural forms
                        val singularForm = format.format(Measure(1.0, measureUnit)).lowercase()
                        val pluralForm = format.format(Measure(2.0, measureUnit)).lowercase()
                        
                        // Extract just the unit part (remove the number)
                        val singularUnit = singularForm.replace(Regex("^[\\d.,\\s]+"), "").trim()
                        val pluralUnit = pluralForm.replace(Regex("^[\\d.,\\s]+"), "").trim()
                        
                        listOf(singularUnit, pluralUnit).any { formattedUnit ->
                            if (formattedUnit.isNotEmpty()) {
                                val formRegex = "\\b${Regex.escape(formattedUnit)}\\b".toRegex()
                                formRegex.containsMatchIn(normalizedText)
                            } else false
                        }
                    }
                } ?: 
                // Try Currency matching
                unit.currencyCode?.let { code ->
                    runCatching {
                        val currency = Currency.getInstance(code)
                        val javaLocale = Locale.forLanguageTag(locale.toLanguageTag())
                        val displayName = currency.getDisplayName(javaLocale).lowercase()
                        val symbol = currency.symbol.lowercase()
                        val currencyCode = currency.currencyCode.lowercase()
                        
                        // Check exact match or word boundary match for display name, symbol, and code
                        val namesToCheck = mutableListOf(displayName, symbol, currencyCode)
                        
                        // Add common plural forms
                        if (displayName.isNotEmpty()) {
                            namesToCheck.add(displayName + "s")  // e.g., "euro" -> "euros"
                        }
                        
                        namesToCheck.any { name ->
                            name == normalizedText || 
                            "\\b${Regex.escape(name)}\\b".toRegex().containsMatchIn(normalizedText)
                        }
                    }.getOrDefault(false)
                } ?: false
            }
        }

        fun convert(value: Double, from: Unit, to: Unit): Double? {
            if (from.type != to.type) return null
            
            // Handle temperature conversions specially (non-linear)
            if (from.type == UnitType.TEMPERATURE) {
                return convertTemperature(value, from, to)
            }
            
            // Handle fuel consumption conversions specially (inverse relationship)
            if (from.type == UnitType.CONSUMPTION) {
                return convertConsumption(value, from, to)
            }
            
            // Get conversion factors to base units
            val fromFactor = getConversionFactor(from) ?: return null
            val toFactor = getConversionFactor(to) ?: return null
            
            // Convert: value * (from factor / to factor)
            return value * fromFactor / toFactor
        }
        
        private fun convertTemperature(value: Double, from: Unit, to: Unit): Double? {
            // Convert to Kelvin first
            val kelvin = when (from) {
                CELSIUS -> value + 273.15
                FAHRENHEIT -> (value - 32.0) * 5.0 / 9.0 + 273.15
                KELVIN -> value
                else -> return null
            }
            
            // Convert from Kelvin to target
            return when (to) {
                CELSIUS -> kelvin - 273.15
                FAHRENHEIT -> (kelvin - 273.15) * 9.0 / 5.0 + 32.0
                KELVIN -> kelvin
                else -> null
            }
        }
        
        private fun getConversionFactor(unit: Unit): Double? = when (unit) {
            // LENGTH - base: METER
            PICOMETER -> 1e-12
            NANOMETER -> 1e-9
            MICROMETER -> 1e-6
            MILLIMETER -> 0.001
            CENTIMETER -> 0.01
            DECIMETER -> 0.1
            METER -> 1.0
            KILOMETER -> 1000.0
            INCH -> 0.0254
            FOOT -> 0.3048
            YARD -> 0.9144
            FATHOM -> 1.8288
            FURLONG -> 201.168
            MILE -> 1609.344
            MILE_SCANDINAVIAN -> 10000.0
            NAUTICAL_MILE -> 1852.0
            LIGHT_YEAR -> 9.4607304725808e15
            ASTRONOMICAL_UNIT -> 1.495978707e11
            PARSEC -> 3.0856776e16
            
            // MASS - base: KILOGRAM
            MICROGRAM -> 1e-9
            MILLIGRAM -> 1e-6
            GRAM -> 0.001
            KILOGRAM -> 1.0
            METRIC_TON -> 1000.0
            TONNE -> 1000.0
            OUNCE -> 0.028349523125
            POUND -> 0.45359237
            STONE -> 6.35029318
            TON -> 907.18474
            OUNCE_TROY -> 0.0311034768
            CARAT -> 0.0002
            
            // TEMPERATURE - special case handled separately
            CELSIUS, FAHRENHEIT, KELVIN -> null
            
            // VOLUME - base: LITER
            MILLILITER -> 0.001
            CENTILITER -> 0.01
            DECILITER -> 0.1
            LITER -> 1.0
            HECTOLITER -> 100.0
            MEGALITER -> 1000000.0
            CUBIC_CENTIMETER -> 0.001
            CUBIC_METER -> 1000.0
            CUBIC_KILOMETER -> 1e12
            CUBIC_INCH -> 0.016387064
            CUBIC_FOOT -> 28.316846592
            CUBIC_YARD -> 764.554857984
            CUBIC_MILE -> 4.16818182544e12
            TEASPOON -> 0.00492892
            TABLESPOON -> 0.0147868
            FLUID_OUNCE -> 0.0295735
            CUP -> 0.2365882365
            CUP_METRIC -> 0.25
            PINT -> 0.473176
            PINT_METRIC -> 0.5
            QUART -> 0.946353
            GALLON -> 3.78541
            GALLON_IMPERIAL -> 4.54609
            BUSHEL -> 35.2391
            ACRE_FOOT -> 1233481.84
            
            // AREA - base: SQUARE_METER
            SQUARE_CENTIMETER -> 0.0001
            SQUARE_METER -> 1.0
            SQUARE_KILOMETER -> 1000000.0
            HECTARE -> 10000.0
            SQUARE_INCH -> 0.00064516
            SQUARE_FOOT -> 0.092903
            SQUARE_YARD -> 0.836127
            ACRE -> 4046.86
            SQUARE_MILE -> 2589988.110336
            
            // SPEED - base: METER_PER_SECOND
            METER_PER_SECOND -> 1.0
            KILOMETER_PER_HOUR -> 0.277778
            MILE_PER_HOUR -> 0.44704
            KNOT -> 0.514444
            
            // TIME - base: SECOND
            NANOSECOND -> 1e-9
            MICROSECOND -> 1e-6
            MILLISECOND -> 0.001
            SECOND -> 1.0
            MINUTE -> 60.0
            HOUR -> 3600.0
            DAY -> 86400.0
            WEEK -> 604800.0
            MONTH -> 2629800.0  // 30.4375 days average
            YEAR -> 31557600.0  // 365.25 days
            DECADE -> 315576000.0
            CENTURY -> 3155760000.0
            
            // DIGITAL STORAGE - base: BYTE
            BIT -> 0.125
            BYTE -> 1.0
            KILOBIT -> 125.0
            KILOBYTE -> 1000.0
            MEGABIT -> 125000.0
            MEGABYTE -> 1000000.0
            GIGABIT -> 125000000.0
            GIGABYTE -> 1000000000.0
            TERABIT -> 125000000000.0
            TERABYTE -> 1000000000000.0
            PETABYTE -> 1000000000000000.0
            
            // ENERGY - base: JOULE
            JOULE -> 1.0
            KILOJOULE -> 1000.0
            CALORIE -> 4.184
            KILOCALORIE -> 4184.0
            FOODCALORIE -> 4184.0
            KILOWATT_HOUR -> 3600000.0
            
            // POWER - base: WATT
            MILLIWATT -> 0.001
            WATT -> 1.0
            KILOWATT -> 1000.0
            MEGAWATT -> 1000000.0
            GIGAWATT -> 1000000000.0
            HORSEPOWER -> 745.7
            
            // PRESSURE - base: PASCAL
            HECTOPASCAL -> 100.0
            MILLIBAR -> 100.0
            ATMOSPHERE -> 101325.0
            POUND_PER_SQUARE_INCH -> 6894.76
            INCH_HG -> 3386.39
            MILLIMETER_OF_MERCURY -> 133.322
            
            // ANGLE - base: RADIAN
            ARC_SECOND -> 4.84814e-6
            ARC_MINUTE -> 0.000290888
            DEGREE -> 0.0174533
            RADIAN -> 1.0
            REVOLUTION_ANGLE -> 6.28319
            
            // FREQUENCY - base: HERTZ
            HERTZ -> 1.0
            KILOHERTZ -> 1000.0
            MEGAHERTZ -> 1000000.0
            GIGAHERTZ -> 1000000000.0
            
            // ACCELERATION - base: METER_PER_SECOND_SQUARED
            METER_PER_SECOND_SQUARED -> 1.0
            G_FORCE -> 9.80665
            
            // CURRENCY - handled separately
            else -> null
        }
    }
}