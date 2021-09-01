package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode
import de.tu_darmstadt.rs.logictool.common.representation.Variable
import obdd.logic.*
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

object FuseRecurseMapper : LutMapStrategy() {

    override val strategyName = "fusemap"
    private const val lutCap = 5

    // This is pretty dense, I'll try explaining as best I can
    override fun mapQRBDD(bdd: Bdd, outputName: String): List<Lut> {
        // Pre-sift to find good cuts
        val siftTime = measureTimeMillis {
            Sifter(bdd).siftFirstN(16)
        }
        log("Sifting took $siftTime ms")
        //debugDumpBdd(bdd)

        // Very useful to have access to all nodes of a level
        val nodesByLevel = getNodesByLevel(bdd)

        // Enumerate all possible cut positions and find the one that minimizes estimated cost
        val bestCut = (0 until bdd.variables.size).minByOrNull { scoreCut(bdd, nodesByLevel, it) }!!
        val nodesUnderCut = nodesByLevel[bestCut]
        log("Best cut at level $bestCut, cost ${scoreCut(bdd, nodesByLevel, bestCut)}, ${nodesUnderCut.size} nodes under cut")

        // Compute the formulas which "activate" / lead to each node under the cut
        val pathConditions = getPathConditions(bdd, bestCut)
        val relevantPathConditions = nodesUnderCut.map { pathConditions.getOrDefault(it, ConstTrue) }

        // Currently, path condition formulas are one-hot. We can save wires by converting to a dense binary representation
        // such that a select signal of 011 indicates the third node under the cut (instead of e.g. 0010000)
        val packedSelectSignals = densePackExclusiveFormulas(relevantPathConditions)
        val selectSignalIDs = genSignalIDs(packedSelectSignals.size)
        log("Packed select signal is ${packedSelectSignals.size} bit wide")

        // Using the generated select signals as input to our LUT, some free inputs remain which we will 'pack' with
        // variables from the following levels of the BDD
        val lutPackEndLevel = min(bdd.variables.size, bestCut + (lutCap - selectSignalIDs.size))
        val packVariables = bdd.variables.filter { it.number in bestCut until lutPackEndLevel }.map { it.name }
        log("Packing variables ${packVariables.joinToString(", ")} into new LUT(s)")

        val nodesUnderLut = if(lutPackEndLevel == bdd.variables.size) listOf(bdd.oneNode) else nodesByLevel[lutPackEndLevel]

        val internalPathConditionByEntryNode = nodesUnderCut.map { getPathConditionsFromNode(it, lutPackEndLevel) }

        // Create list of formulas that one-hot encode the correct node to take under the LUT end level
        val outNodeFormulas = nodesUnderLut.map { node ->
            internalPathConditionByEntryNode.mapIndexed{ i, pathCond ->
                // We take the internal path conditions for each entry node and qualify them with the correct select signal that activates that entry node
                And(pathCond.getOrDefault(node, ConstFalse), selectPrefixFor(i, selectSignalIDs)) as Formula
            }.reduce { acc, singleEntryCondition -> Or(acc, singleEntryCondition) } // Then take the or of all that to get a single formula for each output node
        }

        val lutInputs = (selectSignalIDs + packVariables).toTypedArray()

        val lowerMapped = if(lutPackEndLevel == bdd.variables.size)
            listOf(Lut(lutInputs, outputName, outNodeFormulas[0]))
        else {
            val lutFormulas = densePackExclusiveFormulas(outNodeFormulas)
            val lutOutIDs = genSignalIDs(lutFormulas.size)

            // These are the LUTs that take the select signal for the entry layer and convert into a select signal for the exit layer
            val luts = lutOutIDs.zip(lutFormulas).map { Lut(lutInputs, it.first, it.second) }

            val fusedBdd = genSelectBdd(lutOutIDs, lutPackEndLevel, nodesUnderLut.toList(), bdd)

            // At this point, we don't need the original BDD anymore, so we'll manually destroy it to make sure it can be garbage collected
            bdd.clear()

            // These are the LUTs representing everything below the exit layer (as well as the previously computed LUTs for between entry and exit)
            mapQRBDD(fusedBdd, outputName) + luts
        }

        // These are the LUTs representing the select signals / everything above the entry layer
        val upperMapped = packedSelectSignals.zip(selectSignalIDs).flatMap { mapFormula(it.first, it.second) }

        return upperMapped + lowerMapped
    }

    private fun genSelectBdd(selectSignals: List<String>, targetLevel: Int, targetNodes: List<BddNode>, oldBdd: Bdd): Bdd {
        val selectVariables = selectSignals.reversed().mapIndexed { index, s -> Variable(s, index) }
        val remainingVariables = oldBdd.variables.filter { it.number >= targetLevel }.sortedBy { it.number }
        remainingVariables.forEachIndexed { index, variable -> variable.number = index + selectVariables.size } // Re-number existing variables
        val variables = selectVariables + remainingVariables

        val root = genSelectBddRecursive(selectVariables, targetNodes)
        val bdd = Bdd(variables.toTypedArray(), oldBdd.oneNode, oldBdd.zeroNode, extractSubtree(root, variables.size))
        bdd.rootNode = root

        return bdd
    }

    private fun genSelectBddRecursive(selectVariables: List<Variable>, targetNodes: List<BddNode>) : BddNode {
        if(selectVariables.isEmpty() && targetNodes.size == 1)
            return targetNodes.first()

        val node = BddNode(selectVariables.first())
        val rest = selectVariables.drop(1)
        val subtreeCap = 1 shl rest.size

        if(targetNodes.size <= subtreeCap) {
            val child = genSelectBddRecursive(rest, targetNodes)
            node.zeroChild = child
            node.oneChild = child
        }
        else {
            node.zeroChild = genSelectBddRecursive(rest, targetNodes.subList(0, subtreeCap))
            node.oneChild = genSelectBddRecursive(rest, targetNodes.subList(subtreeCap, targetNodes.size))
        }

        return node
    }

    private fun scoreCut(bdd: Bdd, nodesByLevel: Array<MutableSet<BddNode>>, cutLevel: Int) : Int {
        val cutWidth = nodesByLevel[cutLevel].size
        val heightAboveCut = cutLevel
        val selectSignalWidth = ceil(log2(cutWidth.toDouble())).toInt()

        // Avoid infeasible cuts
        if(selectSignalWidth > lutCap)
            return Int.MAX_VALUE

        val heightBelowCut = max((bdd.variables.size - cutLevel) - (lutCap - selectSignalWidth), 0)

        val estimatedUpperCost = selectSignalWidth * ceil(heightAboveCut.toDouble() / (lutCap - 1)).toInt()

        val estimatedLowerCost = if(heightBelowCut == 0)
            0
        else {
            val outMultiplicity = max(ceil(log2(nodesByLevel[bdd.variables.size - heightBelowCut].size.toDouble())).toInt(), 1)
            if(outMultiplicity >= lutCap)
                return Int.MAX_VALUE
            ceil(((heightBelowCut.toDouble() + outMultiplicity) / (lutCap - 1))).toInt()
        }

        return estimatedUpperCost + estimatedLowerCost
    }

}