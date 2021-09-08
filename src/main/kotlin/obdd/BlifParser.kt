package obdd

import obdd.logic.*
import java.io.File

object BlifParser {

    // Maps a signal to the input wires and gate specs that define its value
    private val gates: MutableMap<String, Pair<List<String>, List<String>>> = mutableMapOf()

    // Cache of already evaluated formulas
    private val wireFormulas: MutableMap<String, Formula> = mutableMapOf()

    private var inputVars: MutableSet<String> = mutableSetOf()

    // The return type is a bit messy, but it's basically: (input vars, {output vars -> output formula})
    fun parse(filename: String) : Pair<Collection<String>, List<Pair<String,Formula>>> {
        gates.clear()
        wireFormulas.clear()
        inputVars.clear()
        val outputVars: MutableList<String> = mutableListOf()

        val lines = normalizeLines(File(filename).readLines())

        var i = 0
        while(i < lines.size) {
            val line = lines[i]
            i += 1

            when {
                line.startsWith(".names") -> {
                    val wireNames = line.removePrefix(".names ").trim().split(' ')
                    val output = wireNames.last()
                    val inputs = wireNames.dropLast(1)
                    val gateSpec = mutableListOf<String>()

                    while(i < lines.size && !lines[i].startsWith('.')){
                        gateSpec.add(lines[i])
                        i += 1
                    }
                    gates[output] = Pair(inputs, gateSpec)
                }
                line.startsWith(".inputs") -> inputVars.addAll(line.removePrefix(".inputs ").trim().split(' '))
                line.startsWith(".outputs") -> outputVars.addAll(line.removePrefix(".outputs ").trim().split(' '))
                line.startsWith(".latch") -> {
                    // We 'ignore' latches by interpreting them as independent input/output pairs
                    val parts = line.removePrefix(".latch").trim().split(' ').filter { it.isNotEmpty() }
                    outputVars.add(parts[0])
                    inputVars.add(parts[1])
                }
                line.startsWith(".model") || line.startsWith(".wire_load_slope") -> { } // We don't really care about the model name or wire models
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
        // Honestly I'm not super comfortable with this cache as it introduces lots of aliasing, but it seems to work okay
        if(wireFormulas.containsKey(wireName)) {
            return wireFormulas[wireName]!!
        }

        // If no cached evaluation exists, compute formula the hard way
        val gateInfo = gates[wireName]!!
        val inputs = gateInfo.first
        val table = gateInfo.second

        val formula = if(inputs.isEmpty())
                if(table.isEmpty() || table.first() != "1") ConstFalse else ConstTrue
            else
                table.fold(ConstFalse as Formula){ acc, line -> Or(acc, gateLineToFormula(inputs, line)) }.simplify(simplifyNonce++) // OR over all lines in the table

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
        val noComments = lines.map{ it.replaceAfter('#', "").trim() }.filter { it != "#" && it.isNotBlank() }

        // Resolve line concatenations
        var buffer = ""
        val res = mutableListOf<String>()
        noComments.forEach {
            if(it.endsWith('\\')) {
                buffer += it.removeSuffix("\\")
                if(!buffer.endsWith(" "))
                    buffer += " "
            }
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