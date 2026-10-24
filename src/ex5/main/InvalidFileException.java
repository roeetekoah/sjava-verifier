package ex5.main;

/**
 * Informative exception: wrong file format / invalid file name (print 2).
 */
public class InvalidFileException extends Exception {
    private static final long serialVersionUID = 1L;
    /**
     * Constructor.
     * @param message the exception message
     */
    public InvalidFileException(String message) { super(message); }
}
