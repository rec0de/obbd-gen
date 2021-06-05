package obdd.test

import de.tu_darmstadt.rs.logictool.bdd.tools.BddReducer
import obdd.BddBuilder
import obdd.FormulaConverter
import obdd.NaiveBddBuilder
import obdd.logic.Formula
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
fun main() {
    val formulas = File("benchmark/500.txt").readLines()

    val minLength = formulas.minByOrNull { it.length }!!.length
    val maxLength = formulas.maxByOrNull { it.length }!!.length
    val avgLength = formulas.sumBy { it.length } / formulas.size

    println("Loaded ${formulas.size} formulas")
    println("Formula length (min/avg/max): $minLength/$avgLength/$maxLength")

    val parsed = formulas.map{ FormulaConverter.parse(it) }
    val factory = NaiveBddBuilder()
    val reducer = BddReducer()

    val avgVars = parsed.sumBy { it.computeVarWeights(Int.MAX_VALUE).keys.size }.toDouble() / parsed.size
    println("Avg num of vars $avgVars")

    naiveReduced(factory, reducer, parsed)
    synSimReduced(reducer, parsed)
    naive(factory, parsed)
    synSim(parsed)
}

@ExperimentalTime
fun naive(factory: NaiveBddBuilder, parsed: List<Formula>) {
    var naiveNodes = 0
    val naiveTime = measureTime {
        parsed.forEach { formula ->
            val order = formula.computeVarWeights(Int.MAX_VALUE).toList().sortedByDescending { it.second }.map { it.first }
            val bdd = factory.create(formula, order)
            naiveNodes += bdd.nodes.size
        }
    }

    println("Naive generation")
    println("Took ${naiveTime.inMilliseconds} ms")
    println("Created $naiveNodes nodes total")
}

@ExperimentalTime
fun synSim(parsed: List<Formula>) {
    var nodes = 0
    val time = measureTime {
        parsed.forEach { formula ->
            val order = formula.computeVarWeights(Int.MAX_VALUE).toList().sortedByDescending { it.second }.map { it.first }
            val bdd = BddBuilder.create(formula, order)
            nodes += bdd.nodes.size
        }
    }

    println("Syntactic simplification")
    println("Took ${time.inMilliseconds} ms")
    println("Created $nodes nodes total")
}

@ExperimentalTime
fun naiveReduced(factory: NaiveBddBuilder, reducer: BddReducer, parsed: List<Formula>) {
    var nodes = 0
    val time = measureTime {
        parsed.forEach { formula ->
            val order = formula.computeVarWeights(Int.MAX_VALUE).toList().sortedByDescending { it.second }.map { it.first }
            val bdd = factory.create(formula, order)
            reducer.reduceBdd(bdd)
            nodes += bdd.nodes.size
        }
    }

    println("Naive generation, reduced")
    println("Took ${time.inMilliseconds} ms")
    println("Created $nodes nodes total")
}

@ExperimentalTime
fun synSimReduced(reducer: BddReducer, parsed: List<Formula>) {
    var nodes = 0
    val time = measureTime {
        parsed.forEach { formula ->
            val order = formula.computeVarWeights(Int.MAX_VALUE).toList().sortedByDescending { it.second }.map { it.first }
            val bdd = BddBuilder.create(formula, order)
            reducer.reduceBdd(bdd)
            nodes += bdd.nodes.size
        }
    }

    println("Syntactic simplification, reduced")
    println("Took ${time.inMilliseconds} ms")
    println("Created $nodes nodes total")
}