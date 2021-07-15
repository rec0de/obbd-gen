package de.tu_darmstadt.rs.logictool.bdd.representation;

import de.tu_darmstadt.rs.logictool.common.representation.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * A node of a BDD.
 */
public class BddNode {

    /**
     * The number which is unique in the BDD.
     */
    private int number;

    /**
     * The parents of the node.
     */
    private ArrayList<BddNode> parents = new ArrayList<>(3);

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
     * @return  The number which is unique in the BDD.
     */
    public int getNumber() {
        return number;
    }

    /**
     * @param number  The number which is unique in the BDD.
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * @return  The parents of the node.
     */
    public List<BddNode> getParents() {
        return parents;
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
        // remove link to parent
        if (this.zeroChild != null)
            this.zeroChild.parents.remove(this);

        this.zeroChild = zeroChild;

        // set new parent
        if (zeroChild != null)
            this.zeroChild.parents.add(this);

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
        // remove link to parent
        if (this.oneChild != null)
            this.oneChild.parents.remove(this);

        this.oneChild = oneChild;

        // set new parent
        if (oneChild != null)
            this.oneChild.parents.add(this);
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
