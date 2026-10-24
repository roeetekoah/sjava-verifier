package ex5.syntax;

/**
 * Classifies lines using Patterns.
 */
public final class LineClassifier {

    private LineClassifier() {}

    /**
     * Classifies a line (String) into a LineKind.
     * @param line the line to classify
     * @return the LineKind of the line
     */
    public static LineKind classify(String line) {
        if (Patterns.EMPTY_LINE.matcher(line).matches()) return LineKind.EMPTY;
        if (Patterns.COMMENT_LINE.matcher(line).matches()) return LineKind.COMMENT;
        if (Patterns.CLOSE_BRACE.matcher(line).matches()) return LineKind.CLOSE_BRACE;
        if (Patterns.RETURN.matcher(line).matches()) return LineKind.RETURN;

        if (Patterns.METHOD_DECL.matcher(line).matches()) return LineKind.METHOD_DECL;
        if (Patterns.IF_WHILE.matcher(line).matches()) return LineKind.IF_WHILE_OPEN;

        if (Patterns.VAR_DECL.matcher(line).matches()) return LineKind.VAR_DECL;
        if (Patterns.ASSIGNMENT.matcher(line).matches()) return LineKind.ASSIGNMENT;
        if (Patterns.METHOD_CALL.matcher(line).matches()) return LineKind.METHOD_CALL;

        return LineKind.ILLEGAL;
    }
}
