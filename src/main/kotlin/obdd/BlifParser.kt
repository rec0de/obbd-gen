package obdd

import obdd.logic.*
import java.io.File
import java.lang.Exception

object BlifParser {

    // Maps a signal to the input wires and gate specs that define its value
    private val gates: MutableMap<String, Pair<List<String>, List<String>>> = mutableMapOf()

    // Cache of already evaluated formulas
    private val wireFormulas: MutableMap<String, Formula> = mutableMapOf()

    private var inputVars: Set<String> = emptySet()

    // The return type is a bit messy, but it's basically: (input vars, {output vars -> output formula})
    fun parse(filename: String) : Pair<Collection<String>, List<Pair<String,Formula>>> {
        gates.clear()
        wireFormulas.clear()
        inputVars = emptySet()
        var outputVars: List<String> = emptyList()

        val lines = normalizeLines(File(filename).readLines())

        var i = 0
        while(i < lines.size) {
            val line = lines[i]
            i += 1

            when {
                line.startsWith(".names") -> {
                    val wireNames = line.removePrefix(".names ").split(' ')
                    val output = wireNames.last()
                    val inputs = wireNames.dropLast(1)
                    val gateSpec = mutableListOf<String>()

                    while(i < lines.size && !lines[i].startsWith('.')){
                        gateSpec.add(lines[i])
                        i += 1
                    }
                    gates[output] = Pair(inputs, gateSpec)
                }
                line.startsWith(".inputs") -> inputVars = line.removePrefix(".inputs ").split(' ').toSet()
                line.startsWith(".outputs") -> outputVars = line.removePrefix(".outputs ").split(' ')
                line.startsWith(".model") -> { } // We don't really care about the model name
                line.startsWith(".end") -> break // We'll only read the first model of the file (TODO?)
                else -> throw Exception("BLIF parse error: Unrecognized command '$line'")
            }
        }

        /*println("Inputs: ${inputVars.joinToString(", ")}")
        println("Outputs: ${outputVars.joinToString(", ")}")*/

        return Pair(inputVars, outputVars.map { Pair(it, gateToFormula(it)) })
    }

    private fun gateToFormula(wireName: String) : Formula {
        // Input variables can directly be translated to variables
        if(inputVars.contains(wireName))
            return Var(wireName)

        // If we have previously evaluated this sub-formula, just return a reference
        // Caching can cause lots of headaches later because the AST is not a proper tree anymore, so we'll clone formulas on re-use and hope that cloning is much faster than re-parsing for now
        if(wireFormulas.containsKey(wireName))
            return wireFormulas[wireName]!!.simplify() // simplify clones implicitly, maybe worth adding a dedicated clone function?

        // If no cached evaluation exists, compute formula the hard way
        val gateInfo = gates[wireName]!!
        val inputs = gateInfo.first
        val table = gateInfo.second

        val formula = if(inputs.isEmpty() && table.first() == "1")
                ConstTrue
            else
                table.fold(ConstFalse as Formula){ acc, line -> Or(acc, gateLineToFormula(inputs, line)) }.simplify() // OR over all lines in the table

        wireFormulas[wireName] = formula

        return formula
    }

    private fun gateLineToFormula(varNames: List<String>, line: String) : Formula {
        if(!line.endsWith(" 1"))
            throw Exception("BLIF parse error: For now, only on-set specification is allowed")

        return line.removeSuffix(" 1").mapIndexed { index, c ->
            when(c) {
                '1' -> gateToFormula(varNames[index])
                '0' -> Not(gateToFormula(varNames[index]))
                else -> null
            }
        }.filterNotNull().fold(ConstTrue as Formula){ acc, lit -> And(acc, lit)} // AND over all literals
    }

    private fun normalizeLines(lines: List<String>) : List<String> {
        // Remove comments and excess whitespace
        val noComments = lines.map{ it.replaceAfter('#', "").trim() }.filter { it.isNotBlank() }

        // Resolve line concatenations
        var buffer = ""
        val res = mutableListOf<String>()
        noComments.forEach {
            if(it.endsWith('\\'))
                buffer += it.removeSuffix("\\")
            else {
                res.add(buffer + it)
                buffer = ""
            }
        }

        // There shouldn't be any incomplete line concatenations
        if(buffer != "")
            throw Exception("BLIF parse error: Expected line continuation after '\\' but got end of input")

        return res
    }
}