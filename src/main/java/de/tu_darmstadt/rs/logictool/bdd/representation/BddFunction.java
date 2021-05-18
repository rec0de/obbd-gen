package de.tu_darmstadt.rs.logictool.bdd.representation;

import de.tu_darmstadt.rs.logictool.common.representation.BooleanFunction;
import de.tu_darmstadt.rs.logictool.common.representation.Variable;

/**
 * A boolean function represented by a BDD.
 */
public class BddFunction implements BooleanFunction {

    /**
     * The variables of the function in the same order as in the BDD.
     */
    private final Variable[] variables;

    /**
     * The BDD.
     */
    private final Bdd bdd;

    /**
     * The name.
     */
    private final String name;


    /**
     * Creates a new BDD function.
     *
     * @param bdd         The BDD.
     * @param variables  The variables of the function in the same order as in the BDD.
     * @param name        The name of the function.
     */
    public BddFunction(Bdd bdd, Variable[] variables, String name) {
        this.bdd = bdd;
        this.variables = variables;
        this.name = name;
    }


    @Override
    public boolean compute(boolean[] inputs) {
        BddNode currentNode = bdd.getRootNode();
        while (true) {
            // check for constant
            if (currentNode == bdd.getZeroNode())
                return false;
            else if (currentNode == bdd.getOneNode())
                return true;

            // get child corresponding to inputs
            if (inputs[currentNode.getVariable().getNumber()])
                currentNode = currentNode.getOneChild();
            else
                currentNode = currentNode.getZeroChild();
        }
    }

    @Override
    public Variable[] getVariables() {
        return variables;
    }

    @Override
    public String getName() {
        return name;
    }
}
