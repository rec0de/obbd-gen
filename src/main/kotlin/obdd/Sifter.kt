package obdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode
import de.tu_darmstadt.rs.logictool.common.representation.Variable

class Sifter(private val bdd: Bdd) {

    private var nodesByLevel: Array<MutableSet<BddNode>> = getNodesByLevel(bdd)

    fun sift() = siftFromLevel(0)

    fun siftFromLevel(startingLevel: Int) {
        val siftOrder = getVariableSiftOrder(startingLevel)
        siftOrder.forEach {
            findOptimalVarPosition(startingLevel, it.number)
        }
    }

    private fun getVariableSiftOrder(startingLevel: Int): List<Variable> {
        val baseList = bdd.variables.filter { it.number >= startingLevel }

        // Compute the list of variables to be sifted, ordered descending by the number of nodes for that variable
        return baseList.sortedByDescending { nodesByLevel[it.number].size }
    }

    private fun findOptimalVarPosition(lowerLimit: Int, variableLevel: Int) {
        val upperLimit = bdd.variables.size - 1

        // Sift in the direction with fewer levels first, since we will have to do these swaps twice to undo
        val upwardsFirst = variableLevel - lowerLimit < upperLimit - variableLevel
        val upwardsRange = (variableLevel-1 downTo lowerLimit)
        val downwardsRange = (variableLevel until upperLimit)

        val firstRange = if(upwardsFirst) upwardsRange else downwardsRange
        val secondRange = if(upwardsFirst) downwardsRange else upwardsRange
        val restorePoint = if(upwardsFirst) lowerLimit else upperLimit
        val endPoint = if(upwardsFirst) upperLimit else lowerLimit

        var bestSize = computeSize()
        var bestPosition = variableLevel

        println("Sifting variable at level $variableLevel, base size is $bestSize")

        // Perform first direction sift
        for(i in firstRange) {
            swapVariable(i)
            val evo = bdd.variables.sortedBy { it.number }.joinToString(", ")
            val size = computeSize()

            println("EVO: $evo")
            println("Size: $size")

            if(size < bestSize) {
                bestSize = size
                bestPosition = if(upwardsFirst) i else i + 1
                println("New best!")
            }
        }

        // Undo first sift
        println("Resetting $restorePoint to $variableLevel")
        moveVariableTo(restorePoint, variableLevel)

        // Perform second direction sift
        println("Second sift: $secondRange")
        for(i in secondRange) {
            swapVariable(i)
            val evo = bdd.variables.sortedBy { it.number }.joinToString(", ")
            val size = computeSize()

            println("EVO: $evo")
            println("Size: $size")

            if(size < bestSize) {
                bestSize = size
                bestPosition = if(upwardsFirst) i + 1 else i
                println("New best!")
            }
        }

        println("Determined best position $bestPosition for optimal size $bestSize")
        moveVariableTo(endPoint, bestPosition)

        val evo = bdd.variables.sortedBy { it.number }.joinToString(", ")
        val size = computeSize()
        println("EVO: $evo")
        println("Size: $size")
    }

    private fun moveVariableTo(startIndex: Int, targetIndex: Int) {
        val range = if(startIndex > targetIndex)
                (startIndex-1 downTo targetIndex)
            else
                (startIndex until targetIndex)

        for(i in range) {
            computeSize()
            swapVariable(i)
        }
    }

    private fun swapVariable(i: Int) {
        if(i >= bdd.variables.size - 1) // can't swap last variable
            return

        // Probably inefficient, not sure if we care?
        val upperVariable = bdd.variables.first { it.number == i }
        val lowerVariable = bdd.variables.first { it.number == i + 1 }

        //println("Swapping variable $upperVariable with $lowerVariable")

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

        // We'll re-create nodesByLevel while we're at it to remove orphans from there
        nodesByLevel.forEach { it.clear() }

        while(queue.isNotEmpty()) {
            val node = queue.first()
            queue.remove(node)
            usedNodes.add(node)

            if(node.oneChild != null) {
                queue.add(node.oneChild)
                nodesByLevel[node.variable.number].add(node)
            }
            if(node.zeroChild != null)
                queue.add(node.zeroChild)
        }

        bdd.nodes.clear()
        bdd.nodes.addAll(usedNodes)
        return usedNodes.size
    }
}