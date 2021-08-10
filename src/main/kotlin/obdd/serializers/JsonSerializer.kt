package obdd.serializers

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object JsonSerializer {
    fun writeToFile(bdd: Bdd, filename: String) {
        val indexMap = bdd.nodes.withIndex().associate { Pair(it.value, it.index) }

        BufferedWriter(OutputStreamWriter(FileOutputStream(filename))).use { out ->
            out.write("{\n")
            out.write("\"root\": ${indexMap[bdd.rootNode]},\n")
            out.write("\"zeroNode\": ${indexMap[bdd.zeroNode]},\n")
            out.write("\"oneNode\": ${indexMap[bdd.oneNode]},\n")
            out.write("\"nodes\":\n\t[\n")

            val nodeBlock = bdd.nodes.joinToString(",\n") {
                val label = when (it) {
                    bdd.zeroNode -> "0"
                    bdd.oneNode -> "1"
                    else -> it.variable.name
                }
                val zero = indexMap[it.zeroChild]
                val one = indexMap[it.oneChild]
                "\t\t{\"variable\": \"$label\", \"zeroChild\": $zero, \"oneChild\": $one}"
            }

            out.write(nodeBlock)
            out.write("\n\t]\n")
            out.write("}")
        }
    }
}