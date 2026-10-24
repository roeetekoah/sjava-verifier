package ex5.model;

/**
 * Thrown when declaring a variable with an existing name in the same Scope.
 */
public class DuplicateVarException extends Exception {
    private static final long serialVersionUID = 1L;
    /**
     * Constructor.
     * @param message the exception message
     */
    public DuplicateVarException(String message) { super(message); }
}
