package ex5.model;

/**
 * Represents a method parameter - name, type, and whether it is final.
 */
public record ParamSymbol(String name, SType type, boolean isFinal) {}
