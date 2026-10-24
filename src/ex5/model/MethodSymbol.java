package ex5.model;

import java.util.List;

/**
 * Represents a method signature (void methods only).
 */
public record MethodSymbol(String name, List<ParamSymbol> params) {}
