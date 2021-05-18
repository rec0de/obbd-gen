package de.tu_darmstadt.rs.logictool.bdd.tools;

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd;
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode;
import de.tu_darmstadt.rs.logictool.common.representation.Variable;

import java.util.*;

/**
 * Reduces BDDs by eliminating redundant nodes.
 */
public class BddReducer {

    /**
     * Reduces the given BDD.
     *
     * @param bdd  The BDD to reduce.
     */
    public void reduceBdd(Bdd bdd) {

        // order nodes by variable
        ArrayList<BddNode>[] nodes = new ArrayList[bdd.getVariables().length];
        for (int i = 0; i < nodes.length; i++)
            nodes[i] = new ArrayList<>();
        for (BddNode node : bdd.getNodes()) {
            Variable var = node.getVariable();
            if (var != null)
                nodes[var.getNumber()].add(node);
        }

        // reduce bottom up
        for (int i = nodes.length - 1; i >= 0; i--) {
            ArrayList<BddNode> currentNodes = nodes[i];

            // delete nodes with same child for 0 and 1
            ListIterator<BddNode> it = currentNodes.listIterator();
            while (it.hasNext()) {
                BddNode node = it.next();
                if (node.getOneChild() == node.getZeroChild()) {

                    if (node == bdd.getRootNode()) {
                        // set new root node
                        bdd.setRootNode(node.getZeroChild());
                    } else {
                        // update links to parents
                        for (BddNode parent : new ArrayList<>(node.getParents())) {
                            if (parent.getZeroChild() == node)
                                parent.setZeroChild(node.getZeroChild());
                            else
                                parent.setOneChild(node.getOneChild());
                        }
                    }

                    // update links to children
                    node.setZeroChild(null);
                    node.setOneChild(null);

                    // delete node
                    it.set(null);
                }
            }

            // compare nodes pairwise and delete redundant ones
            ListIterator<BddNode> outerIt = currentNodes.listIterator();
            while (outerIt.nextIndex() < currentNodes.size() - 1) {
                BddNode outerNode = outerIt.next();
                if (outerNode == null)  // already deleted
                    continue;

                ListIterator<BddNode> innerIt = currentNodes.listIterator(outerIt.nextIndex());
                while (innerIt.hasNext()) {
                    BddNode innerNode = innerIt.next();
                    if (innerNode == null)  // already deleted
                        continue;

                    // comparison
                    if (outerNode.getZeroChild() != innerNode.getZeroChild())
                        continue;
                    if (outerNode.getOneChild() != innerNode.getOneChild())
                        continue;

                    // nodes are equivalent -> link all parents to outer node
                    for (BddNode parent : new ArrayList<>(innerNode.getParents())) {
                        if (parent.getZeroChild() == innerNode)
                            parent.setZeroChild(outerNode);
                        if (parent.getOneChild() == innerNode)
                            parent.setOneChild(outerNode);
                    }

                    // update links to children
                    innerNode.setZeroChild(null);
                    innerNode.setOneChild(null);

                    // delete node
                    innerIt.set(null);
                }
            }
        }

        // update node list
        List<BddNode> bddNodes = bdd.getNodes();
        bddNodes.clear();
        bddNodes.add(bdd.getZeroNode());
        bddNodes.add(bdd.getOneNode());
        for (ArrayList<BddNode> list : nodes) {
            for (BddNode node : list) {
                if (node != null)
                    bddNodes.add(node);
            }
        }
    }
}
