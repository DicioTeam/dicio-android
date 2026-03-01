package org.dicio.skill.standard

import org.dicio.skill.skill.Score
import org.dicio.skill.standard.capture.Capture
import org.dicio.skill.standard.capture.ParsedDataCapture
import org.dicio.skill.standard.capture.StringCapture

data class StandardScore(
    val userMatched: Float,
    val userWeight: Float,
    val refMatched: Float,
    val refWeight: Float,

    // will form a binary tree structure, where inner nodes are Pairs of subtrees,
    // while the leaves are either Capture or StringRangeCapture
    val capturingGroups: Any?,
) : Score {
    fun score(): Float {
        return UM * userMatched +
                UW * userWeight +
                RM * refMatched +
                RW * refWeight
    }

    /**
     * This is not a well-behaving score and **should not** be used to compare two
     * [StandardScore]s. This is to be used strictly only when a score in range
     * [[0, 1]] is needed, e.g. to compare with scores of other types.
     */
    override fun scoreIn01Range(): Float {
        if (userMatched <= 0.0f || userWeight <= 0.0f || refMatched <= 0.0f || refWeight <= 0.0f) {
            return 0.0f
        }

        // use harmonic mean so that if one of the two terms is significantly lower than the other,
        // the mean tends towards the lower one
        return 2.0f / (
                userWeight / userMatched +
                refWeight / refMatched
        )
    }

    override fun isBetterThan(other: Score): Boolean {
        return if (other is StandardScore) {
            score() > other.score()
        } else {
            scoreIn01Range() > other.scoreIn01Range()
        }
    }

    fun exploreCapturingGroupsTree(node: Any?, name: String): Capture? {
        return when (node) {
            null ->
                null

            is Pair<*, *> ->
                exploreCapturingGroupsTree(node.first, name)
                    ?: exploreCapturingGroupsTree(node.second, name)

            is Capture ->
                if (node.name == name) node else null

            else ->
                throw IllegalArgumentException(
                    "Unexpected type found in capturing groups tree: type=${
                        node::class.simpleName
                    }, value=$node"
                )
        }
    }

    inline fun <reified T> getCapturingGroup(userInput: String, name: String): T? {
        val result = exploreCapturingGroupsTree(capturingGroups, name) ?: return null
        return when (result) {
            is StringCapture if T::class == String::class ->
                userInput.subSequence(result.start, result.end) as T

            is ParsedDataCapture<*> if result.parsedData is T ->
                result.parsedData

            else -> throw IllegalArgumentException("Capturing group \"$name\" has wrong type: expectedType=${
                T::class.simpleName}, actualType=${result::class.simpleName}, actualValue=\"$result\"")
        }
    }

    operator fun plus(other: StandardScore): StandardScore {
        return StandardScore(
            userMatched = this.userMatched + other.userMatched,
            userWeight = this.userWeight + other.userWeight,
            refMatched = this.refMatched + other.refMatched,
            refWeight = this.refWeight + other.refWeight,
            capturingGroups = if (this.capturingGroups == null) {
                other.capturingGroups
            } else if (other.capturingGroups == null) {
                this.capturingGroups
            } else {
                Pair(this.capturingGroups, other.capturingGroups)
            }
        )
    }

    fun plus(
        userMatched: Float = 0.0f,
        userWeight: Float = 0.0f,
        refMatched: Float = 0.0f,
        refWeight: Float = 0.0f,
    ): StandardScore {
        return StandardScore(
            userMatched = this.userMatched + userMatched,
            userWeight = this.userWeight + userWeight,
            refMatched = this.refMatched + refMatched,
            refWeight = this.refWeight + refWeight,
            capturingGroups = this.capturingGroups,
        )
    }

    fun plus(
        userMatched: Float = 0.0f,
        userWeight: Float = 0.0f,
        refMatched: Float = 0.0f,
        refWeight: Float = 0.0f,
        capturingGroup: Any
    ): StandardScore {
        return StandardScore(
            userMatched = this.userMatched + userMatched,
            userWeight = this.userWeight + userWeight,
            refMatched = this.refMatched + refMatched,
            refWeight = this.refWeight + refWeight,
            capturingGroups = if (this.capturingGroups == null) {
                capturingGroup
            } else {
                Pair(this.capturingGroups, capturingGroup)
            },
        )
    }

    companion object {
        val EMPTY = StandardScore(0.0f, 0.0f, 0.0f, 0.0f, null)

        fun keepBest(m1: StandardScore?, m2: StandardScore): StandardScore {
            return if (m1 == null || m2.score() > m1.score()) m2 else m1
        }

        const val UM: Float = 2.0f;
        const val UW: Float = -1.1f;
        const val RM: Float = 2.0f;
        const val RW: Float = -1.1f;
    }
}
