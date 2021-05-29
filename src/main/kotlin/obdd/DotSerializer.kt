package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object DotSerializer {
    fun writeToFile(bdd: Bdd, filename: String) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(filename))).use { out ->
            out.write("digraph {\n")
            out.write("name [label = f] [shape = box];\n")

            val indexMap = mutableMapOf<BddNode,Int>()

            bdd.nodes.withIndex().forEach {
                val label = when(it.value) {
                    bdd.zeroNode -> "0"
                    bdd.oneNode -> "1"
                    else -> it.value.variable.name
                }
                val shape = when(it.value) {
                    bdd.zeroNode, bdd.oneNode -> "box"
                    else -> "circle"
                }
                out.write("node${it.index} [label = $label] [shape = $shape];\n")
                indexMap[it.value] = it.index
            }

            out.write("name -> node${indexMap[bdd.rootNode]!!};\n")

            bdd.nodes.forEach {
                if(it.zeroChild != null)
                    out.write("node${indexMap[it]} -> node${indexMap[it.zeroChild]} [style=dotted];\n")
                if(it.oneChild != null)
                    out.write("node${indexMap[it]} -> node${indexMap[it.oneChild]};\n")
            }

            out.write("}")
        }
    }
}