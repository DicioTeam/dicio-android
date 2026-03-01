package org.dicio.skill.standard.capture

import org.dicio.numbers.unit.Number

/**
 * An interval in the original string that was captured by a capturing group of type [Number]
 * named [name].
 * @see Capture
 */
data class ParsedDataCapture<T>(
    override val name: String,
    override val start: Int,
    override val end: Int,
    val parsedData: T,
) : Capture
