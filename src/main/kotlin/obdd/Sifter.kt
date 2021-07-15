package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode

class Sifter(private val bdd: Bdd) {

    private var nodesByLevel: Array<MutableSet<BddNode>> = getNodesByLevel()

    fun sift() = siftFromLevel(0)

    fun siftFromLevel(startingLevel: Int) {
        var i = startingLevel
        val limit = bdd.variables.size - 1

        println("Size: ${computeSize()}")

        while(i < limit) {
            swapVariable(i)
            val evo = bdd.variables.sortedBy { it.number }.joinToString(", ")
            println("EVO: $evo")
            println("Size: ${computeSize()}")
            DotSerializer.writeToFile(bdd, "sift-$i.dot")
            i++
        }
    }

    private fun getNodesByLevel(): Array<MutableSet<BddNode>> {
        val res = Array(bdd.variables.size) { mutableSetOf<BddNode>() }
        bdd.nodes.filter { it != null && it.variable != null }.forEach { node ->
            res[node.variable.number].add(node)
        }
        return res
    }

    private fun swapVariable(i: Int) {
        if(i >= bdd.variables.size - 1) // can't swap last variable
            return

        // Probably inefficient, not sure if we care?
        val upperVariable = bdd.variables.first { it.number == i }
        val lowerVariable = bdd.variables.first { it.number == i + 1 }

        println("Swapping variable $upperVariable with $lowerVariable")

        nodesByLevel[i].toList().forEach { node ->
            swapSingleNode(node)
        }

        // Update variable numbers
        upperVariable.number = i + 1
        lowerVariable.number = i
    }

    private fun swapSingleNode(node: BddNode) {
        val currentLevel = node.variable.number
        val oneChildLevel = node.oneChild.variable?.number ?: bdd.variables.size
        val zeroChildLevel = node.zeroChild.variable?.number ?: bdd.variables.size

        if(oneChildLevel != currentLevel + 1 && zeroChildLevel != currentLevel + 1) { // Swap target level was optimized out for this node, nothing to swap
            nodesByLevel[currentLevel].remove(node)
            nodesByLevel[currentLevel + 1].add(node)
            return
        }

        val lowerVariable = if(oneChildLevel == currentLevel + 1) node.oneChild.variable else node.zeroChild.variable

        // Build reference nodes with the correct children

        val newOne = BddNode(node.variable)
        val newZero = BddNode(node.variable)

        if(oneChildLevel == currentLevel + 1) {
            newOne.oneChild = node.oneChild.oneChild
            newZero.oneChild = node.oneChild.zeroChild
        }
        else {
            newOne.oneChild = node.oneChild
            newZero.oneChild = node.oneChild
        }

        if(zeroChildLevel == currentLevel + 1) {
            newOne.zeroChild = node.zeroChild.oneChild
            newZero.zeroChild = node.zeroChild.zeroChild
        }
        else {
            newOne.zeroChild = node.zeroChild
            newZero.zeroChild = node.zeroChild
        }

        // Try to find already existing nodes equivalent to the reference ones to avoid introducing redundant nodes

        val oneReuseCandidate = nodesByLevel[currentLevel + 1].firstOrNull{ it.isEquivalent(newOne) }
        node.oneChild = oneReuseCandidate ?: newOne

        if(oneReuseCandidate == null)
            nodesByLevel[currentLevel + 1].add(newOne)

        val zeroReuseCandidate = nodesByLevel[currentLevel + 1].firstOrNull{ it.isEquivalent(newZero) }
        node.zeroChild = zeroReuseCandidate ?: newZero

        if(zeroReuseCandidate == null)
            nodesByLevel[currentLevel + 1].add(newZero)

        // Update variable of the now swapped upper node
        node.variable = lowerVariable
    }

    // This is probably way too inefficient, TODO
    // Implicitly does garbage collection, should probably be explicit
    private fun computeSize() : Int {
        val queue = mutableSetOf(bdd.rootNode)
        val usedNodes = mutableSetOf(bdd.rootNode)

        while(queue.isNotEmpty()) {
            val node = queue.first()
            queue.remove(node)
            usedNodes.add(node)

            if(node.oneChild != null)
                queue.add(node.oneChild)
            if(node.zeroChild != null)
                queue.add(node.zeroChild)
        }

        bdd.nodes.clear()
        bdd.nodes.addAll(usedNodes)
        return usedNodes.size
    }
}