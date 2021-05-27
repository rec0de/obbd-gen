package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode
import de.tu_darmstadt.rs.logictool.common.representation.Variable
import obdd.logic.ConstFalse
import obdd.logic.ConstTrue
import obdd.logic.Formula

/**
 * An attempt at a smarter OBDD factory optimized for algebraic inputs
 * As opposed to the provided BddFactory, this BddBuilder should be thread-safe can probably be parallelized
 */
object BddBuilder {

    fun create(formula: Formula, stringOrder: List<String>) : Bdd {
        val order = genOrder(stringOrder)
        val simplified = formula.simplify(emptyMap())

        val bdd = Bdd(order)

        bdd.rootNode = when(simplified) {
            ConstFalse -> bdd.zeroNode
            ConstTrue -> bdd.oneNode
            else -> createSplit(bdd, formula, order, 0)
        }

        return bdd
    }

    private fun createSplit(bdd: Bdd, formula: Formula, order: Array<Variable>, vi: Int) : BddNode {
        val variable = order[vi]
        val node = BddNode(variable)
        bdd.nodes.add(node)

        // One branch
        val oneFormula = formula.simplify(mapOf(variable.name to true))

        node.oneChild = when(oneFormula) {
            ConstTrue -> bdd.oneNode
            ConstFalse -> bdd.zeroNode
            else -> createSplit(bdd, oneFormula, order, vi + 1)
        }

        // Zero branch
        val zeroFormula = formula.simplify(mapOf(variable.name to false))

        node.zeroChild = when(zeroFormula) {
            ConstTrue -> bdd.oneNode
            ConstFalse -> bdd.zeroNode
            else -> createSplit(bdd, zeroFormula, order, vi + 1)
        }

        return node
    }
}

fun genOrder(stringOrder: List<String>) = stringOrder.withIndex().map{ Variable(it.value, it.index) }.toTypedArray()