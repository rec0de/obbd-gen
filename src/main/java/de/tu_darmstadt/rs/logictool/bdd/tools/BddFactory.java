package de.tu_darmstadt.rs.logictool.bdd.tools;

import de.tu_darmstadt.rs.logictool.bdd.representation.Bdd;
import de.tu_darmstadt.rs.logictool.bdd.representation.BddNode;
import de.tu_darmstadt.rs.logictool.common.representation.BooleanFunction;
import de.tu_darmstadt.rs.logictool.common.representation.Variable;

/**
 * Factory for a BDD. The resulting BDD is not reduced.
 * This class is not thread safe.
 */
public class BddFactory {

    /**
     * The BDD to be created.
     */
    private Bdd bdd;

    /**
     * Current combination of input values.
     */
    private boolean[] inputs;

    /**
     * The boolean function to create the BDD for.
     */
    BooleanFunction function;


    /**
     * Create a new BDD for the given function using the default variable order of the function.
     *
     * @param function  The boolean function to create the BDD for.
     * @return           The newly created BDD.
     */
    public Bdd create(BooleanFunction function) {
        return createInternal(function, function.getVariables());
    }

    /**
     * Create a new BDD for the given function using the given order of variables.
     *
     * @param function  The boolean function to create the BDD for.
     * @param order     The order of variables.
     * @return           The newly created BDD.
     */
    public Bdd create(BooleanFunction function, Variable[] order) {
        // check if each variable from the function is contained in the order
        outerLoop: for (Variable var : function.getVariables()) {
            for (Variable otherVar : order) {
                if (var.getName().equals(otherVar.getName()))
                    continue outerLoop;
            }
            throw new IllegalArgumentException("The order does not contain the variable " + var.getName() + ".");
        }

        return createInternal(function, order);
    }

    /**
     * Create a new BDD for the given function using the given order of variables.
     * Without sanity check for parameters.
     *
     * @param function  The boolean function to create the BDD for.
     * @param order     The order of variables.
     * @return           The newly created BDD.
     */
    private Bdd createInternal(BooleanFunction function, Variable[] order) {
        // create BDD
        bdd = new Bdd(order);

        // set variable numbers
        int number = 0;
        for (Variable var : order) {
            var.setNumber(number);
            number++;
        }

        if (bdd.getVariables().length == 0) {
            // special case: constant functions
            if (function.compute(new boolean[0]))
                bdd.setRootNode(bdd.getOneNode());
            else
                bdd.setRootNode(bdd.getZeroNode());
        } else {
            // create root node
            this.inputs = new boolean[bdd.getVariables().length];
            this.function = function;
            BddNode node = new BddNode(bdd.getVariables()[0]);
            bdd.getNodes().add(node);
            bdd.setRootNode(node);
            createChildren(node);
        }

        return bdd;
    }

    /**
     * Creates the children for the given node.
     *
     * @param node  The node to create the children for.
     */
    private void createChildren(BddNode node) {
        int varNr = node.getVariable().getNumber();

        // zero child
        inputs[varNr] = false;
        if (varNr == inputs.length - 1) {
            // last variable -> compute constant
            if (function.compute(inputs))
                node.setZeroChild(bdd.getOneNode());
            else
                node.setZeroChild(bdd.getZeroNode());
        } else {
            // create new node
            BddNode child = new BddNode(bdd.getVariables()[varNr+1]);
            bdd.getNodes().add(child);
            node.setZeroChild(child);
            createChildren(child);
        }

        // one child
        inputs[varNr] = true;
        if (varNr == inputs.length - 1) {
            // last variable -> compute constant
            if (function.compute(inputs))
                node.setOneChild(bdd.getOneNode());
            else
                node.setOneChild(bdd.getZeroNode());
        } else {
            // create new node
            BddNode child = new BddNode(bdd.getVariables()[varNr+1]);
            bdd.getNodes().add(child);
            node.setOneChild(child);
            createChildren(child);
        }
    }
}
