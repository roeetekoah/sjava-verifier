package ex5.syntax;

/**
 * Syntactic classification of a single S-Java source line.
 * Produced by LineClassifier and consumed by semantic verifiers.
 */
public enum LineKind {

    /** Empty or whitespace-only line. */
    EMPTY,

    /** Comment line (//). */
    COMMENT,

    /** Closing brace '}'. */
    CLOSE_BRACE,

    /** Method declaration header: 'void name(...) {'. */
    METHOD_DECL,

    /** Opening of if/while block: 'if (...) {' or 'while (...) {'. */
    IF_WHILE_OPEN,

    /** Return statement: 'return;'. */
    RETURN,

    /** Variable declaration statement. */
    VAR_DECL,

    /** Assignment statement. */
    ASSIGNMENT,

    /** Standalone method call statement. */
    METHOD_CALL,

    /** Line does not match any legal S-Java syntax. */
    ILLEGAL
}
