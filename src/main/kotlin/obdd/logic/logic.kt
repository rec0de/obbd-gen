package obdd.logic

import java.lang.Exception

interface Formula {
    fun eval(varMap: Map<String, Boolean>): Boolean
    fun collectVars(): Set<String>
}

class Var(val name: String) : Formula {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return if(varMap.containsKey(name))
            varMap[name]!!
        else
            throw Exception("Variable assignment does not contain value for '$name'")
    }

    override fun collectVars() = setOf(name)
    override fun toString() = name
}

object ConstTrue : Formula {
    override fun eval(varMap: Map<String, Boolean>) = true

    override fun collectVars() = emptySet<String>()
    override fun toString() = "true"
}

object ConstFalse : Formula {
    override fun eval(varMap: Map<String, Boolean>) = false

    override fun collectVars() = emptySet<String>()
    override fun toString() = "false"
}

class Not(val child: Formula) : Formula {
    override fun eval(varMap: Map<String, Boolean>) = !child.eval(varMap)

    override fun collectVars() = child.collectVars()
    override fun toString() = "!${child}"
}

abstract class BinOp(val left: Formula, val right: Formula) : Formula {
    override fun collectVars() = left.collectVars() + right.collectVars()
}

class And(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) && right.eval(varMap)
    }
    override fun toString() = "(${left} & ${right})"
}

class Or(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) || right.eval(varMap)
    }
    override fun toString() = "(${left} | ${right})"
}

class Impl(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return right.eval(varMap) || !left.eval(varMap)
    }
    override fun toString() = "(${left} -> ${right})"
}

class Equiv(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) == right.eval(varMap)
    }
    override fun toString() = "(${left} <=> ${right})"
}