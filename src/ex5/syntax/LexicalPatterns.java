package ex5.syntax;

import ex5.model.SType;

/**
 * A collection of regex patterns for lexical elements of SJava.
 * Covers variable names, method names, literals, types, and some common tokens.
 */
public final class LexicalPatterns {
    private LexicalPatterns() {}

    /**
     * regex for optional whitespaces
     */
    public static final String WS = "\\s*";

    // Variable name rules (spec):
    // - letters/digits/_ ; not start with digit
    // - "_" alone is illegal
    // - cannot start with "__" or more underscores
    /**
     * regex for variable names
     */
    public static final String VAR_NAME_CORE_RE =
            "(?!__)(?:_[A-Za-z0-9][A-Za-z0-9_]*|[A-Za-z][A-Za-z0-9_]*)";

    // Method names: like variable names but must start with a letter (no '_' start)
    /**
     * regex for method names
     */
    public static final String METHOD_NAME_CORE_RE =
            "[A-Za-z][A-Za-z0-9_]*";

    // ----- Literals -----
    /**
     * regex for integer literals
     */
    public static final String INT_CORE_RE =
            "[+-]?\\d+";
    /**
     * regex for double literals
     */
    // Double may be integer OR floating-point, but "." alone is illegal.
    // This covers: 5, 5.2, 2., .1, -.3
    public static final String DOUBLE_CORE_RE =
            "[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)";
    /**
     * regex for number literals (int or double)
     */
    // Useful helper: "number" as allowed in boolean conditions and boolean assignments
    public static final String NUMBER_CORE_RE = DOUBLE_CORE_RE;
    /**
     * regex for boolean literals (true, false, or number)
     */
    public static final String BOOLEAN_CORE_RE =
            "(?:\\b(?:true|false)\\b|" + NUMBER_CORE_RE + ")";
    /**
     * regex for char literals
     */
    // Per spec assumptions: no backslash / quote issues inside the literal values
    public static final String CHAR_CORE_RE =
            "'[^'\\\\]'";
    /**
     * regex for string literals
     */
    public static final String STRING_CORE_RE =
            "\"[^\"\\\\]*\"";
    /**
     * regex for the possible SJava variable types
     */
    // The set of type keywords (from your enum): (?:int|double|boolean|char|String)
    public static final String TYPE =
            SType.regexAlternation();
    /**
     * regex for value tokens (identifier or literal)
     */
    // ----- Value token = identifier OR literal -----
    // Order doesn't matter for matching; for typing, your resolver should check INT before DOUBLE.
    public static final String VALUE_TOKEN =
            "(?:" +
                    VAR_NAME_CORE_RE + "|" +
                    STRING_CORE_RE + "|" +
                    CHAR_CORE_RE + "|" +
                    "true|false|" +
                    NUMBER_CORE_RE +
                    ")";
    /**
     * regex for an opening bracket "("
     */
    public static final String OPEN_BRACKET = "\\(";
    /**
     * regex for a closing bracket ")"
     */
    public static final String CLOSE_BRACKET = "\\)";
    /**
     * regex for logic operators (&&, ||)
     */
    public static final String LOGIC_OPERATOR = "(?:\\|\\||&&)";
}
