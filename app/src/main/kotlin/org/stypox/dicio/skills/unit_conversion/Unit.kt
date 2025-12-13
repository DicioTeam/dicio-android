package org.stypox.dicio.skills.unit_conversion

import android.content.res.Resources
import org.stypox.dicio.R

enum class UnitType {
    LENGTH,
    MASS,
    TEMPERATURE,
    VOLUME,
    AREA,
    SPEED,
    TIME,
    DIGITAL_STORAGE,
    ENERGY,
    POWER,
    PRESSURE,
    ANGLE
}

enum class Unit(
    val type: UnitType,
    val singularNamesResId: Int,  // Resource ID for string-array of singular localized names
    val pluralNamesResId: Int,    // Resource ID for string-array of plural localized names
    val abbreviations: List<String>,  // Keep abbreviations hardcoded (mostly international)
    val toBaseUnit: (Double) -> Double,
    val fromBaseUnit: (Double) -> Double
) {
    // LENGTH (base: meter)
    MILLIMETER(UnitType.LENGTH, R.array.unit_millimeter_singular, R.array.unit_millimeter_plural, listOf("mm"),
        { it / 1000.0 }, { it * 1000.0 }),
    CENTIMETER(UnitType.LENGTH, R.array.unit_centimeter_singular, R.array.unit_centimeter_plural, listOf("cm"),
        { it / 100.0 }, { it * 100.0 }),
    METER(UnitType.LENGTH, R.array.unit_meter_singular, R.array.unit_meter_plural, listOf("m"),
        { it }, { it }),
    KILOMETER(UnitType.LENGTH, R.array.unit_kilometer_singular, R.array.unit_kilometer_plural, listOf("km"),
        { it * 1000.0 }, { it / 1000.0 }),
    INCH(UnitType.LENGTH, R.array.unit_inch_singular, R.array.unit_inch_plural, listOf("in"),
        { it * 0.0254 }, { it / 0.0254 }),
    FOOT(UnitType.LENGTH, R.array.unit_foot_singular, R.array.unit_foot_plural, listOf("ft"),
        { it * 0.3048 }, { it / 0.3048 }),
    YARD(UnitType.LENGTH, R.array.unit_yard_singular, R.array.unit_yard_plural, listOf("yd"),
        { it * 0.9144 }, { it / 0.9144 }),
    MILE(UnitType.LENGTH, R.array.unit_mile_singular, R.array.unit_mile_plural, listOf("mi"),
        { it * 1609.344 }, { it / 1609.344 }),
    NAUTICAL_MILE(UnitType.LENGTH, R.array.unit_nautical_mile_singular, R.array.unit_nautical_mile_plural, listOf("nmi", "nm"),
        { it * 1852.0 }, { it / 1852.0 }),
    LIGHT_YEAR(UnitType.LENGTH, R.array.unit_light_year_singular, R.array.unit_light_year_plural, listOf("ly"),
        { it * 9.460730472580800e15 }, { it / 9.460730472580800e15 }),
    ASTRONOMICAL_UNIT(UnitType.LENGTH, R.array.unit_astronomical_unit_singular, R.array.unit_astronomical_unit_plural, listOf("au", "ua"),
        { it * 1.495978707e11 }, { it / 1.495978707e11 }),

    // MASS (base: kilogram)
    MILLIGRAM(UnitType.MASS, R.array.unit_milligram_singular, R.array.unit_milligram_plural, listOf("mg"),
        { it / 1000000.0 }, { it * 1000000.0 }),
    GRAM(UnitType.MASS, R.array.unit_gram_singular, R.array.unit_gram_plural, listOf("g"),
        { it / 1000.0 }, { it * 1000.0 }),
    KILOGRAM(UnitType.MASS, R.array.unit_kilogram_singular, R.array.unit_kilogram_plural, listOf("kg"),
        { it }, { it }),
    METRIC_TON(UnitType.MASS, R.array.unit_metric_ton_singular, R.array.unit_metric_ton_plural, listOf("t"),
        { it * 1000.0 }, { it / 1000.0 }),
    OUNCE(UnitType.MASS, R.array.unit_ounce_singular, R.array.unit_ounce_plural, listOf("oz"),
        { it * 0.028349523125 }, { it / 0.028349523125 }),
    POUND(UnitType.MASS, R.array.unit_pound_singular, R.array.unit_pound_plural, listOf("lb", "lbs"),
        { it * 0.45359237 }, { it / 0.45359237 }),
    STONE(UnitType.MASS, R.array.unit_stone_singular, R.array.unit_stone_plural, listOf("st"),
        { it * 6.35029318 }, { it / 6.35029318 }),
    TON_US(UnitType.MASS, R.array.unit_ton_us_singular, R.array.unit_ton_us_plural, listOf("ton"),
        { it * 907.18474 }, { it / 907.18474 }),
    TROY_OUNCE(UnitType.MASS, R.array.unit_troy_ounce_singular, R.array.unit_troy_ounce_plural, listOf("oz t", "ozt"),
        { it * 0.0311034768 }, { it / 0.0311034768 }),

    // TEMPERATURE (base: Celsius)
    CELSIUS(UnitType.TEMPERATURE, R.array.unit_celsius_singular, R.array.unit_celsius_plural, listOf("c", "°c"),
        { it }, { it }),
    FAHRENHEIT(UnitType.TEMPERATURE, R.array.unit_fahrenheit_singular, R.array.unit_fahrenheit_plural, listOf("f", "°f"),
        { (it - 32.0) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32.0 }),
    KELVIN(UnitType.TEMPERATURE, R.array.unit_kelvin_singular, R.array.unit_kelvin_plural, listOf("k"),
        { it - 273.15 }, { it + 273.15 }),

    // VOLUME (base: liter)
    MILLILITER(UnitType.VOLUME, R.array.unit_milliliter_singular, R.array.unit_milliliter_plural, listOf("ml"),
        { it / 1000.0 }, { it * 1000.0 }),
    LITER(UnitType.VOLUME, R.array.unit_liter_singular, R.array.unit_liter_plural, listOf("l"),
        { it }, { it }),
    CUBIC_METER(UnitType.VOLUME, R.array.unit_cubic_meter_singular, R.array.unit_cubic_meter_plural, listOf("m3", "m³"),
        { it * 1000.0 }, { it / 1000.0 }),
    TEASPOON(UnitType.VOLUME, R.array.unit_teaspoon_singular, R.array.unit_teaspoon_plural, listOf("tsp"),
        { it * 0.00492892 }, { it / 0.00492892 }),
    TABLESPOON(UnitType.VOLUME, R.array.unit_tablespoon_singular, R.array.unit_tablespoon_plural, listOf("tbsp"),
        { it * 0.01478676 }, { it / 0.01478676 }),
    FLUID_OUNCE(UnitType.VOLUME, R.array.unit_fluid_ounce_singular, R.array.unit_fluid_ounce_plural, listOf("fl oz"),
        { it * 0.02957353 }, { it / 0.02957353 }),
    CUP(UnitType.VOLUME, R.array.unit_cup_singular, R.array.unit_cup_plural, listOf("cup"),
        { it * 0.23658824 }, { it / 0.23658824 }),
    PINT(UnitType.VOLUME, R.array.unit_pint_singular, R.array.unit_pint_plural, listOf("pt"),
        { it * 0.47317647 }, { it / 0.47317647 }),
    QUART(UnitType.VOLUME, R.array.unit_quart_singular, R.array.unit_quart_plural, listOf("qt"),
        { it * 0.94635295 }, { it / 0.94635295 }),
    GALLON(UnitType.VOLUME, R.array.unit_gallon_singular, R.array.unit_gallon_plural, listOf("gal"),
        { it * 3.78541178 }, { it / 3.78541178 }),

    // AREA (base: square meter)
    SQUARE_METER(UnitType.AREA, R.array.unit_square_meter_singular, R.array.unit_square_meter_plural, listOf("m2", "m²", "sq m"),
        { it }, { it }),
    SQUARE_KILOMETER(UnitType.AREA, R.array.unit_square_kilometer_singular, R.array.unit_square_kilometer_plural, listOf("km2", "km²", "sq km"),
        { it * 1000000.0 }, { it / 1000000.0 }),
    HECTARE(UnitType.AREA, R.array.unit_hectare_singular, R.array.unit_hectare_plural, listOf("ha"),
        { it * 10000.0 }, { it / 10000.0 }),
    SQUARE_FOOT(UnitType.AREA, R.array.unit_square_foot_singular, R.array.unit_square_foot_plural, listOf("ft2", "ft²", "sq ft"),
        { it * 0.09290304 }, { it / 0.09290304 }),
    SQUARE_YARD(UnitType.AREA, R.array.unit_square_yard_singular, R.array.unit_square_yard_plural, listOf("yd2", "yd²", "sq yd"),
        { it * 0.83612736 }, { it / 0.83612736 }),
    ACRE(UnitType.AREA, R.array.unit_acre_singular, R.array.unit_acre_plural, listOf("ac"),
        { it * 4046.8564224 }, { it / 4046.8564224 }),
    SQUARE_MILE(UnitType.AREA, R.array.unit_square_mile_singular, R.array.unit_square_mile_plural, listOf("mi2", "mi²", "sq mi"),
        { it * 2589988.110336 }, { it / 2589988.110336 }),

    // SPEED (base: meters per second)
    METERS_PER_SECOND(UnitType.SPEED, R.array.unit_meters_per_second_singular, R.array.unit_meters_per_second_plural, listOf("m/s", "mps"),
        { it }, { it }),
    KILOMETERS_PER_HOUR(UnitType.SPEED, R.array.unit_kilometers_per_hour_singular, R.array.unit_kilometers_per_hour_plural, listOf("km/h", "kmph", "kph"),
        { it / 3.6 }, { it * 3.6 }),
    MILES_PER_HOUR(UnitType.SPEED, R.array.unit_miles_per_hour_singular, R.array.unit_miles_per_hour_plural, listOf("mph", "mi/h"),
        { it * 0.44704 }, { it / 0.44704 }),
    FEET_PER_SECOND(UnitType.SPEED, R.array.unit_feet_per_second_singular, R.array.unit_feet_per_second_plural, listOf("ft/s", "fps"),
        { it * 0.3048 }, { it / 0.3048 }),
    KNOT(UnitType.SPEED, R.array.unit_knot_singular, R.array.unit_knot_plural, listOf("kn", "kt"),
        { it * 0.514444 }, { it / 0.514444 }),

    // TIME (base: second)
    MILLISECOND(UnitType.TIME, R.array.unit_millisecond_singular, R.array.unit_millisecond_plural, listOf("ms"),
        { it / 1000.0 }, { it * 1000.0 }),
    SECOND(UnitType.TIME, R.array.unit_second_singular, R.array.unit_second_plural, listOf("s", "sec"),
        { it }, { it }),
    MINUTE(UnitType.TIME, R.array.unit_minute_singular, R.array.unit_minute_plural, listOf("min"),
        { it * 60.0 }, { it / 60.0 }),
    HOUR(UnitType.TIME, R.array.unit_hour_singular, R.array.unit_hour_plural, listOf("h", "hr"),
        { it * 3600.0 }, { it / 3600.0 }),
    DAY(UnitType.TIME, R.array.unit_day_singular, R.array.unit_day_plural, listOf("d"),
        { it * 86400.0 }, { it / 86400.0 }),
    WEEK(UnitType.TIME, R.array.unit_week_singular, R.array.unit_week_plural, listOf("wk"),
        { it * 604800.0 }, { it / 604800.0 }),
    MONTH(UnitType.TIME, R.array.unit_month_singular, R.array.unit_month_plural, listOf("mo"),
        { it * 2629800.0 }, { it / 2629800.0 }), // average month (30.4375 days)
    YEAR(UnitType.TIME, R.array.unit_year_singular, R.array.unit_year_plural, listOf("y", "yr"),
        { it * 31557600.0 }, { it / 31557600.0 }), // average year (365.25 days)

    // DIGITAL STORAGE (base: byte)
    BIT(UnitType.DIGITAL_STORAGE, R.array.unit_bit_singular, R.array.unit_bit_plural, listOf("b"),
        { it / 8.0 }, { it * 8.0 }),
    BYTE(UnitType.DIGITAL_STORAGE, R.array.unit_byte_singular, R.array.unit_byte_plural, listOf("B"),
        { it }, { it }),
    KILOBYTE(UnitType.DIGITAL_STORAGE, R.array.unit_kilobyte_singular, R.array.unit_kilobyte_plural, listOf("kB", "kb"),
        { it * 1000.0 }, { it / 1000.0 }),
    MEGABYTE(UnitType.DIGITAL_STORAGE, R.array.unit_megabyte_singular, R.array.unit_megabyte_plural, listOf("MB", "mb"),
        { it * 1000000.0 }, { it / 1000000.0 }),
    GIGABYTE(UnitType.DIGITAL_STORAGE, R.array.unit_gigabyte_singular, R.array.unit_gigabyte_plural, listOf("GB", "gb"),
        { it * 1000000000.0 }, { it / 1000000000.0 }),
    TERABYTE(UnitType.DIGITAL_STORAGE, R.array.unit_terabyte_singular, R.array.unit_terabyte_plural, listOf("TB", "tb"),
        { it * 1000000000000.0 }, { it / 1000000000000.0 }),
    PETABYTE(UnitType.DIGITAL_STORAGE, R.array.unit_petabyte_singular, R.array.unit_petabyte_plural, listOf("PB", "pb"),
        { it * 1000000000000000.0 }, { it / 1000000000000000.0 }),
    KIBIBYTE(UnitType.DIGITAL_STORAGE, R.array.unit_kibibyte_singular, R.array.unit_kibibyte_plural, listOf("KiB", "kib"),
        { it * 1024.0 }, { it / 1024.0 }),
    MEBIBYTE(UnitType.DIGITAL_STORAGE, R.array.unit_mebibyte_singular, R.array.unit_mebibyte_plural, listOf("MiB", "mib"),
        { it * 1048576.0 }, { it / 1048576.0 }),
    GIBIBYTE(UnitType.DIGITAL_STORAGE, R.array.unit_gibibyte_singular, R.array.unit_gibibyte_plural, listOf("GiB", "gib"),
        { it * 1073741824.0 }, { it / 1073741824.0 }),
    TEBIBYTE(UnitType.DIGITAL_STORAGE, R.array.unit_tebibyte_singular, R.array.unit_tebibyte_plural, listOf("TiB", "tib"),
        { it * 1099511627776.0 }, { it / 1099511627776.0 }),

    // ENERGY (base: joule)
    JOULE(UnitType.ENERGY, R.array.unit_joule_singular, R.array.unit_joule_plural, listOf("j", "J"),
        { it }, { it }),
    KILOJOULE(UnitType.ENERGY, R.array.unit_kilojoule_singular, R.array.unit_kilojoule_plural, listOf("kj", "kJ"),
        { it * 1000.0 }, { it / 1000.0 }),
    CALORIE(UnitType.ENERGY, R.array.unit_calorie_singular, R.array.unit_calorie_plural, listOf("cal"),
        { it * 4.184 }, { it / 4.184 }),
    KILOCALORIE(UnitType.ENERGY, R.array.unit_kilocalorie_singular, R.array.unit_kilocalorie_plural, listOf("kcal", "Cal"),
        { it * 4184.0 }, { it / 4184.0 }),
    WATT_HOUR(UnitType.ENERGY, R.array.unit_watt_hour_singular, R.array.unit_watt_hour_plural, listOf("wh", "Wh"),
        { it * 3600.0 }, { it / 3600.0 }),
    KILOWATT_HOUR(UnitType.ENERGY, R.array.unit_kilowatt_hour_singular, R.array.unit_kilowatt_hour_plural, listOf("kwh", "kWh"),
        { it * 3600000.0 }, { it / 3600000.0 }),
    ELECTRONVOLT(UnitType.ENERGY, R.array.unit_electronvolt_singular, R.array.unit_electronvolt_plural, listOf("ev", "eV"),
        { it * 1.602176634e-19 }, { it / 1.602176634e-19 }),

    // POWER (base: watt)
    WATT(UnitType.POWER, R.array.unit_watt_singular, R.array.unit_watt_plural, listOf("W", "w"),
        { it }, { it }),
    KILOWATT(UnitType.POWER, R.array.unit_kilowatt_singular, R.array.unit_kilowatt_plural, listOf("kW", "kw"),
        { it * 1000.0 }, { it / 1000.0 }),
    MEGAWATT(UnitType.POWER, R.array.unit_megawatt_singular, R.array.unit_megawatt_plural, listOf("MW", "mw"),
        { it * 1000000.0 }, { it / 1000000.0 }),
    HORSEPOWER(UnitType.POWER, R.array.unit_horsepower_singular, R.array.unit_horsepower_plural, listOf("hp"),
        { it * 745.699872 }, { it / 745.699872 }), // This might be a little bit contentious. This is mechanical/imperial horsepower.

    // PRESSURE (base: pascal)
    PASCAL(UnitType.PRESSURE, R.array.unit_pascal_singular, R.array.unit_pascal_plural, listOf("pa", "Pa"),
        { it }, { it }),
    KILOPASCAL(UnitType.PRESSURE, R.array.unit_kilopascal_singular, R.array.unit_kilopascal_plural, listOf("kpa", "kPa"),
        { it * 1000.0 }, { it / 1000.0 }),
    BAR(UnitType.PRESSURE, R.array.unit_bar_singular, R.array.unit_bar_plural, listOf("bar"),
        { it * 100000.0 }, { it / 100000.0 }),
    ATMOSPHERE(UnitType.PRESSURE, R.array.unit_atmosphere_singular, R.array.unit_atmosphere_plural, listOf("atm"),
        { it * 101325.0 }, { it / 101325.0 }),
    PSI(UnitType.PRESSURE, R.array.unit_psi_singular, R.array.unit_psi_plural, listOf("psi"),
        { it * 6894.75729 }, { it / 6894.75729 }),
    TORR(UnitType.PRESSURE, R.array.unit_torr_singular, R.array.unit_torr_plural, listOf("torr"),
        { it * 133.322368 }, { it / 133.322368 }),
    MMHG(UnitType.PRESSURE, R.array.unit_mmhg_singular, R.array.unit_mmhg_plural, listOf("mmhg", "mmHg"),
        { it * 133.322 }, { it / 133.322 }),

    // ANGLE (base: degree)
    DEGREE(UnitType.ANGLE, R.array.unit_degree_singular, R.array.unit_degree_plural, listOf("deg", "°"),
        { it }, { it }),
    RADIAN(UnitType.ANGLE, R.array.unit_radian_singular, R.array.unit_radian_plural, listOf("rad"),
        { it * 180.0 / Math.PI }, { it * Math.PI / 180.0 }),
    GRADIAN(UnitType.ANGLE, R.array.unit_gradian_singular, R.array.unit_gradian_plural, listOf("grad"),
        { it * 0.9 }, { it / 0.9 });

    companion object {
        fun findUnit(text: String, resources: Resources): Unit? {
            val normalizedText = text.lowercase().trim()
            
            // First try exact match with abbreviations (case-sensitive for some units like B vs b)
            for (unit in values()) {
                if (unit.abbreviations.contains(text.trim())) {
                    return unit
                }
            }
            
            // Then try case-insensitive abbreviations
            for (unit in values()) {
                for (abbr in unit.abbreviations) {
                    if (abbr.equals(normalizedText, ignoreCase = true)) {
                        return unit
                    }
                }
            }
            
            // Finally try localized full names from resources (both singular and plural)
            for (unit in values()) {
                try {
                    // Check singular names
                    val singularNames = resources.getStringArray(unit.singularNamesResId)
                    for (name in singularNames) {
                        if (name.lowercase() == normalizedText) {
                            return unit
                        }
                    }
                    
                    // Check plural names
                    val pluralNames = resources.getStringArray(unit.pluralNamesResId)
                    for (name in pluralNames) {
                        if (name.lowercase() == normalizedText) {
                            return unit
                        }
                    }
                } catch (e: Exception) {
                    // Resource not found, skip this unit
                    continue
                }
            }
            
            return null
        }

        fun convert(value: Double, from: Unit, to: Unit): Double? {
            if (from.type != to.type) {
                return null // Cannot convert between different types
            }
            val baseValue = from.toBaseUnit(value)
            return to.fromBaseUnit(baseValue)
        }
    }
}