package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode
import de.tu_darmstadt.rs.logictool.common.representation.Variable
import obdd.logic.Formula

/**
 * Quick re-implementation of the basic BDD factory because the given one is buggy
 */
class NaiveBddBuilder {

    private lateinit var formula: Formula
    private lateinit var interpretation: MutableMap<String, Boolean>
    private lateinit var order: Array<Variable>

    fun create(formula: Formula, stringOrder: List<String>) : Bdd {
        val order = genOrder(stringOrder)
        this.formula = formula
        this.interpretation = mutableMapOf()
        this.order = order

        val bdd = Bdd(order)

        // Special case: constant functions
        bdd.rootNode = when {
            order.isEmpty() && formula.eval(emptyMap()) -> bdd.oneNode
            order.isEmpty() -> bdd.zeroNode
            else -> createSplit(bdd, 0)
        }

        return bdd
    }

    private fun createSplit(bdd: Bdd, vi: Int) : BddNode {
        val variable = order[vi]
        val node = BddNode(variable)
        bdd.nodes.add(node)

        // One branch
        interpretation[variable.name] = true

        node.oneChild = if(vi == order.lastIndex) {
            if(formula.eval(interpretation))
                bdd.oneNode
            else
                bdd.zeroNode
        }
        else
            createSplit(bdd, vi + 1)

        // Zero branch
        interpretation[variable.name] = false

        node.zeroChild = if(vi == order.lastIndex) {
            if(formula.eval(interpretation))
                bdd.oneNode
            else
                bdd.zeroNode
        }
        else
            createSplit(bdd, vi + 1)

        return node
    }
}