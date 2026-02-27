package org.dicio.skill.standard.capture

/**
 * A base class that holds data that was captured while matching.
 */
sealed interface Capture {
    /**
     * Identifies the capturing group.
     */
    val name: String

    /**
     * The index of the first character in the original string that is contained in this capturing
     * group. [[start], [end]) is inclusive-exclusive.
     */
    val start: Int

    /**
     * The index of one past the last character in the original string that is contained in this
     * capturing group. [[start], [end]) is inclusive-exclusive.
     */
    val end: Int // exclusive
}
