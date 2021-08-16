package de.tu_darmstadt.rs.logictool.bdd.representation;

import de.tu_darmstadt.rs.logictool.common.representation.Variable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A BDD for representing a boolean function.
 */
public class Bdd implements Iterable<BddNode> {

    /**
     * All nodes of the BDD.
     */
    private ArrayList<BddNode> nodes;

    /**
     * The root node.
     */
    private BddNode rootNode = null;

    /**
     * Defines the order of variables in the BDD.
     */
    private final Variable[] variables;

    /**
     * The constant 0 node of the BDD.
     */
    private final BddNode zeroNode;

    /**
     * The constant 1 node of the BDD.
     */
    private final BddNode oneNode;


    /**
     * Creates a new BDD.
     *
     * @param variables  Defines the order of variables in the BDD.
     */
    public Bdd(Variable[] variables) {
        this.variables = variables;
        this.nodes = new ArrayList<>();
        this.zeroNode = new BddNode(null);
        this.oneNode = new BddNode(null);
        nodes.add(zeroNode);
        nodes.add(oneNode);
    }

    public void clear() {
        rootNode = null;
        nodes = null;
    }

    /**
     * @return  All nodes of the BDD.
     */
    public List<BddNode> getNodes() {
        return nodes;
    }

    @Override
    public Iterator<BddNode> iterator() {
        return nodes.iterator();
    }

    /**
     * @return  The root node.
     */
    public BddNode getRootNode() {
        return rootNode;
    }

    /**
     * @param rootNode  The root node.
     */
    public void setRootNode(BddNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * @return  Defines the order of variables in the BDD.
     */
    public Variable[] getVariables() {
        return variables;
    }

    /**
     * @return  The constant 0 node of the BDD.
     */
    public BddNode getZeroNode() {
        return zeroNode;
    }

    /**
     * @return  The constant 1 node of the BDD.
     */
    public BddNode getOneNode() {
        return oneNode;
    }
}
