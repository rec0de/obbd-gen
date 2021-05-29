package obdd.logic

import java.lang.Exception

interface Formula {
    fun eval(varMap: Map<String, Boolean>): Boolean
    fun simplify(varMap: Map<String, Boolean>): Formula
    fun computeVarWeights(baseWeight: Int): MutableMap<String, Int>
}

class Var(val name: String) : Formula {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return if(varMap.containsKey(name))
            varMap[name]!!
        else
            throw Exception("Variable assignment does not contain value for '$name'")
    }

    override fun simplify(varMap: Map<String, Boolean>): Formula {
        return if(varMap.containsKey(name))
            if(varMap[name]!!) ConstTrue else ConstFalse
        else
            Var(name)
    }

    override fun computeVarWeights(baseWeight: Int) = mutableMapOf(name to baseWeight)
    override fun toString() = name
}

object ConstTrue : Formula {
    override fun eval(varMap: Map<String, Boolean>) = true
    override fun simplify(varMap: Map<String, Boolean>) = this

    override fun computeVarWeights(baseWeight: Int) = mutableMapOf<String,Int>()
    override fun toString() = "true"
}

object ConstFalse : Formula {
    override fun eval(varMap: Map<String, Boolean>) = false
    override fun simplify(varMap: Map<String, Boolean>) = this

    override fun computeVarWeights(baseWeight: Int) = mutableMapOf<String,Int>()
    override fun toString() = "false"
}

class Not(val child: Formula) : Formula {
    override fun eval(varMap: Map<String, Boolean>) = !child.eval(varMap)

    override fun simplify(varMap: Map<String, Boolean>): Formula {
        return if(child is Not)
            child.child.simplify(varMap)
        else
            Not(child.simplify(varMap))
    }

    override fun computeVarWeights(baseWeight: Int) = child.computeVarWeights(baseWeight)
    override fun toString() = "!${child}"
}

abstract class BinOp(val left: Formula, val right: Formula) : Formula {
    override fun computeVarWeights(baseWeight: Int): MutableMap<String,Int> {
        val leftWeights = left.computeVarWeights(baseWeight / 2)
        val rightWeights = right.computeVarWeights(baseWeight / 2)

        rightWeights.forEach {
            val leftValue = leftWeights.getOrDefault(it.key, 0)
            leftWeights[it.key] = leftValue + it.value
        }

        return leftWeights
    }
}

class And(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) && right.eval(varMap)
    }

    override fun simplify(varMap: Map<String, Boolean>): Formula {
        val leftSim = left.simplify(varMap)

        if(leftSim == ConstFalse)
            return ConstFalse

        val rightSim = right.simplify(varMap)

        return when {
            rightSim == ConstFalse -> ConstFalse
            rightSim == ConstTrue && leftSim == ConstTrue -> ConstTrue
            else -> And(leftSim, rightSim)
        }
    }

    override fun toString() = "(${left} & ${right})"
}

class Or(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) || right.eval(varMap)
    }

    override fun simplify(varMap: Map<String, Boolean>): Formula {
        val leftSim = left.simplify(varMap)
        val rightSim = right.simplify(varMap)
        return when {
            leftSim == ConstTrue || rightSim == ConstTrue -> ConstTrue
            leftSim == ConstFalse -> rightSim
            rightSim == ConstFalse -> leftSim
            else -> Or(leftSim, rightSim)
        }
    }

    override fun toString() = "(${left} | ${right})"
}

class Equiv(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) == right.eval(varMap)
    }

    override fun simplify(varMap: Map<String, Boolean>): Formula {
        val leftSim = left.simplify(varMap)
        val rightSim = right.simplify(varMap)

        return when {
            leftSim == rightSim -> ConstTrue
            leftSim == ConstTrue && rightSim == ConstFalse -> ConstFalse
            leftSim == ConstFalse && rightSim == ConstTrue -> ConstFalse
            else -> Equiv(leftSim, rightSim)
        }
    }

    override fun toString() = "(${left} <=> ${right})"
}