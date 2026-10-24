package ex5.model;

/**
 * Handles type checking and conversions.
 */
public final class TypeSystem {

    private TypeSystem() {}

    /**
     * Checks whether a value of sourceType can be assigned into targetType.
     * Legal assignments are: equality, double <- int, boolean <- int/double.
     * @return true if assignment is legal
     */
    public static boolean canAssign(SType targetType, SType sourceType) {
        if (targetType == sourceType) return true;
        return switch (targetType) {
            case DOUBLE -> sourceType == SType.INT;
            case BOOLEAN -> sourceType == SType.INT || sourceType == SType.DOUBLE;
            default -> false;
        };
    }
}
