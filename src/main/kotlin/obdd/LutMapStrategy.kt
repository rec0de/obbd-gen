package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import obdd.logic.*
import obdd.serializers.DotSerializer

abstract class LutMapStrategy {

    protected abstract val strategyName: String
    private var outFileCounter = 0
    private var signalIdCounter = 0

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

    // Construct a logic prefix that becomes true if and only if the truth values of the select signals
    // encode the given index (in binary, select signals being ordered LSB -> MSB)
    protected fun selectPrefixFor(entryNodeIndex: Int, selectSignalIDs: List<String>) : Formula {
        return if(selectSignalIDs.isEmpty())
            ConstTrue
        else
            selectSignalIDs.mapIndexed { i, sid ->
                if(entryNodeIndex % (2 shl i) == 0) Not(Var(sid)) else Var(sid)
            }.reduce { acc, atom -> And(acc, atom) }
    }

    // Generate a group of fresh signal identifiers / wires for the desired bit width
    protected fun genSignalIDs(bitWidth: Int) : List<String> {
        signalIdCounter += 1
        return (0 until bitWidth).map { "${strategyName}_int_${signalIdCounter}_b$it" }
    }

    protected fun log(msg: String) {
        println("[$strategyName] $msg")
    }

    protected fun debugDumpBdd(bdd: Bdd) {
        outFileCounter += 1
        val filename = "$strategyName-input-${outFileCounter}.dot"
        DotSerializer.writeToFile(bdd, filename)
        log("Dumped base BDD to $filename")
    }
}