package ex5.main;

/**
 * Informative exception: illegal number of arguments (print 2).
 */
public class InvalidArgumentsException extends Exception {
    private static final long serialVersionUID = 1L;
    /**
     * Constructor.
     * @param message the exception message
     */
    public InvalidArgumentsException(String message) { super(message); }
}
