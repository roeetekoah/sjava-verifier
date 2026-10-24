package ex5.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles method signatures.
 */
public final class MethodTable {
    private final Map<String, MethodSymbol> methods = new HashMap<>();

    /**
     * Adds a method to the table.
     * @param m the method signature to add.
     * @throws DuplicateMethodException if a method with the same name already exists.
     */
    public void add(MethodSymbol m) throws DuplicateMethodException {
        if (methods.containsKey(m.name())) {
            throw new DuplicateMethodException("Duplicate method name: " + m.name());
        }

        methods.put(m.name(), m);
    }

    /**
     * Retrieves a method by name.
     * @param name the name of the method to retrieve.
     * @return the method signature, or null if not found.
     */
    public MethodSymbol get(String name) {
        return methods.get(name);
    }

    /**
     * Checks if a method exists in the table.
     * @param name the name of the method to check.
     * @return true if the method exists, false otherwise.
     */
    public boolean contains(String name) { return methods.containsKey(name); }
}
