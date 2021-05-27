package obdd

import de.tu_darmstadt.rs.logictool.bdd.tools.BddFactory

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("--") }
    val other = args.filter { !it.startsWith("--") }

    if(other.size != 1) {
        println("Usage: obdd-gen [flags] [formula]")
        println("--naive\tUse naive function evaluation (rather than syntactic simplification)")
        println("--order=[none|weight|a,b,c]\tSpecify variable evaluation order (default: weight)")
        return
    }

    val formula = FormulaConverter.parse(other.first())
    println("Parsed input formula: $formula")

    val varWeights = formula.computeVarWeights(Int.MAX_VALUE)

    val order = when(val orderFlag = flags.firstOrNull{ it.startsWith("--order=") }) {
        "--order=none" -> varWeights.keys.toList()
        "--order=weight", null -> varWeights.toList().sortedByDescending { it.second }.map { it.first }
        else -> {
            val varList = orderFlag.removePrefix("--order=").split(",")
            if(varWeights.keys.any{ !varList.contains(it) })
                throw RuntimeException("Given variable order '${varList.joinToString(", ")}' does not contain all variables '${varWeights.keys.joinToString(", ")}'")
            varList
        }
    }

    println("Using variable order: '${order.joinToString(", ")}'")

    val naiveEval = flags.contains("--naive")

    val bdd = if(naiveEval)
            BddFactory().create(FormulaBasedFunction(formula), genOrder(order))
        else
            BddBuilder.create(formula, order)

    println(bdd) // TODO: produce usable representation
}