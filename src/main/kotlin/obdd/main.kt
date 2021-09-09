package obdd

import de.tu_darmstadt.rs.logictool.bdd.tools.BddReducer
import obdd.bdd.*
import obdd.serializers.DotSerializer
import obdd.serializers.JsonSerializer
import java.io.File

var logLevel: Int = 0
var simplifyNonce: Int = 0

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
        println("--order=[none|weight|count|subgraph|a,b,c]\tSpecify variable evaluation order (default: weight)")
        println("--out=[path]\tWrite output to this location (default: bdd.dot / bdd.json)")
        println("\nobdd-gen --blif-map [flags] [blif file]")
        println("--out=[path]\tWrite output to this location (default: mapped.blif)")
        println("--loglevel=[0-4]\tLog less (4) or more (0) progress information")
        return
    }

    logLevel = when(val logFlag = flags.firstOrNull{ it.startsWith("--loglevel=") }) {
        null -> 0
        else -> logFlag.removePrefix("--loglevel=").toInt()
    }

    if(flags.contains("--blif-map")) {
        val res = FuseRecurseMapper.mapBLIF(other.first())
        val filename = when(val outFlag = flags.firstOrNull{ it.startsWith("--out=") }) {
            null -> "mapped.blif"
            else -> outFlag.removePrefix("--out=")
        }
        File(filename).writeText(res)
        return
    }

    // Parse formula
    val formula = FormulaConverter.parse(other.first())
    println("Parsed input formula: $formula")

    // Compute order according to flag / verify given order contains all required vars
    val heuristics = BddOrderHeuristics(formula)
    val order = when(val orderFlag = flags.firstOrNull{ it.startsWith("--order=") }) {
        "--order=none" -> heuristics.variables.toList()
        "--order=count" -> heuristics.varCount()
        "--order=subgraph" -> heuristics.subGraphComplexity()
        "--order=weight", null -> heuristics.varWeight()
        else -> {
            val varList = orderFlag.removePrefix("--order=").split(",")
            if(heuristics.variables.any{ !varList.contains(it) })
                throw RuntimeException("Given variable order '${varList.joinToString(", ")}' does not contain all variables '${heuristics.variables.joinToString(", ")}'")
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