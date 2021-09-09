package obdd.bdd

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode

object BddReducer {
    fun reduceBdd(bdd: Bdd) {
        val parentMap: MutableMap<BddNode, MutableList<BddNode>> = HashMap()
        for (node in bdd) {
            if (node.zeroChild == null) continue
            parentMap.putIfAbsent(node.zeroChild, ArrayList())
            parentMap.putIfAbsent(node.oneChild, ArrayList())
            parentMap[node.zeroChild]!!.add(node)
            parentMap[node.oneChild]!!.add(node)
        }

        // order nodes by variable
        val nodes: Array<ArrayList<BddNode?>> = Array(bdd.variables.size){ arrayListOf() }
        for (node in bdd.nodes)
            if (node.variable != null) nodes[node.variable.number].add(node)

        // reduce bottom up
        for (i in nodes.indices.reversed()) {
            val currentNodes = nodes[i]

            // delete nodes with same child for 0 and 1
            currentNodes.removeIf { node ->
                node!!
                if (node.oneChild === node.zeroChild) {
                    if (node === bdd.rootNode) {
                        bdd.rootNode = node.zeroChild // set new root node
                    } else {
                        // update links to parents
                        for (parent in parentMap.getOrDefault(node, mutableListOf())) {
                            parentMap[node.zeroChild]!!.remove(node)
                            parentMap[node.zeroChild]!!.add(parent)
                            if (parent.zeroChild === node)
                                parent.zeroChild = node.zeroChild
                            else
                                parent.oneChild = node.zeroChild
                        }
                    }
                    true
                }
                else
                    false
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
                        if (parent.zeroChild === innerNode)
                            parent.zeroChild = outerNode
                        if (parent.oneChild === innerNode)
                            parent.oneChild = outerNode
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
        bdd.nodes.clear()
        bdd.nodes.add(bdd.zeroNode)
        bdd.nodes.add(bdd.oneNode)
        bdd.nodes.addAll(nodes.flatMap { it }.filterNotNull())
    }
}