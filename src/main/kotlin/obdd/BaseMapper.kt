package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode
import obdd.logic.LutGraph
import obdd.serializers.DotSerializer
import java.lang.Integer.max
import kotlin.math.ceil
import kotlin.math.log2

object BaseMapper : LutMapStrategy() {

    override fun mapQRBDD(bdd: Bdd): LutGraph {
        Sifter(bdd).sift()
        val nodesByLevel = getNodesByLevel(bdd)
        DotSerializer.writeToFile(bdd, "baseMapper-in.dot")
        for(i in (0 until bdd.variables.size)) {
            println("Cut at $i, cost ${scoreCut(bdd, nodesByLevel, i)}")
        }

        throw Exception("yea not done implementing this")
    }

    private fun scoreCut(bdd: Bdd, nodesByLevel: Array<MutableSet<BddNode>>, cutLevel: Int) : Int {
        val lutCap = 5
        val cutWidth = nodesByLevel[cutLevel].size
        val heightAboveCut = cutLevel
        val selectSignalWidth = ceil(log2(cutWidth.toDouble())).toInt()
        val heightBelowCut = max((bdd.variables.size - cutLevel) - (lutCap - selectSignalWidth), 0)

        println("Width: $cutWidth, Height above: $heightAboveCut, Signal width: $selectSignalWidth, Height below: $heightBelowCut")
        println("LUT Height below: ~${ceil(heightBelowCut.toDouble() / lutCap).toInt()}")
        if(heightBelowCut != 0)
            println("Out multiplicity: ${nodesByLevel[bdd.variables.size - heightBelowCut].size}")

        val estimatedUpperCost = selectSignalWidth * ceil(heightAboveCut.toDouble() / lutCap).toInt()
        val estimatedLowerCost = if(heightBelowCut == 0) 0 else ceil((heightBelowCut.toDouble() / lutCap)).toInt() * nodesByLevel[bdd.variables.size - heightBelowCut].size

        return estimatedUpperCost + estimatedLowerCost
    }

}