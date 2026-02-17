package org.stypox.dicio.skills.unit_conversion

import org.dicio.numbers.unit.Number
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.UnitConversion
import org.stypox.dicio.util.ConnectionUtils
import org.json.JSONObject

class UnitConversionSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<UnitConversion>
) : StandardRecognizerSkill<UnitConversion>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: UnitConversion): SkillOutput {
        when (inputData) {
            is UnitConversion.Convert -> {
                // Extract target unit
                val targetUnitText = inputData.targetUnit?.trim()
                if (targetUnitText.isNullOrBlank()) {
                    return UnitConversionOutput.Error(ctx.android.getString(
                        org.stypox.dicio.R.string.skill_unit_conversion_missing_target_unit))
                }
                
                val targetUnit = Unit.findUnit(targetUnitText, ctx.android.resources)
                if (targetUnit == null) {
                    return UnitConversionOutput.Error(ctx.android.getString(
                        org.stypox.dicio.R.string.skill_unit_conversion_unknown_target_unit,
                        targetUnitText))
                }

                // Parse value and source unit from the combined string
                val valueWithUnitText = inputData.valueWithUnit?.trim()
                if (valueWithUnitText.isNullOrBlank()) {
                    return UnitConversionOutput.Error(ctx.android.getString(
                        org.stypox.dicio.R.string.skill_unit_conversion_missing_value_and_source_unit))
                }
                
                // Use number parser to extract the number and remaining text
                val parsed = ctx.parserFormatter?.extractNumber(valueWithUnitText)
                if (parsed == null) {
                    return UnitConversionOutput.Error(ctx.android.getString(
                        org.stypox.dicio.R.string.skill_unit_conversion_could_not_parse_value))
                }
                
                // Find the number in the mixed list
                var value: Double? = null
                val mixedList = parsed.mixedWithText
                for (item in mixedList) {
                    if (item is Number) {
                        value = if (item.isDecimal) {
                            item.decimalValue()
                        } else {
                            item.integerValue().toDouble()
                        }
                        break
                    }
                }
                
                if (value == null) {
                    // Fallback: check if the text starts with "a " or "an " (e.g., "a gallon")
                    val normalized = valueWithUnitText.lowercase().trim()
                    if (normalized.startsWith("a ") || normalized.startsWith("an ")) {
                        value = 1.0
                    } else {
                        return UnitConversionOutput.Error(ctx.android.getString(
                            org.stypox.dicio.R.string.skill_unit_conversion_could_not_parse_number_value))
                    }
                }
                
                // Extract source unit from the remaining text
                val sourceUnit = Unit.findUnit(valueWithUnitText, ctx.android.resources)
                if (sourceUnit == null) {
                    return UnitConversionOutput.Error(ctx.android.getString(
                        org.stypox.dicio.R.string.skill_unit_conversion_could_not_identify_source_unit,
                        valueWithUnitText))
                }

                if (sourceUnit.type != targetUnit.type) {
                    return UnitConversionOutput.Error(
                        ctx.android.getString(
                            org.stypox.dicio.R.string.skill_unit_conversion_cannot_convert_between_types,
                            sourceUnit.type.name.lowercase(),
                            targetUnit.type.name.lowercase())
                    )
                }

                // Perform conversion
                val result = if (sourceUnit.type == UnitType.CURRENCY) {
                    convertCurrency(value, sourceUnit, targetUnit)
                } else {
                    Unit.convert(value, sourceUnit, targetUnit)
                }
                
                if (result == null) {
                    return UnitConversionOutput.Error(ctx.android.getString(
                        org.stypox.dicio.R.string.skill_unit_conversion_conversion_failed))
                }

                return UnitConversionOutput.Success(
                    inputValue = value,
                    sourceUnit = sourceUnit,
                    targetUnit = targetUnit,
                    result = result
                )
            }
        }
    }

    /**
     * Convert currency using the Frankfurter API.
     * API format: https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR
     * Returns the converted amount with 5 decimal precision, or null if the conversion fails.
     */
    private fun convertCurrency(amount: Double, fromCurrency: Unit, toCurrency: Unit): Double? {
        val baseCurrency = fromCurrency.currencyCode ?: return null
        val targetCurrency = toCurrency.currencyCode ?: return null

        return try {
            val apiUrl = "https://api.frankfurter.dev/v1/latest?base=$baseCurrency&symbols=$targetCurrency"
            val exchangeRate = ConnectionUtils.getPageJson(apiUrl)
                .getJSONObject("rates")
                .getDouble(targetCurrency)
            
            val result = amount * exchangeRate
            String.format("%.5f", result).toDouble()
        } catch (_: Exception) {
            null
        }
    }
}
