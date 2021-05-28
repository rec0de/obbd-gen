package de.tu_darmstadt.rs.logictool.common.representation;

// Test

/**
 * A variable/input of a boolean function.
 */
public class Variable {

    /**
     * The name of the variable.
     */
    private final String name;

    /**
     * The unique number of the variable.
     */
    private int number;

    /**
     * Creates a new variable.
     *
     * @param name    The name of the variable.
     * @param number  The unique number of the variable.
     */
    public Variable(String name, int number) {
        this.name = name;
        this.number = number;
    }


    /**
     * @return  The unique number of the variable.
     */
    public int getNumber() {
        return number;
    }

    /**
     * @param number  The unique number of the variable.
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * @return  The name of the variable.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + '(' + number + ')';
    }
}
