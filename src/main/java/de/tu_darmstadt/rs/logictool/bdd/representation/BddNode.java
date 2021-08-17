package de.tu_darmstadt.rs.logictool.bdd.representation;

import de.tu_darmstadt.rs.logictool.common.representation.Variable;

/**
 * A node of a BDD.
 */
public class BddNode {

    /**
     * The zero child of the node.
     */
    private BddNode zeroChild;

    /**
     * The one child of the node.
     */
    private BddNode oneChild;

    /**
     * The decision variable of the node.
     */
    private Variable variable;

    /**
     * Creates a new BDD node.
     *
     * @param variable  The decision variable of the node.
     */
    public BddNode(Variable variable) {
        this.variable = variable;
    }
    
    /**
     * @return  The zero child of the node.
     */
    public BddNode getZeroChild() {
        return zeroChild;
    }

    /**
     * @param zeroChild  The zero child of the node.
     */
    public void setZeroChild(BddNode zeroChild) {
        this.zeroChild = zeroChild;
    }

    /**
     * @return  The one child of the node.
     */
    public BddNode getOneChild() {
        return oneChild;
    }

    /**
     * @param oneChild  The one child of the node.
     */
    public void setOneChild(BddNode oneChild) {
        this.oneChild = oneChild;
    }

    /**
     * @return  The decision variable of the node.
     */
    public Variable getVariable() {
        return variable;
    }

    /**
     * @param newVar  The decision variable of the node.
     */
    public void setVariable(Variable newVar) {
        variable = newVar;
    }

    public boolean isEquivalent(BddNode other) {
        return this.variable == other.variable && this.oneChild == other.oneChild && this.zeroChild == other.zeroChild;
    }
}
