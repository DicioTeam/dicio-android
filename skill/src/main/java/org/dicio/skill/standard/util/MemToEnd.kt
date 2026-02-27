package org.dicio.skill.standard.util

import org.dicio.skill.standard.StandardScore


fun initialMemToEnd(cumulativeWeight: FloatArray): Array<StandardScore> {
    val endWeight = cumulativeWeight.last()
    return Array(cumulativeWeight.size) { start ->
        StandardScore(
            userMatched = 0.0f,
            userWeight = endWeight - cumulativeWeight[start],
            refMatched = 0.0f,
            refWeight = 0.0f,
            capturingGroups = null,
        )
    }
}

fun normalizeMemToEnd(memToEnd: Array<StandardScore>, cumulativeWeight: FloatArray) {
    for (i in memToEnd.size-2 downTo 0) {
        memToEnd[i] = StandardScore.keepBest(
            memToEnd[i],
            memToEnd[i+1].plus(userWeight = cumulativeWeight[i+1] - cumulativeWeight[i]),
        )
    }
}

fun addRefWeightEverywhere(memToEnd: Array<StandardScore>, refWeight: Float) {
    for (i in memToEnd.indices) {
        memToEnd[i] = memToEnd[i].plus(refWeight = refWeight)
    }
}
