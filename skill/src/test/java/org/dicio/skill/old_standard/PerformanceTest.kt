package org.dicio.skill.old_standard

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FunSpec
import org.dicio.skill.benchmarkContext
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Specificity
import org.dicio.skill.old_standard_impl.Sentence
import org.dicio.skill.old_standard_impl.StandardRecognizerData
import org.dicio.skill.old_standard_impl.word.CapturingGroup
import org.dicio.skill.old_standard_impl.word.DiacriticsInsensitiveRegexWord
import org.dicio.skill.old_standard_impl.word.DiacriticsInsensitiveWord

@Ignored // only enable to record a benchmark, otherwise it takes too much time
class PerformanceTest : FunSpec({
    benchmarkContext("current_time", scoreFunction(current_time)) {
        warmup("test", "time", "what time", "current time")
        runBenchmarks("time", "what time is it?", "what's the time", "hey I would like to know what time it is")
        runIncrementalBenchmarks()
    }

    benchmarkContext("weather", scoreFunction(weather)) {
        warmup("test", "weather", "cold", "weather in rome")
        runBenchmarks("weather", "what's the weather", "is it cold in budapest", "what is the weather in rome")
        runIncrementalBenchmarks()
    }

    benchmarkContext("timer", scoreFunction(timer)) {
        warmup( "test", "timer", "timer 1s", "named a")
        runBenchmarks("set a timer", "set a timer of 5s", "set a timer named a")
        runIncrementalBenchmarks()
    }
})

fun scoreFunction(data: StandardRecognizerData): (SkillContext, String) -> Unit = { _, input ->
    val inputWords: List<String> = WordExtractor.extractWords(input)
    val normalizedWordKeys: List<String> = WordExtractor.normalizeWords(inputWords)
    data.score(input, inputWords, normalizedWordKeys)
}

// @formatter:off
val current_time = StandardRecognizerData(Specificity.HIGH, Sentence("", intArrayOf(0, 1, 4), DiacriticsInsensitiveWord("what", 5, 5, 6), DiacriticsInsensitiveWord("what", 10, 2, 3), DiacriticsInsensitiveWord("s", 7, 5, 6), DiacriticsInsensitiveWord("is", 9, 5, 6), DiacriticsInsensitiveWord("whats", 11, 5, 6), DiacriticsInsensitiveWord("the", 10, 6), DiacriticsInsensitiveWord("time", 10, 7, 9), DiacriticsInsensitiveWord("is", 9, 8), DiacriticsInsensitiveWord("it", 8, 9)))
class SectionClass_weather internal constructor() : StandardRecognizerData(Specificity.HIGH, Sentence("", intArrayOf(0, 3), DiacriticsInsensitiveWord("what", 21, 1, 2), DiacriticsInsensitiveWord("is", 12, 4), DiacriticsInsensitiveWord("s", 20, 4), DiacriticsInsensitiveWord("whats", 28, 4), DiacriticsInsensitiveWord("the", 27, 5), DiacriticsInsensitiveWord("weather", 26, 6, 7, 8, 10), DiacriticsInsensitiveWord("like", 22, 7, 8, 10), DiacriticsInsensitiveWord("in", 23, 9), DiacriticsInsensitiveWord("on", 25, 9), CapturingGroup("where", 24, 10)), Sentence("", intArrayOf(0), DiacriticsInsensitiveWord("weather", 7, 1, 2, 3, 4), DiacriticsInsensitiveWord("in", 3, 3), DiacriticsInsensitiveWord("on", 5, 3), CapturingGroup("where", 6, 4)), Sentence("", intArrayOf(0), DiacriticsInsensitiveWord("how", 4, 1), DiacriticsInsensitiveWord("is", 3, 2), DiacriticsInsensitiveWord("it", 2, 3), DiacriticsInsensitiveWord("outside", 1, 4)), Sentence("", intArrayOf(0), DiacriticsInsensitiveWord("is", 32, 1), DiacriticsInsensitiveWord("it", 31, 2, 3, 4, 5, 6, 7, 8), DiacriticsInsensitiveWord("cold", 6, 9, 10, 12, 13), DiacriticsInsensitiveWord("cool", 10, 9, 10, 12, 13), DiacriticsInsensitiveWord("warm", 14, 9, 10, 12, 13), DiacriticsInsensitiveWord("hot", 18, 9, 10, 12, 13), DiacriticsInsensitiveWord("sunny", 22, 9, 10, 12, 13), DiacriticsInsensitiveWord("rainy", 26, 9, 10, 12, 13), DiacriticsInsensitiveWord("raining", 30, 9, 10, 12, 13), DiacriticsInsensitiveWord("in", 27, 11), DiacriticsInsensitiveWord("on", 29, 11), CapturingGroup("where", 28, 13), DiacriticsInsensitiveWord("outside", 7, 13))) {
    val where:String = "where"}val weather = SectionClass_weather()
class SectionClass_timer internal constructor() : StandardRecognizerData(Specificity.HIGH, Sentence("set", intArrayOf(0, 1), DiacriticsInsensitiveWord("timer", 3, 4), DiacriticsInsensitiveWord("ping", 7, 2), DiacriticsInsensitiveWord("me", 6, 3), DiacriticsInsensitiveWord("in", 5, 4), CapturingGroup("duration", 4, 5)), Sentence("set", intArrayOf(0, 2, 3, 4), DiacriticsInsensitiveWord("set", 56, 1, 5, 6, 7, 8), DiacriticsInsensitiveWord("up", 32, 5, 6, 7, 8), DiacriticsInsensitiveWord("setup", 80, 5, 6, 7, 8), DiacriticsInsensitiveWord("start", 104, 5, 6, 7, 8), DiacriticsInsensitiveWord("create", 128, 5, 6, 7, 8), DiacriticsInsensitiveWord("a", 116, 6, 7, 8), CapturingGroup("duration", 21, 7), DiacriticsInsensitiveWord("timer", 20, 18), DiacriticsInsensitiveWord("timer", 127, 9, 10), DiacriticsInsensitiveWord("for", 120, 11), DiacriticsInsensitiveWord("of", 126, 11), CapturingGroup("duration", 125, 18, 12, 13, 14), DiacriticsInsensitiveWord("called", 117, 17), DiacriticsInsensitiveWord("named", 119, 17), DiacriticsInsensitiveWord("with", 123, 15), DiacriticsInsensitiveWord("the", 122, 16), DiacriticsInsensitiveWord("name", 121, 17), CapturingGroup("name", 120, 18)), Sentence("cancel", intArrayOf(0, 1, 2, 3, 4), DiacriticsInsensitiveWord("cancel", 17, 5, 6, 7, 8), DiacriticsInsensitiveWord("stop", 29, 5, 6, 7, 8), DiacriticsInsensitiveWord("disable", 41, 5, 6, 7, 8), DiacriticsInsensitiveWord("end", 53, 5, 6, 7, 8), DiacriticsInsensitiveWord("terminate", 65, 5, 6, 7, 8), DiacriticsInsensitiveWord("the", 59, 6, 7, 8), CapturingGroup("name", 21, 7), DiacriticsInsensitiveRegexWord("time(?:r|rs|)", 20, 15), DiacriticsInsensitiveRegexWord("time(?:r|rs|)", 64, 9, 10, 11), DiacriticsInsensitiveWord("called", 57, 14), DiacriticsInsensitiveWord("named", 59, 14), DiacriticsInsensitiveWord("with", 63, 12), DiacriticsInsensitiveWord("the", 62, 13), DiacriticsInsensitiveWord("name", 61, 14), CapturingGroup("name", 60, 15)), Sentence("cancel", intArrayOf(0, 1, 2, 4, 5), DiacriticsInsensitiveWord("silence", 65, 16, 17, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), DiacriticsInsensitiveWord("shut", 126, 3), DiacriticsInsensitiveWord("turn", 186, 3), DiacriticsInsensitiveWord("off", 185, 16, 17, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), DiacriticsInsensitiveWord("quiet", 245, 16, 17, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), DiacriticsInsensitiveWord("mute", 305, 16, 17, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), DiacriticsInsensitiveWord("the", 287, 16, 17, 7, 8, 9, 10, 11, 12, 13, 14, 15), CapturingGroup("name", 21, 8, 9, 10, 11, 12), DiacriticsInsensitiveRegexWord("time(?:r|rs|)", 20, 24), DiacriticsInsensitiveWord("bell", 20, 24), DiacriticsInsensitiveWord("alert", 20, 24), DiacriticsInsensitiveWord("sound", 20, 24), DiacriticsInsensitiveWord("ringtone", 20, 24), DiacriticsInsensitiveRegexWord("time(?:r|rs|)", 292, 18, 19, 20), DiacriticsInsensitiveWord("bell", 298, 18, 19, 20), DiacriticsInsensitiveWord("alert", 304, 18, 19, 20), DiacriticsInsensitiveWord("sound", 262, 18, 19, 20), DiacriticsInsensitiveWord("ringtone", 268, 18, 19, 20), DiacriticsInsensitiveWord("called", 297, 23), DiacriticsInsensitiveWord("named", 299, 23), DiacriticsInsensitiveWord("with", 303, 21), DiacriticsInsensitiveWord("the", 302, 22), DiacriticsInsensitiveWord("name", 301, 23), CapturingGroup("name", 300, 24)), Sentence("query", intArrayOf(0), DiacriticsInsensitiveWord("how", 57, 1, 2), DiacriticsInsensitiveWord("long", 31, 4), DiacriticsInsensitiveWord("much", 56, 3), DiacriticsInsensitiveWord("time", 55, 4), DiacriticsInsensitiveWord("is", 54, 5, 6), DiacriticsInsensitiveWord("left", 42, 6), DiacriticsInsensitiveWord("on", 53, 7, 8, 9, 10), DiacriticsInsensitiveWord("the", 47, 8, 9, 10), CapturingGroup("name", 17, 9), DiacriticsInsensitiveRegexWord("time(?:r|rs|)", 16, 17), DiacriticsInsensitiveRegexWord("time(?:r|rs|)", 52, 11, 12, 13), DiacriticsInsensitiveWord("called", 45, 16), DiacriticsInsensitiveWord("named", 47, 16), DiacriticsInsensitiveWord("with", 51, 14), DiacriticsInsensitiveWord("the", 50, 15), DiacriticsInsensitiveWord("name", 49, 16), CapturingGroup("name", 48, 17)), Sentence("query", intArrayOf(0), DiacriticsInsensitiveWord("when", 49, 1, 2), DiacriticsInsensitiveWord("will", 28, 3, 4, 5, 6), DiacriticsInsensitiveWord("is", 48, 3, 4, 5, 6), DiacriticsInsensitiveWord("the", 38, 4, 5, 6), CapturingGroup("name", 36, 5), DiacriticsInsensitiveRegexWord("time(?:r|rs|)", 36, 13, 15), DiacriticsInsensitiveRegexWord("time(?:r|rs|)", 47, 7, 8, 9), DiacriticsInsensitiveWord("called", 40, 12), DiacriticsInsensitiveWord("named", 42, 12), DiacriticsInsensitiveWord("with", 46, 10), DiacriticsInsensitiveWord("the", 45, 11), DiacriticsInsensitiveWord("name", 44, 12), CapturingGroup("name", 43, 13, 15), DiacriticsInsensitiveWord("going", 41, 14), DiacriticsInsensitiveWord("to", 40, 15), DiacriticsInsensitiveWord("expire", 40, 16))) {
    val duration:String = "duration"
    val name:String = "name"}val timer = SectionClass_timer()
// @formatter:on
