package obdd.logic

import java.lang.Exception

abstract class Formula {
    abstract fun eval(varMap: Map<String, Boolean>): Boolean

    fun simplify(nonce: Int): Formula = simplify(nonce, "", false)
    fun simplify(nonce: Int, varName: String, interpretation: Boolean): Formula {
        if(simplified != null && nonce == this.nonce)
            return simplified!!
        this.nonce = nonce
        simplified = internalSimplify(varName, interpretation)
        return simplified!!
    }

    fun computeVarWeights(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        computeVarWeights(Int.MAX_VALUE, map)
        return map
    }

    fun computeVarCounts(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        computeVarCounts(map)
        return map
    }

    open fun computeVarWeights(baseWeight: Int, map: MutableMap<String, Int>) {}
    open fun computeVarCounts(map: MutableMap<String, Int>) {}
    open fun size(): Int = 1
    abstract fun synEq(other: Formula): Boolean

    // Once logic formulas are not strictly trees anymore, we need to make sure to only simplify each unique part once
    // to do so, we cache the most recent simplification with a 'nonce' to ensure cache-validity
    protected abstract fun internalSimplify(varName: String, interpretation: Boolean): Formula
    protected var nonce: Int = 0
    private var simplified: Formula? = null
}

class Var(val name: String) : Formula() {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return if(varMap.containsKey(name))
            varMap[name]!!
        else
            throw Exception("Variable assignment does not contain value for '$name'")
    }

    override fun internalSimplify(varName: String, interpretation: Boolean): Formula {
        return if(varName == name)
            if(interpretation) ConstTrue else ConstFalse
        else
            Var(name)
    }

    override fun synEq(other: Formula): Boolean {
        return other is Var && other.name == name
    }

    override fun computeVarWeights(baseWeight: Int, map: MutableMap<String, Int>){
        map.merge(name, baseWeight) { a, b -> a + b }
    }

    override fun computeVarCounts(map: MutableMap<String, Int>) {
        map.merge(name, 1){ a, b -> a + b}
    }
    override fun toString() = name
}

object ConstTrue : Formula() {
    override fun eval(varMap: Map<String, Boolean>) = true
    override fun internalSimplify(varName: String, interpretation: Boolean) = this

    override fun synEq(other: Formula) = other == this
    override fun toString() = "true"
}

object ConstFalse : Formula() {
    override fun eval(varMap: Map<String, Boolean>) = false
    override fun internalSimplify(varName: String, interpretation: Boolean) = this

    override fun synEq(other: Formula) = other == this
    override fun toString() = "false"
}

class Not(val child: Formula) : Formula() {
    override fun eval(varMap: Map<String, Boolean>) = !child.eval(varMap)

    override fun internalSimplify(varName: String, interpretation: Boolean): Formula {
        return when (val childSim = child.simplify(nonce, varName, interpretation)) {
            ConstTrue -> ConstFalse
            ConstFalse -> ConstTrue
            is Not -> childSim.child
            else -> Not(childSim)
        }
    }

    override fun synEq(other: Formula) = other is Not && child.synEq(other.child)

    override fun computeVarWeights(baseWeight: Int, map: MutableMap<String, Int>) = child.computeVarWeights(baseWeight, map)
    override fun computeVarCounts(map: MutableMap<String, Int>) = child.computeVarCounts(map)
    override fun size() = 1 + child.size()
    override fun toString() = "!${child}"
}

abstract class BinOp(val left: Formula, val right: Formula) : Formula() {
    override fun computeVarWeights(baseWeight: Int, map: MutableMap<String, Int>) {
        left.computeVarWeights(baseWeight / 2, map)
        right.computeVarWeights(baseWeight / 2, map)
    }

    override fun computeVarCounts(map: MutableMap<String, Int>) {
        left.computeVarCounts(map)
        right.computeVarCounts(map)
    }

    override fun size() = 1 + left.size() + right.size()
}

class And(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) && right.eval(varMap)
    }

    override fun internalSimplify(varName: String, interpretation: Boolean): Formula {
        val leftSim = left.simplify(nonce, varName, interpretation)

        if(leftSim == ConstFalse)
            return ConstFalse

        val rightSim = right.simplify(nonce, varName, interpretation)

        return when {
            rightSim == ConstFalse -> ConstFalse
            leftSim == ConstTrue -> rightSim
            rightSim == ConstTrue -> leftSim
            else -> And(leftSim, rightSim)
        }
    }

    override fun synEq(other: Formula) = other is And && left.synEq(other.left) && right.synEq(other.right)

    override fun toString() = "(${left} & ${right})"
}

class Or(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) || right.eval(varMap)
    }

    override fun internalSimplify(varName: String, interpretation: Boolean): Formula {
        val leftSim = left.simplify(nonce, varName, interpretation)
        val rightSim = right.simplify(nonce, varName, interpretation)
        return when {
            leftSim == ConstTrue || rightSim == ConstTrue -> ConstTrue
            leftSim == ConstFalse -> rightSim
            rightSim == ConstFalse -> leftSim
            else -> Or(leftSim, rightSim)
        }
    }

    override fun synEq(other: Formula) = other is Or && left.synEq(other.left) && right.synEq(other.right)

    override fun toString() = "(${left} | ${right})"
}

class Equiv(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) == right.eval(varMap)
    }

    override fun internalSimplify(varName: String, interpretation: Boolean): Formula {
        val leftSim = left.simplify(nonce, varName, interpretation)
        val rightSim = right.simplify(nonce, varName, interpretation)

        return when {
            leftSim == rightSim -> ConstTrue
            leftSim == ConstTrue && rightSim == ConstFalse -> ConstFalse
            leftSim == ConstFalse && rightSim == ConstTrue -> ConstFalse
            else -> Equiv(leftSim, rightSim)
        }
    }

    override fun synEq(other: Formula) = other is Equiv && left.synEq(other.left) && right.synEq(other.right)

    override fun toString() = "(${left} <=> ${right})"
}