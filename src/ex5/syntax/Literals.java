package ex5.syntax;

import java.util.regex.Pattern;

/**
 * This class contains regex patterns for different types of literals.
 */
public class Literals {
    /**
     * Regex pattern for integer literals.
     */
    public static final Pattern INT_LITERAL =
            Pattern.compile("^" + LexicalPatterns.INT_CORE_RE + "$");
    /**
     * Regex pattern for double literals.
     */
    public static final Pattern DOUBLE_LITERAL =
            Pattern.compile("^" + LexicalPatterns.DOUBLE_CORE_RE + "$");
    /**
     * Regex pattern for boolean literals.
     */
    public static final Pattern BOOLEAN_LITERAL =
            Pattern.compile("^" + LexicalPatterns.BOOLEAN_CORE_RE + "$");
    /**
     * Regex pattern for character literals.
     */
    public static final Pattern CHAR_LITERAL =
            Pattern.compile("^" + LexicalPatterns.CHAR_CORE_RE + "$");
    /**
     * Regex pattern for string literals.
     */
    public static final Pattern STRING_LITERAL =
            Pattern.compile("^" + LexicalPatterns.STRING_CORE_RE + "$");
    /**
     * Regex pattern for value tokens.
     */
    public static final Pattern VALUE_TOKEN =
            Pattern.compile("^" + LexicalPatterns.VALUE_TOKEN + "$");
}
