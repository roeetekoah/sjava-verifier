package ex5.model;

/**
 * Represents one variable in some scope.
 * Global vs Local is determined by which Scope holds it.
 */
public final class VariableSymbol {
    private final String name;
    private final SType type;
    private final boolean isFinal;
    private boolean initialized;

    /**
     * Constructor for VariableSymbol.
     * @param name the variable name
     * @param type the variable type
     * @param isFinal whether the variable is final
     * @param initialized whether the variable is initialized
     */
    public VariableSymbol(String name, SType type, boolean isFinal, boolean initialized) {
        this.name = name;
        this.type = type;
        this.isFinal = isFinal;
        this.initialized = initialized;
    }

    /**
     * Returns the variable name.
     * @return the variable name
     */
    public String name() { return name; }

    /**
     * Returns the variable type.
     * @return the variable type
     */
    public SType type() { return type; }

    /**
     * Returns whether the variable is final.
     * @return true if the variable is final, false otherwise
     */
    public boolean isFinal() { return isFinal; }

    /**
     * Returns whether the variable is initialized.
     * @return true if the variable is initialized, false otherwise
     */
    public boolean isInitialized() { return initialized; }

    /**
     * Marks the variable as initialized.
     */
    public void markInitialized() { this.initialized = true; }

    /**
     * Overrides toString method to provide a string representation of the VariableSymbol.
     * @return string representation of the VariableSymbol.
     */
    @Override
    public String toString() {
        return "VariableSymbol{name='" + name + "', type=" + type +
                ", final=" + isFinal + ", initialized=" + initialized + "}";
    }
}
