package org.dicio.skill.standard.capture

/**
 * An interval in the original string that was captured by a capturing group of type [String]
 * named [name].
 * @see Capture
 */
data class StringCapture(
    override val name: String,
    override val start: Int,
    override val end: Int,
) : Capture
