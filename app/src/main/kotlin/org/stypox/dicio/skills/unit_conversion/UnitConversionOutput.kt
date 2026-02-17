package org.stypox.dicio.skills.unit_conversion

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.ULocale
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.io.graphical.Subtitle
import org.stypox.dicio.util.getString
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

sealed interface UnitConversionOutput : SkillOutput {
    data class Success(
        val inputValue: Double,
        val sourceUnit: Unit,
        val targetUnit: Unit,
        val result: Double
    ) : UnitConversionOutput {
        
        private fun formatNumber(value: Double): String {
            // Use appropriate precision based on magnitude
            val symbols = DecimalFormatSymbols().apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            
            return when {
                value == 0.0 -> "0"
                Math.abs(value) >= 1000000 -> {
                    DecimalFormat("#,##0.##E0", symbols).format(value)
                }
                Math.abs(value) >= 1 -> {
                    DecimalFormat("#,##0.####", symbols).format(value)
                }
                Math.abs(value) >= 0.01 -> {
                    DecimalFormat("0.####", symbols).format(value)
                }
                else -> {
                    DecimalFormat("0.######E0", symbols).format(value)
                }
            }
        }

        private fun getUnitDisplayName(unit: Unit, resources: android.content.res.Resources): String {
            val locale = ULocale.forLocale(resources.configuration.locales[0])
            
            // Use MeasureFormat for non-currency units
            unit.measureUnit?.let { measureUnit ->
                val formatWidth = when (unit.type) {
                    UnitType.DIGITAL_STORAGE, UnitType.ENERGY, UnitType.POWER, UnitType.PRESSURE -> {
                        MeasureFormat.FormatWidth.NARROW
                    }
                    else -> {
                        MeasureFormat.FormatWidth.WIDE
                    }
                }
                return MeasureFormat.getInstance(locale, formatWidth).getUnitDisplayName(measureUnit)
            }
            
            // Use Currency API for currency units
            unit.currencyCode?.let { code ->
                return runCatching {
                    val javaLocale = Locale.forLanguageTag(locale.toLanguageTag())
                    Currency.getInstance(code).getDisplayName(javaLocale)
                }.getOrDefault(code)
            }
            
            return ""
        }

        override fun getSpeechOutput(ctx: SkillContext): String {
            val inputStr = formatNumber(inputValue)
            val resultStr = formatNumber(result)
            val sourceUnitName = getUnitDisplayName(sourceUnit, ctx.android.resources)
            val targetUnitName = getUnitDisplayName(targetUnit, ctx.android.resources)
            
            return ctx.getString(
                R.string.skill_unit_conversion_result,
                inputStr,
                sourceUnitName,
                resultStr,
                targetUnitName
            )
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column {
                Subtitle(
                    text = "${formatNumber(inputValue)} ${getUnitDisplayName(sourceUnit, ctx.android.resources)}"
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
Headline(
                    text = "${formatNumber(result)} ${getUnitDisplayName(targetUnit, ctx.android.resources)}"
                )
            }
        }
    }

    data class Error(
        val message: String
    ) : UnitConversionOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            return ctx.getString(R.string.skill_unit_conversion_error, message)
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Headline(text = getSpeechOutput(ctx))
        }
    }
}
