package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import obdd.logic.*
import obdd.serializers.DotSerializer
import kotlin.system.measureTimeMillis

abstract class LutMapStrategy {

    protected abstract val strategyName: String
    private var outFileCounter = 0
    private var signalIdCounter = 0
    private val initialBddSizeSearchCutoff = 1000 // don't bother searching for better BDDs once one sub-1k nodes is found
    private val initialBddSizeSearchSoftCutoff = 75000 // abort search if a sub 75k bdd is found in the first 3 attempts (intuition: later orders are last-ditch only and usually worse than the first 3)

    fun mapBLIF(filename: String) : String {
        val parsed = BlifParser.parse(filename)
        val formulas = parsed.second
        val inputs = parsed.first.joinToString(" ")
        val placeholder = parsed.first.first()
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

        return header + mapped.joinToString("\n") { it.toBLIF(listOf(3, 5), placeholder) } + "\n.end"
    }

    fun mapFormula(formula: Formula, outputName: String) : List<Lut> {
        // Get an assortment of promising variable orders based on different heuristics
        val orders = BddOrderHeuristics(formula).mix()
        var bestSize = Int.MAX_VALUE
        var bdd: Bdd? = null

        for (i in orders.indices) {
            val candidate = QrbddBuilder.create(formula, orders[i])
            log("Trying order #$i for a initial size of ${candidate.nodes.size} nodes", 2)
            if(candidate.nodes.size < bestSize) {
                bdd = candidate
                bestSize = candidate.nodes.size
            }
            if(bestSize < initialBddSizeSearchCutoff || (i >= 2 && bestSize < initialBddSizeSearchSoftCutoff))
                break
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