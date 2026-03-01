package org.stypox.dicio.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.stypox.dicio.util.StringUtils.contactStringDistance
import org.stypox.dicio.util.StringUtils.customStringDistance
import org.stypox.dicio.util.StringUtils.joinNonBlank
import org.stypox.dicio.util.StringUtils.levenshteinDistance
import org.stypox.dicio.util.StringUtils.removePunctuation

class StringUtilsTest : StringSpec({
    "joinNonBlank with default parameters" {
        joinNonBlank("a", "b", "c") shouldBe "a • b • c"
        joinNonBlank() shouldBe ""
        joinNonBlank("abc") shouldBe "abc"
        joinNonBlank("", "") shouldBe ""
        joinNonBlank("a", "", "c", null, "", "k", " ", "\t", "d") shouldBe "a • c • k • d"
    }

    "joinNonBlank" {
        joinNonBlank("a", "b", "c", separator = "-") shouldBe "a-b-c"
        joinNonBlank(separator = "-") shouldBe ""
        joinNonBlank("abc", separator = "-") shouldBe "abc"
        joinNonBlank("", "", separator = "-") shouldBe ""
        joinNonBlank("a", "", "c", null, "", "k", " ", "\t", "d", separator = "-") shouldBe "a-c-k-d"
    }

    "remove punctuation" {
        removePunctuation("hello, how are you? ") shouldBe "hello how are you "
        removePunctuation("!\"#1$%&'()*+2,-./:;<=34>?@[5]^_`{|}~") shouldBe "12345"
    }

    "levenshtein distance" {
        levenshteinDistance("kitten", "kitten") shouldBe 0
        levenshteinDistance("kitten", "sitten") shouldBe 1
        levenshteinDistance("kitten", "sittin") shouldBe 2
        levenshteinDistance("kitten", "sitting") shouldBe 3
        levenshteinDistance("dog", "dog") shouldBe 0
        levenshteinDistance("dòg", "caty") shouldBe 4
        levenshteinDistance("dog", "doggy") shouldBe 2
        levenshteinDistance("dog", "dosgy") shouldBe 2
        levenshteinDistance("dog", "dosay") shouldBe 3
        levenshteinDistance("dog", "doag") shouldBe 1
        levenshteinDistance("dog", "dogè") shouldBe 1
    }

    "levenshtein distance with different upper/lower case" {
        levenshteinDistance("Kitten", "siTting") shouldBe 3
        levenshteinDistance("DOG", "dog") shouldBe 0
        levenshteinDistance("cÈd", "CèD") shouldBe 0
    }

    "levenshtein distance with special characters" {
        levenshteinDistance("abc123ABC&%$", "&%\$ABCabc") shouldBe 3
        levenshteinDistance("abc123ABC&%$", "ABC&123%abc.") shouldBe 0
        levenshteinDistance("email@email.email", "EMAIL#atEMAIL#dotEMAIL") shouldBe 5
        levenshteinDistance("Hello, hòw are you?", "hellohoware!you") shouldBe 0
    }

    "custom string distance" {
        customStringDistance("kitten", "kitten") shouldBe -12
        customStringDistance("kitten", "sitten") shouldBe -9
        customStringDistance("kitten", "sittin") shouldBe -5
        customStringDistance("kitten", "sitlen") shouldBe -5
        customStringDistance("kitten", "sitting") shouldBe -4
        customStringDistance("kitten", "sittieng") shouldBe -6
        customStringDistance("dog", "dog") shouldBe -6
        customStringDistance("dòg", "caty") shouldBe 4
        customStringDistance("dog", "dosgy") shouldBe -3
        customStringDistance("dog", "dosay") shouldBe -1
        customStringDistance("dog", "doag") shouldBe -4
        customStringDistance("dog", "dogè") shouldBe -5
    }

    "custom string distance with different upper/lower case" {
        customStringDistance("Kitten", "siTting") shouldBe -4
        customStringDistance("DOG", "dog") shouldBe -6
        customStringDistance("cÈd", "CèD") shouldBe -6
    }

    "custom string distance with special characters" {
        customStringDistance("abc123ABC&%$", "&%\$ABCabc") shouldBe -6
        customStringDistance("abc123ABC&%$", "ABC&123%abc.") shouldBe -18
        customStringDistance("email@email.email", "EMAIL#atEMAIL#dotEMAIL") shouldBe -20
        customStringDistance("Hello, hòw are you?", "hellohoware!you") shouldBe -28
    }

    "custom string distance with repeated letters" {
        customStringDistance("hello", "Helloo Hiii") shouldBe -5
        customStringDistance("hello", "Hello Guys") shouldBe -6
        customStringDistance("hello", "Hello 2uys") shouldBe -6
        customStringDistance("dog", "dogy") shouldBe -5
        customStringDistance("dog", "doggy") shouldBe -4
    }

    "contact string distance" {
        contactStringDistance("Leo Morgan", "Morgan") shouldBe -12
        contactStringDistance("John Morgan", "Morgan") shouldBe -12
        contactStringDistance("Johan Morgan", "John") shouldBe -7
        contactStringDistance("Leonard John", "Morgan") shouldBe -4
    }
})
