package obdd

import de.tu_darmstadt.rs.logictool.bdd.tools.BddFactory

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("--") }
    val other = args.filter { !it.startsWith("--") }

    if(other.size != 1) {
        println("Usage: obdd-gen [flags] [formula]")
        return
    }

    val formula = other.first()
    val parsed = FormulaConverter.parse(formula)

    println(parsed)

    val bddFactory = BddFactory()
    val bdd = bddFactory.create(FormulaBasedFunction(parsed))
}