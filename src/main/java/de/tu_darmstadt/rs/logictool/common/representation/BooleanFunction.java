package de.tu_darmstadt.rs.logictool.common.representation;

/**
 * Common interface for all boolean functions.
 */
public interface BooleanFunction {

    /**
     * Computes the value of the function for the given input configuration.
     *
     * @param inputs  The values of the input variables.
     * @return  The value of the function.
     */
    boolean compute(boolean[] inputs);

    /**
     * Gets the variables of the function.
     *
     * @return  The variables of the function.
     */
    Variable[] getVariables();

    /**
     * @return The name of the function.
     */
    String getName();
}
