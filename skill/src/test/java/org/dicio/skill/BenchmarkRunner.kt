package org.dicio.skill

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.measureTime
import org.dicio.skill.context.SkillContext
import org.dicio.skill.standard.util.MatchHelper

const val saveFolder = "benchmarks/999_current"

fun FunSpec.benchmarkContext(
    name: String,
    scoreFunction: (SkillContext, String) -> Unit,
    f: suspend BenchmarkRunner.() -> Unit
) {
    context(name) {
        val runner = BenchmarkRunner(this, name) { skillContext, input ->
            skillContext.standardMatchHelper = MatchHelper(null, input)
            scoreFunction(skillContext, input)
        }
        f(runner)
        runner.saveJson()
    }
}

class BenchmarkRunner(
    private val funSpec: FunSpecContainerScope,
    private val name: String,
    private val scoreFunction: (MockSkillContext, String) -> Unit,
) {
    private var incrementalJsonData = "[]"
    private var benchmarksJsonData = ArrayList<String>()
    private val skillContext = MockSkillContext()

    fun warmup(vararg inputs: String) {
        for (input in inputs) {
            scoreFunction(skillContext, input)
        }
    }

    suspend fun runBenchmarks(vararg inputs: String) {
        for (input in inputs) {
            funSpec.test("INPUT = $input") {
                val time = getBenchmarkTime(input)
                println("[$name] [$input] $time")
                benchmarksJsonData.add("""{"input": "${
                    input.replace("\\", "\\\\").replace("\"", "\\\"")
                }", "time": ${
                    time.inWholeNanoseconds
                }}""")
            }
        }
    }

    suspend fun runIncrementalBenchmarks() {
        funSpec.test("INCREMENTAL") {
            val maxDuration = 1.seconds
            val wantedPoints = 20
            val results = ArrayList<Pair<Int, Duration>>()
            val input = StringBuilder()
            results.add(Pair(0, measureTime { scoreFunction(skillContext, "") }))

            var skipUntilSize = 1
            // limit increment initially, since at small values the slope is not reliable
            var maxIncrement = 1.0f
            while (results.last().second < maxDuration) {
                input.append(if (input.length % 4 == 3) " " else "a")
                if (input.length >= skipUntilSize) {
                    val currSize = input.length
                    val currTime = try {
                        val inputToString = input.toString()
                        measureTime { scoreFunction(skillContext, inputToString) }
                    } catch (e: StackOverflowError) {
                        break
                    }
                    results.add(Pair(currSize, currTime))

                    val dx = currSize.toFloat()
                    val dy = currTime.inWholeNanoseconds * 1e-9f
                    val rawIncrement = dx / dy *
                            (maxDuration - currTime).inWholeNanoseconds * 1e-9f /
                            max(1, wantedPoints - results.size)
                    skipUntilSize += max(1f, min(maxIncrement, rawIncrement)).toInt()
                    maxIncrement *= 10f
                }
            }

            incrementalJsonData = "[${
                results.joinToString { (size, time) ->
                    """{"size": $size, "time": ${time.inWholeNanoseconds}}"""
                }
            }]"
        }
    }

    private fun getBenchmarkTime(input: String): Duration {
        val timeSource = TimeSource.Monotonic

        // benchmark phase
        val startMark = timeSource.markNow()
        val endMark = startMark.plus(2.seconds)
        var times = 0
        while (endMark.hasNotPassedNow()) {
            scoreFunction(skillContext, input)
            times += 1
        }

        return startMark.elapsedNow() / times
    }

    fun saveJson() {
        Files.createDirectories(Path(saveFolder))
        File("$saveFolder/$name.json")
            .writeText("""{"incremental": $incrementalJsonData, "benchmarks": [${
                benchmarksJsonData.joinToString()
            }]}""")
    }
}
