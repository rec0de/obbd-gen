package obdd.bdd

import obdd.logic.Formula
import obdd.simplifyNonce

class BddOrderHeuristics(val formula: Formula) {
    private val counts = formula.computeVarCounts()
    val variables = counts.keys

    private val varWeights by lazy {
        formula.computeVarWeights()
    }

    fun mix(): List<List<String>> {
        return listOf(varCount(), subGraphComplexity(), weightCountHybrid(), weightCountHybrid(1000000), varWeight())
    }

    fun varWeight(): List<String> {
        return varWeights.toList().sortedByDescending { it.second }.map { it.first }
    }

    fun varCount(): List<String> {
        return counts.toList().sortedByDescending { it.second }.map { it.first }
    }

    fun weightCountHybrid(weightFactor: Int = 10000000): List<String> {
        val varScores = counts.mapValues { it.value.toLong() * weightFactor + varWeights[it.key]!! }
        return varScores.toList().sortedByDescending { it.second }.map { it.first }
    }

    fun subGraphComplexity(): List<String> {
        val variables = varCount().toMutableList()
        val order = mutableListOf<String>()
        var simplified = formula
        var newSimplified = simplified
        var bestSize = Int.MAX_VALUE

        while (variables.isNotEmpty()) {
            var bestVar = variables.first()

            variables.forEach { v ->
                val candTrue = simplified.simplify(simplifyNonce++, v, true)
                val sizeTrue = candTrue.size()
                if (sizeTrue < bestSize) {
                    newSimplified = candTrue
                    bestSize = sizeTrue
                    bestVar = v
                }

                val candFalse = simplified.simplify(simplifyNonce++, v, false)
                val sizeFalse = candFalse.size()
                if (sizeFalse < bestSize) {
                    newSimplified = candFalse
                    bestSize = sizeFalse
                    bestVar = v
                }
            }

            variables.remove(bestVar)
            order.add(bestVar)
            simplified = newSimplified
        }

        return order
    }
}