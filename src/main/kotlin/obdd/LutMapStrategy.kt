package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import obdd.bdd.BddCutoffReached
import obdd.bdd.BddOrderHeuristics
import obdd.bdd.QrbddBuilder
import obdd.bdd.QrbddReducer
import obdd.logic.*
import obdd.serializers.DotSerializer
import kotlin.system.measureTimeMillis

abstract class LutMapStrategy {

    protected abstract val strategyName: String
    private var outFileCounter = 0
    private var signalIdCounter = 0
    private val bddSizeSearchCutoff = 10 // don't bother searching for better BDDs once a very small one is found

    fun mapBLIF(filename: String) : String {
        val parsed = BlifParser.parse(filename)
        val formulas = parsed.second
        val inputs = parsed.first.joinToString(" ")
        val outputs = formulas.joinToString(" ") { it.first }
        val header = ".model Mapped\n.inputs $inputs\n.outputs $outputs\n"

        val mapped: List<Lut>
        val elapsed = measureTimeMillis {
            mapped = formulas.flatMapIndexed{ idx, output ->
                log("Mapping output ${output.first} (${idx+1}/${formulas.size})", 3)
                mapFormula(output.second, output.first)
            }
        }
        log("Mapping complete! Took $elapsed ms, created ${mapped.size} LUTs total", 4)
        println("$elapsed|${mapped.size}")

        return header + mapped.joinToString("\n") { it.toBLIF() } + "\n.end"
    }

    fun mapFormula(formula: Formula, outputName: String) : List<Lut> {
        // Get an assortment of promising variable orders based on different heuristics
        val orders = BddOrderHeuristics(formula).mix()
        log("Candidate orders generated", 2)
        var bestSize = Int.MAX_VALUE
        var bestPreSize = Int.MAX_VALUE
        var bdd: Bdd? = null

        for (i in orders.indices) {
            // To avoid getting stuck building very large BDDs, assume nothing good can come from a BDD that is more than twice as large pre-reduction as the current best BDD was pre-reduction
            QrbddBuilder.setSizeCutoff(if(bestPreSize == Int.MAX_VALUE) bestPreSize else bestPreSize * 2)
            try {
                val candidate = QrbddBuilder.create(formula, orders[i])
                val preSize = candidate.nodes.size
                QrbddReducer.reduceBdd(candidate)
                log("Trying order #$i for a initial size of ${candidate.nodes.size} nodes", 2)
                if(candidate.nodes.size < bestSize) {
                    bdd = candidate
                    bestSize = candidate.nodes.size
                    bestPreSize = preSize
                }
                if(bestSize < bddSizeSearchCutoff)
                    break
            }
            catch(e: BddCutoffReached) {
                log("Bdd size cutoff reached for attempt #$i", 2)
            }
        }

        bdd!!
        log("QRBBD built (size=${bdd.nodes.size}), proceeding with mapping", 2)

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
                if(entryNodeIndex % (2 shl i) < (1 shl i)) Not(Var(sid)) else Var(sid)
            }.reduce { acc, atom -> And(acc, atom) }
    }

    // Generate a group of fresh signal identifiers / wires for the desired bit width
    protected fun genSignalIDs(bitWidth: Int) : List<String> {
        signalIdCounter += 1
        return (0 until bitWidth).map { "${strategyName}_int_${signalIdCounter}_b$it" }
    }

    protected fun log(msg: String, level: Int) {
        if(level >= logLevel)
            System.err.println("[$strategyName] $msg")
    }

    protected fun debugDumpBdd(bdd: Bdd) {
        outFileCounter += 1
        val filename = "$strategyName-input-${outFileCounter}.dot"
        DotSerializer.writeToFile(bdd, filename)
        log("Dumped base BDD to $filename", 1)
    }
}