package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import obdd.logic.Formula
import obdd.logic.LutGraph

abstract class LutMapStrategy {

    fun mapBLIF(filename: String) : List<LutGraph> {
        val formulas = BlifParser.parse(filename)
        return formulas.map{ mapFormula(it) }
    }

    fun mapFormula(formula: Formula) : LutGraph {
        val varWeights = formula.computeVarWeights(Int.MAX_VALUE)
        val order = varWeights.toList().sortedByDescending { it.second }.map { it.first }
        val bdd = QrbddBuilder.create(formula, order)

        return mapQRBDD(bdd)
    }

    protected abstract fun mapQRBDD(bdd: Bdd) : LutGraph
}