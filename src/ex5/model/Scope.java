package ex5.model;

import java.util.HashMap;
import java.util.Map;

/**
 * A lexical scope with a parent pointer.
 * Used for global scope, method scope, and nested if/while blocks.
 */
public final class Scope {
    private final Scope parent;
    private final Map<String, VariableSymbol> vars = new HashMap<>();

    /**
     * Constructor for scope with given parent.
     * @param parent the parent scope, or null for global scope.
     */
    public Scope(Scope parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent scope.
     * @return the parent scope, null if this is the global scope.
     */
    public Scope parent() { return parent; }

    /**
     * Adds variable to this scope.
     * @param v the variable to add.
     * @throws DuplicateVarException if variable with same name already exists in this scope.
     */
    public void declare(VariableSymbol v) throws DuplicateVarException {
        if (vars.containsKey(v.name())) {
            throw new DuplicateVarException("Duplicate variable in same scope: " + v.name());
        }

        vars.put(v.name(), v);
    }

    /**
     * Returns the variable with the given name, searching this scope and parent scopes.
     * @param name the variable name.
     * @return the VariableSymbol with the given name, null if not found.
     */
    public VariableSymbol resolve(String name) {
        for (Scope s = this; s != null; s = s.parent) {
            VariableSymbol v = s.vars.get(name);
            if (v != null) return v;
        }

        return null;
    }

    /**
     * Returns an unmodifiable view of the local variables in this scope.
     * @return map of local variable names to VariableSymbol objects.
     */
    public Map<String, VariableSymbol> localsView() {
        return Map.copyOf(vars);
    }
}
