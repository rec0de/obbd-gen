package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import obdd.logic.Formula
import obdd.logic.Lut

abstract class LutMapStrategy {

    fun mapBLIF(filename: String) : List<List<Lut>> {
        val formulas = BlifParser.parse(filename)
        return formulas.map{ mapFormula(it.second, it.first) }
    }

    fun mapFormula(formula: Formula, outputName: String) : List<Lut> {
        val varWeights = formula.computeVarWeights(Int.MAX_VALUE)
        val order = varWeights.toList().sortedByDescending { it.second }.map { it.first }
        val bdd = QrbddBuilder.create(formula, order)

        return mapQRBDD(bdd, outputName)
    }

    protected abstract fun mapQRBDD(bdd: Bdd, outputName: String) : List<Lut>
}