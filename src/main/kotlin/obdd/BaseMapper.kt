package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode
import obdd.logic.*
import obdd.serializers.DotSerializer
import kotlin.math.max
import kotlin.math.ceil
import kotlin.math.log2

object BaseMapper : LutMapStrategy() {

    private var outFileCounter = 0
    private var signalIdCounter = 0
    private const val lutCap = 5

    // This is pretty dense, I'll try explaining as best I can
    override fun mapQRBDD(bdd: Bdd): LutGraph {
        // Pre-sift to find good cuts
        Sifter(bdd).sift()
        debugDumpBdd(bdd)

        // Very useful to have access to all nodes of a level
        val nodesByLevel = getNodesByLevel(bdd)

        // Enumerate all possible cut positions and find the one that minimizes estimated cost
        val bestCut = (0 until bdd.variables.size).minByOrNull { scoreCut(bdd, nodesByLevel, it) }!!
        val nodesUnderCut = nodesByLevel[bestCut]
        log("Best cut at level $bestCut, cost ${scoreCut(bdd, nodesByLevel, bestCut)}")

        // Compute the formulas which "activate" / lead to each node under the cut
        val pathConditions = getPathConditions(bdd, bestCut)
        val relevantPathConditions = nodesUnderCut.map { pathConditions[it]!! }

        // Currently, path condition formulas are one-hot. We can save wires by converting to a dense binary representation
        // such that a select signal of 011 indicates the third node under the cut (instead of e.g. 0010000)
        val packedSelectSignals = densePackExclusiveFormulas(relevantPathConditions)
        val selectSignalIDs = genSignalIDs(packedSelectSignals.size)
        log("Packed select signal is ${packedSelectSignals.size} bit wide")
        packedSelectSignals.zip(selectSignalIDs).forEach { log("${it.second}: ${it.first.simplify()}") }

        // Using the generated select signals as input to our LUT, some free inputs remain which we will 'pack' with
        // variables from the following levels of the BDD
        val lutPackEndLevel = max(bdd.variables.size, bestCut + (lutCap - packedSelectSignals.size))
        log("Packing variables $bestCut to $lutPackEndLevel into new LUT(s)")

        val internalPathConditionByEntryNode = nodesUnderCut.map { getPathConditionsFromNode(it, lutPackEndLevel) }
        val nodesUnderLut = if(lutPackEndLevel == bdd.variables.size) listOf(bdd.oneNode) else nodesByLevel[lutPackEndLevel]

        val outNodeFormulas = nodesUnderLut.map { node ->
            internalPathConditionByEntryNode.mapIndexed{ i, pathCond ->
                // We take the internal path conditions for each entry node and qualify them with the correct select signal that activates that entry node
                And(pathCond.getOrDefault(node, ConstFalse), selectPrefixFor(i, selectSignalIDs)) as Formula
            }.reduce { acc, singleEntryCondition -> Or(acc, singleEntryCondition) } // Then take the or of all that to get a single formula for each output node
        }

        if(lutPackEndLevel == bdd.variables.size) {
            val lutFormula = outNodeFormulas[0].simplify()
            log("Final output is: $lutFormula")
        }

        throw Exception("yea not done implementing this")
    }

    // Construct a logic prefix that becomes true if and only if the truth values of the select signals
    // encode the given index (in binary, select signals being ordered LSB -> MSB)
    private fun selectPrefixFor(entryNodeIndex: Int, selectSignalIDs: List<String>) : Formula {
        return selectSignalIDs.mapIndexed { i, sid ->
            if(entryNodeIndex % (2 shl i) == 0) Not(Var(sid)) else Var(sid)
        }.reduce { acc, atom -> And(acc, atom) }
    }

    // Generate a group of fresh signal identifiers / wires for the desired bit width
    private fun genSignalIDs(bitWidth: Int) : List<String> {
        signalIdCounter += 1
        return (0 until bitWidth).map { "basemap_int_${signalIdCounter}_b$it" }
    }

    private fun log(msg: String) {
        println("[base-mapper] $msg")
    }

    private fun debugDumpBdd(bdd: Bdd) {
        outFileCounter += 1
        val filename = "baseMapper-input-$outFileCounter.dot"
        DotSerializer.writeToFile(bdd, filename)
        log("Dumped base BDD to $filename")
    }

    private fun scoreCut(bdd: Bdd, nodesByLevel: Array<MutableSet<BddNode>>, cutLevel: Int) : Int {
        val cutWidth = nodesByLevel[cutLevel].size
        val heightAboveCut = cutLevel
        val selectSignalWidth = ceil(log2(cutWidth.toDouble())).toInt()

        // Avoid infeasible cuts
        if(selectSignalWidth > lutCap)
            return Int.MAX_VALUE

        val heightBelowCut = max((bdd.variables.size - cutLevel) - (lutCap - selectSignalWidth), 0)

        val estimatedUpperCost = selectSignalWidth * ceil(heightAboveCut.toDouble() / lutCap).toInt()

        // TODO: lower cost estimation is incorrect
        val estimatedLowerCost = if(heightBelowCut == 0) 0 else ceil((heightBelowCut.toDouble() / lutCap)).toInt() * nodesByLevel[bdd.variables.size - heightBelowCut].size

        return estimatedUpperCost + estimatedLowerCost
    }

}