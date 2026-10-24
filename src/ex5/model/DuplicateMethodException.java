package ex5.model;

/**
 * Thrown when method overloading / duplicate method name is attempted.
 */
public class DuplicateMethodException extends Exception {
    private static final long serialVersionUID = 1L;
    /**
     * Constructor.
     * @param message the exception message
     */
    public DuplicateMethodException(String message) { super(message); }
}
