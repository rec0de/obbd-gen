package obdd.bdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode

object QrbddReducer {
    fun reduceBdd(bdd: Bdd) {

        // order nodes by variable
        val nodes: Array<ArrayList<BddNode?>> = Array(bdd.variables.size){ arrayListOf() }
        for (node in bdd.nodes) {
            val variable = node.variable
            if (variable != null) nodes[variable.number].add(node)
        }

        // reduce bottom up
        for (i in nodes.indices.reversed()) {
            val currentNodes = nodes[i]
            val parentMap: MutableMap<BddNode, MutableList<BddNode>> = mutableMapOf()

            if(i > 0) {
                nodes[i-1].forEach { parent ->
                    parentMap.putIfAbsent(parent!!.zeroChild, ArrayList())
                    parentMap.putIfAbsent(parent.oneChild, ArrayList())
                    parentMap[parent.zeroChild]!!.add(parent)
                    parentMap[parent.oneChild]!!.add(parent)
                }
            }

            // compare nodes pairwise and delete redundant ones
            val outerIt: ListIterator<BddNode?> = currentNodes.listIterator()
            while (outerIt.nextIndex() < currentNodes.size - 1) {
                val outerNode = outerIt.next() ?: continue // already deleted

                val innerIt = currentNodes.listIterator(outerIt.nextIndex())
                while (innerIt.hasNext()) {
                    val innerNode = innerIt.next() ?: continue // already deleted

                    // comparison
                    if (outerNode.zeroChild !== innerNode.zeroChild) continue
                    if (outerNode.oneChild !== innerNode.oneChild) continue

                    // nodes are equivalent -> link all parents to outer node
                    // parentMap.get(outerNode).addAll(parentMap.get(innerNode));
                    // note: if we wanted to keep the parent map consistent, we'd need the above line
                    //       however, we don't, since the modified entry would never be accessed later
                    for (parent in parentMap[innerNode]!!) {
                        if (parent.zeroChild === innerNode) parent.zeroChild = outerNode
                        if (parent.oneChild === innerNode) parent.oneChild = outerNode
                    }

                    // update links to children
                    innerNode.zeroChild = null
                    innerNode.oneChild = null

                    // delete node
                    innerIt.set(null)
                }
            }
        }

        // update node list
        val bddNodes = bdd.nodes
        bddNodes.clear()
        bddNodes.add(bdd.zeroNode)
        bddNodes.add(bdd.oneNode)
        for (list in nodes) {
            for (node in list) {
                if (node != null) bddNodes.add(node)
            }
        }
    }
}