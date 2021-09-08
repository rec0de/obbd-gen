package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode
import de.tu_darmstadt.rs.logictool.common.representation.Variable
import obdd.logic.ConstFalse
import obdd.logic.ConstTrue
import obdd.logic.Formula

interface GenericBddBuilder {
    fun create(formula: Formula, stringOrder: List<String>) : Bdd
}

abstract class RecursiveSplitBddBuilder : GenericBddBuilder {
    override fun create(formula: Formula, stringOrder: List<String>) : Bdd {
        val order = genOrder(stringOrder)
        val simplified = formula.simplify(simplifyNonce++) // Perform generic simplification

        val bdd = Bdd(order)

        bdd.rootNode = when(simplified) {
            ConstFalse -> bdd.zeroNode
            ConstTrue -> bdd.oneNode
            else -> createSplit(bdd, simplified, order, 0)
        }

        return bdd
    }

    protected abstract fun createSplit(bdd: Bdd, formula: Formula, order: Array<Variable>, vi: Int) : BddNode
}

/**
 * An attempt at a smarter OBDD factory optimized for algebraic inputs
 * As opposed to the provided BddFactory, this BddBuilder should be thread-safe can probably be parallelized
 */
object BddBuilder : RecursiveSplitBddBuilder() {

    private const val SYN_EQ = true

    override fun createSplit(bdd: Bdd, formula: Formula, order: Array<Variable>, vi: Int) : BddNode {
        val variable = order[vi]
        val node = BddNode(variable)

        // One branch
        val oneFormula = formula.simplify(simplifyNonce++, variable.name, true)

        node.oneChild = when(oneFormula) {
            ConstTrue -> bdd.oneNode
            ConstFalse -> bdd.zeroNode
            else -> createSplit(bdd, oneFormula, order, vi + 1)
        }

        // Zero branch
        val zeroFormula = formula.simplify(simplifyNonce++, variable.name, false)

        // if both branch formulas are syntactically equal, the subtrees will be equal as well
        if(SYN_EQ && oneFormula.synEq(zeroFormula))
            return node.oneChild

        node.zeroChild = when(zeroFormula) {
            ConstTrue -> bdd.oneNode
            ConstFalse -> bdd.zeroNode
            else -> createSplit(bdd, zeroFormula, order, vi + 1)
        }

        bdd.nodes.add(node)
        return node
    }
}

fun genOrder(stringOrder: List<String>) = stringOrder.withIndex().map{ Variable(it.value, it.index) }.toTypedArray()