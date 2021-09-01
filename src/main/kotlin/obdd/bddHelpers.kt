package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode
import obdd.logic.*
import kotlin.math.ceil
import kotlin.math.log2

fun getNodesByLevel(bdd: Bdd): Array<MutableSet<BddNode>> {
    val res = Array(bdd.variables.size) { mutableSetOf<BddNode>() }
    bdd.nodes.filter { it != null && it.variable != null }.forEach { node ->
        res[node.variable.number].add(node)
    }
    return res
}

fun extractSubtree(startNode: BddNode, untilLevel: Int) : Set<BddNode> {
    val subtree = mutableSetOf<BddNode>()
    val worklist = mutableSetOf<BddNode>(startNode)

    while(worklist.isNotEmpty()) {
        val node = worklist.first()
        worklist.remove(node)

        if(node.variable?.number ?: Int.MAX_VALUE >= untilLevel)
            continue

        subtree.add(node)
        if(node.zeroChild != null && !subtree.contains(node.zeroChild))
            worklist.add(node.zeroChild)
        if(node.oneChild != null && !subtree.contains(node.oneChild))
            worklist.add(node.oneChild)
    }

    return subtree
}

fun getPathConditionsFromNode(startNode: BddNode, untilLevel: Int) = getPathConditions(extractSubtree(startNode, untilLevel), untilLevel)

fun getPathConditions(nodes: Iterable<BddNode>, untilLevel: Int) : Map<BddNode, Formula> {
    val pathCondMap = mutableMapOf<BddNode, Formula>()
    val sortedNodes = nodes.filter{ it.variable?.number ?: Int.MAX_VALUE < untilLevel }.sortedBy { it.variable.number }

    sortedNodes.forEach { node ->
        val baseFormula = pathCondMap.getOrDefault(node, ConstTrue)

        if(node.zeroChild == node.oneChild) {
            pathCondMap[node.zeroChild] = Or(pathCondMap.getOrDefault(node.zeroChild, ConstFalse), baseFormula)
            pathCondMap[node.oneChild] = Or(pathCondMap.getOrDefault(node.oneChild, ConstFalse), baseFormula)
        }
        else {
            pathCondMap[node.zeroChild] = Or(pathCondMap.getOrDefault(node.zeroChild, ConstFalse), And(baseFormula, Not(Var(node.variable.name))))
            pathCondMap[node.oneChild] = Or(pathCondMap.getOrDefault(node.oneChild, ConstFalse), And(baseFormula, Var(node.variable.name)))
        }
    }

    return pathCondMap
}

// Takes a list of mutually exclusive formulas (basically a one-hot encoding) and packs it into a dense binary encoding
// i.e. for input formulas a b c d, the truth states of the output formulas o1 and o2 correspond to the following truth states of abcd:
// 00: a
// 01: b
// 10: c
// 11: d
// NOTE: Outputs are ordered LSB to MSB
fun densePackExclusiveFormulas(formulas: List<Formula>) : List<Formula> {
    val bitSize = ceil(log2(formulas.size.toDouble())).toInt()
    val output = mutableListOf<Formula>()

    // Generate individual output bits
    for(bit in 0 until bitSize) {
        val constituents = formulas.filterIndexed { index, _ -> index % (2 shl bit) >= (1 shl bit) }
        output.add(constituents.reduce{ acc, part -> Or(acc, part) })
    }

    return output
}