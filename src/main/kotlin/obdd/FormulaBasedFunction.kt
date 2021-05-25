package obdd

import de.tu_darmstadt.rs.logictool.common.representation.BooleanFunction
import de.tu_darmstadt.rs.logictool.common.representation.Variable
import obdd.logic.Formula

class FormulaBasedFunction(val formula: Formula) : BooleanFunction {
    private val vars = formula.collectVars()
    private val libVarArray = vars.withIndex().map { Variable(it.value, it.index) }.toTypedArray()

    override fun getName() = "Formula-based function for: $formula"

    override fun getVariables() = libVarArray

    override fun compute(inputs: BooleanArray?): Boolean {
        val assignments = vars.zip(inputs!!.toTypedArray()).associate { it }
        return formula.eval(assignments)
    }
}