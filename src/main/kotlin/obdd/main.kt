package obdd

import de.tu_darmstadt.rs.logictool.bdd.tools.BddReducer
import obdd.serializers.DotSerializer
import obdd.serializers.JsonSerializer

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("--") }
    val other = args.filter { !it.startsWith("--") }

    // Print help if no (or more than one) formula was given
    if(other.size != 1) {
        println("Usage: obdd-gen [flags] [formula]")
        println("--naive\tUse naive function evaluation (rather than syntactic simplification)")
        println("--reduce\tProduce a fully reduced bdd")
        println("--quasireduce\tProduce a quasi-reduced bdd")
        println("--json\tOutput a json representation rather than a dot graph")
        println("--order=[none|weight|a,b,c]\tSpecify variable evaluation order (default: weight)")
        println("--out=[path]\tWrite output to this location (default: bdd.dot / bdd.json)")
        return
    }

    if(flags.contains("--blif")) {
        println(FuseRecurseMapper.mapBLIF(other.first()).joinToString("\n\n"))
        return
    }

    // Parse formula
    val formula = FormulaConverter.parse(other.first())
    println("Parsed input formula: $formula")

    if(flags.contains("--map")) {
        println(FuseRecurseMapper.mapFormula(formula, "output").joinToString("\n"))
        return
    }

    // Compute order according to flag / verify given order contains all required vars
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
    val reduce = flags.contains("--reduce")
    val quasireduce = flags.contains("--quasireduce")
    val jsonOut = flags.contains("--json")
    val filename = when(val outFlag = flags.firstOrNull{ it.startsWith("--out=") }) {
        null -> if(jsonOut) "bdd.json" else "bdd.dot"
        else -> outFlag.removePrefix("--out=")
    }

    // Create bdd from formula
    val bdd = when {
        naiveEval -> NaiveBddBuilder().create(formula, order)
        quasireduce -> QrbddBuilder.create(formula, order)
        else -> BddBuilder.create(formula, order)
    }

    // Reduce, if necessary
    if(quasireduce || reduce)
        BddReducer().reduceBdd(bdd, quasireduce)

    if(flags.contains("--sift")) {
        val sifter = Sifter(bdd)
        sifter.sift()
    }

    // Write result to file
    if(jsonOut)
        JsonSerializer.writeToFile(bdd, filename)
    else
        DotSerializer.writeToFile(bdd, filename)

    println("Wrote generated BDD to file '$filename'")
}