package ex5.syntax;

/**
 * Thrown when the s-Java syntax is illegal.
 * Add line number + context for informative messages.
 */
public class SjavaSyntaxException extends Exception {
    private static final long serialVersionUID = 1L;
    private final int lineNumber;
    /**
     * Constructor.
     * @param message the exception message
     */
    public SjavaSyntaxException(String message) {
        super(message);
        this.lineNumber = 0;
    }

    private SjavaSyntaxException(int lineNumber, String message) {
        super("Verification error at line " + lineNumber + ": " + message);
        this.lineNumber = lineNumber;
    }

    /** Adds source context to errors from token resolution without nesting prefixes. */
    public SjavaSyntaxException withLineIfAbsent(int lineNumber) {
        return this.lineNumber > 0 ? this : atLine(lineNumber, getMessage());
    }

    /**
     * Creates a SjavaSyntaxException at a specific line number with a message.
     * @param lineNumber the line number where the error occurred
     * @param msg the error message
     * @return a SjavaSyntaxException with the formatted message
     */
    public static SjavaSyntaxException atLine(int lineNumber, String msg) {
        return new SjavaSyntaxException(lineNumber, msg);
    }
}
