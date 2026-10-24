package ex5.model;

/**
 * A wrapper around the global Scope.
 * Handles global variable declarations and lookups.
 */
public final class GlobalSymbolTable {
    private final Scope globalScope = new Scope(null);

    /**
     * Returns the global scope.
     * @return the global scope.
     */
    public Scope scope() { return globalScope; }

    /**
     * Adds a global variable to the global scope.
     * @param v the variable to add.
     * @throws DuplicateVarException if a variable with the same name already exists.
     */
    public void declareGlobal(VariableSymbol v) throws DuplicateVarException {
        globalScope.declare(v);
    }

    /**
     * Finds a global variable by name.
     * @param name the name of the variable.
     * @return the variable symbol, or null if not found.
     */
    public VariableSymbol resolveGlobal(String name) {
        // Only globals: no parent chain anyway, but keep explicit.
        return globalScope.resolve(name);
    }
}
