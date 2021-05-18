package obdd.logic

import java.lang.Exception

interface Formula {
    fun eval(varMap: Map<String, Boolean>): Boolean
}

class Var(val name: String) : Formula {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return if(varMap.containsKey(name))
            varMap[name]!!
        else
            throw Exception("Variable assignment does not contain value for '$name'")
    }
}

object ConstTrue : Formula {
    override fun eval(varMap: Map<String, Boolean>) = true
}

object ConstFalse : Formula {
    override fun eval(varMap: Map<String, Boolean>) = false
}

class Not(val child: Formula) : Formula {
    override fun eval(varMap: Map<String, Boolean>) = !child.eval(varMap)
}

abstract class BinOp(val left: Formula, val right: Formula) : Formula

class And(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) && right.eval(varMap)
    }
}

class Or(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) || right.eval(varMap)
    }
}

class Impl(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return right.eval(varMap) || !left.eval(varMap)
    }
}
class Equiv(left : Formula, right: Formula) : BinOp(left, right) {
    override fun eval(varMap: Map<String, Boolean>): Boolean {
        return left.eval(varMap) == right.eval(varMap)
    }
}