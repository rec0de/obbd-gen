package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode
import de.tu_darmstadt.rs.logictool.common.representation.Variable
import obdd.logic.ConstFalse
import obdd.logic.ConstTrue
import obdd.logic.Formula


object QrbddBuilder : RecursiveSplitBddBuilder() {

    // Keep references to "short circuit" constructions to const 1 / const 0 that are already available
    private val zeroRail : MutableMap<Int, BddNode> = mutableMapOf()
    private val oneRail : MutableMap<Int, BddNode> = mutableMapOf()
    private var cutoff: Int = 0

    override fun create(formula: Formula, stringOrder: List<String>): Bdd {
        zeroRail.clear()
        oneRail.clear()
        return super.create(formula, stringOrder)
    }

    fun setSizeCutoff(size: Int) {
        cutoff = size
    }

    override fun createSplit(bdd: Bdd, formula: Formula, order: Array<Variable>, vi: Int) : BddNode {
        val variable = order[vi]
        val node = BddNode(variable)

        // One branch
        val oneFormula = formula.simplify(simplifyNonce++, variable.name, true)

        node.oneChild = when(oneFormula) {
            ConstTrue -> quickShortCircuit(bdd, order, vi + 1, true)
            ConstFalse -> quickShortCircuit(bdd, order, vi + 1, false)
            else -> createSplit(bdd, oneFormula, order, vi + 1)
        }

        // Zero branch
        val zeroFormula = formula.simplify(simplifyNonce++, variable.name, false)

        node.zeroChild = when {
            oneFormula.synEq(zeroFormula) -> node.oneChild // if both branch formulas are syntactically equal, the subtrees will be equal as well
            zeroFormula == ConstTrue -> quickShortCircuit(bdd, order, vi + 1, true)
            zeroFormula == ConstFalse -> quickShortCircuit(bdd, order, vi + 1, false)
            else -> createSplit(bdd, zeroFormula, order, vi + 1)
        }

        bdd.nodes.add(node)
        if(cutoff != 0 && bdd.nodes.size > cutoff)
            throw BddCutoffReached()
        return node
    }

    private fun quickShortCircuit(bdd: Bdd, order: Array<Variable>, vi: Int, circuitToOne: Boolean) : BddNode {
        return when {
            circuitToOne && vi == order.size -> bdd.oneNode
            !circuitToOne && vi == order.size -> bdd.zeroNode
            circuitToOne && oneRail.containsKey(vi) -> oneRail[vi]!!
            !circuitToOne && zeroRail.containsKey(vi) -> zeroRail[vi]!!
            else -> {
                val node = BddNode(order[vi])

                node.oneChild = quickShortCircuit(bdd, order, vi + 1, circuitToOne)
                node.zeroChild = node.oneChild
                bdd.nodes.add(node)

                if(circuitToOne)
                    oneRail[vi] = node
                else
                    zeroRail[vi] = node

                node
            }
        }
    }
}

class BddCutoffReached(): Exception("pre-set bdd size cutoff reached, aborting generation")