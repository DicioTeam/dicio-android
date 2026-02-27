package org.stypox.dicio.skills.calculator

import net.objecthunter.exp4j.ExpressionBuilder
import org.dicio.numbers.unit.Number
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Calculator
import org.stypox.dicio.sentences.Sentences.CalculatorOperators
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class CalculatorSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Calculator>)
    : StandardRecognizerSkill<Calculator>(correspondingSkillInfo, data) {

    private fun getOperation(
        ctx: SkillContext,
        operatorSection: StandardRecognizerData<CalculatorOperators>,
        text: String
    ): CalculatorOperators? {
        val (score, result) = operatorSection.score(ctx, text)
        return if (score.scoreIn01Range() < 0.3) {
            null
        } else {
            result
        }
    }

    private fun numberToString(decimalFormat: DecimalFormat, number: Number): String {
        return if (number.isDecimal) {
            decimalFormat.format(number.decimalValue())
        } else {
            number.integerValue().toString()
        }
    }

    override suspend fun generateOutput(ctx: SkillContext, inputData: Calculator): SkillOutput {
        val textWithNumbers: List<Any>? = when (inputData) {
            is Calculator.Calculate -> inputData.calculation
        }?.let { ctx.parserFormatter?.extractNumber(it)?.parseMixedWithText() }
        if (textWithNumbers.isNullOrEmpty()
            || (textWithNumbers.size == 1 && textWithNumbers[0] !is Number)) {
            return CalculatorOutput(null, "", "")
        }

        val operatorRecognizerData = CalculatorOperators[ctx.sentencesLanguage]!!
        var firstNumber: Number
        var i: Int
        if (textWithNumbers[0] is Number) {
            firstNumber = textWithNumbers[0] as Number
            i = 1
        } else {
            firstNumber = textWithNumbers[1] as Number
            if (getOperation(ctx, operatorRecognizerData, textWithNumbers[0] as String)
                == CalculatorOperators.Subtraction
            ) {
                firstNumber = firstNumber.multiply(-1)
            }
            i = 2
        }

        val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(ctx.locale))
        val inputInterpretation = StringBuilder(numberToString(decimalFormat, firstNumber))

        var currentVariableNumber = 0
        val variables: MutableMap<String, Double> = HashMap()
        variables["_0"] = if (firstNumber.isDecimal)
            firstNumber.decimalValue()
        else
            firstNumber.integerValue().toDouble()

        val expressionString = StringBuilder()
        expressionString.append("_").append(currentVariableNumber)
        ++currentVariableNumber
        while (i < textWithNumbers.size) {
            val operation: CalculatorOperators
            if (textWithNumbers[i] is Number) {
                operation = CalculatorOperators.Addition
            } else if (i + 1 < textWithNumbers.size) {
                operation = getOperation(ctx, operatorRecognizerData, textWithNumbers[i] as String)
                    ?: CalculatorOperators.Addition // perform addition by default
                ++i
            } else {
                break
            }
            var number = textWithNumbers[i] as Number
            ++i
            when (operation) {
                CalculatorOperators.Addition -> {
                    val op = if (number.lessThan(0)) "-" else "+"
                    inputInterpretation.append(" $op ")
                    expressionString.append(op)
                    number = if (number.lessThan(0)) number.multiply(-1) else number
                }
                CalculatorOperators.Subtraction -> {
                    inputInterpretation.append(" - ")
                    expressionString.append("-")
                }
                CalculatorOperators.Multiplication -> {
                    inputInterpretation.append(" x ")
                    expressionString.append("*")
                }
                CalculatorOperators.Division -> {
                    inputInterpretation.append(" ÷ ")
                    expressionString.append("/")
                }
                CalculatorOperators.Power -> {
                    inputInterpretation.append(" ^ ")
                    expressionString.append("^")
                }
                CalculatorOperators.SquareRoot -> {
                    // TODO unimplemented, this way of building expressions doesn't allow for unary
                    //  operations
                    inputInterpretation.append(" + ")
                    expressionString.append("+")
                }
            }

            variables["_$currentVariableNumber"] = if (number.isDecimal)
                number.decimalValue()
            else
                number.integerValue().toDouble()

            expressionString.append("_").append(currentVariableNumber)
            ++currentVariableNumber
            inputInterpretation.append(numberToString(decimalFormat, number))
        }
        inputInterpretation.append(" =")

        val result = ExpressionBuilder(expressionString.toString())
            .variables(variables.keys)
            .build()
            .setVariables(variables)
            .evaluate()

        return CalculatorOutput(
            result = numberToString(decimalFormat, Number(result)),
            spokenResult = ctx.parserFormatter!!
                .niceNumber(result)
                .get(),
            inputInterpretation = inputInterpretation.toString(),
        )
    }
}
