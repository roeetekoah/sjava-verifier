package ex5.syntax;

import java.util.regex.Pattern;

import static ex5.syntax.LexicalPatterns.CLOSE_BRACKET;
import static ex5.syntax.LexicalPatterns.OPEN_BRACKET;

/**
 * A collection of compiled regex Patterns.
 */
public final class Patterns {

    private Patterns() { }

    // Identifiers
    private static final String FINAL = "final";

    //shortcuts to LexicalParts for readability
    private static final String WS = LexicalPatterns.WS;
    private static final String WS_P = "\\s+";
    private static final String TYPE = LexicalPatterns.TYPE;
    private static final String VALUE = LexicalPatterns.VALUE_TOKEN;
    private static final String VAR_NAME = LexicalPatterns.VAR_NAME_CORE_RE;
    private static final String METHOD_NAME = LexicalPatterns.METHOD_NAME_CORE_RE;

    // --- Basic line categories ---
    /**
     * Pattern matching an empty line (whitespace only).
     */
    public static final Pattern EMPTY_LINE = Pattern.compile("^" + WS + "$");
    /**
     * Pattern matching a comment line (// ...
     */
    public static final Pattern COMMENT_LINE = Pattern.compile("^//.*$");

    /**
     * Pattern matching a closing brace line (}).
     */
    public static final Pattern CLOSE_BRACE = Pattern.compile("^\\s*}\\s*$");

    // method declaration header: void name((final?) param, params) {
    private static final String PARAM_DECL = WS + "(?:final" + WS_P + ")?" + TYPE + WS_P + VAR_NAME + WS;
    private static final String PARAM_LIST =
            "(?:" + PARAM_DECL + "(?:" + WS + "," + WS + PARAM_DECL + ")*)?";
    /**
     * Pattern matching a method declaration line.
     */
    public static final Pattern METHOD_DECL = Pattern.compile(
            "^" + WS + "void" + WS_P + METHOD_NAME + WS +
                    "\\(" + WS +
                    PARAM_LIST +
                    WS + "\\)" +
                    WS + "\\{" + WS + "$");

    // var declaration
    private static final String VAR_ITEM = VAR_NAME + WS + "(?:=" + WS + VALUE + ")?";

    // Validate a whole assignment line: pair(,pair)* ;
    private static final String VAR_LIST = "(?:" + FINAL + WS_P + ")?" +
                TYPE + WS_P + VAR_ITEM + "(?:" + WS + "," + WS + VAR_ITEM + ")*";
    /**
     * Pattern matching a variable declaration line.
     */
    public static final Pattern VAR_DECL = Pattern.compile("^" + WS + VAR_LIST + WS + ";" +  WS + "$");

    // Pair core (capturing LHS name, RHS token).
    // Groups: 1 -> variable name, 2 -> value token (identifier or literal)
    private static final String ASSIGN_PAIR = "(" + VAR_NAME + ")" + WS + "=" + WS + "(" + VALUE + ")";

    // Validate a whole assignment line: pair(,pair)* ;
    /**
     * Pattern matching a variable assignment line.
     */
    public static final Pattern ASSIGNMENT = Pattern.compile(
            "^" + WS + ASSIGN_PAIR + WS + "(?:" + WS + "," + WS + ASSIGN_PAIR + ")*" + WS + ";" + WS + "$"
    );

    // Extract all pairs reliably using find(), with a boundary guard.
    // The lookahead enforces that after a pair we must have comma or semicolon (after optional WS).
    /**
     * Pattern for finding all assignment pairs in an assignment line.
     */
    public static final Pattern ASSIGN_PAIR_FIND = Pattern.compile(WS + ASSIGN_PAIR + WS + "(?=,|;)");

    // if/while header: if (cond) { / while (cond) {

    private static final String ATOMIC_COND = "(?:" + LexicalPatterns.BOOLEAN_CORE_RE + "|" + VAR_NAME + ")";
    private static final String CONDITION = ATOMIC_COND +
                        "(?:" + WS + LexicalPatterns.LOGIC_OPERATOR + WS + ATOMIC_COND + ")*";
    /**
     * Pattern matching an if or while statement line.
     */
    public static final Pattern IF_WHILE =
                Pattern.compile("^\\s*(?:if|while)\\s*\\(\\s*" + CONDITION + "\\s*\\)\\s*\\{\\s*$");

    // method call line: name(args);

    private static final String METHOD_CALL_PARAM_LIST =
            "(?:" + WS + VALUE + WS + "(?:" + "," + WS + VALUE + WS + ")*)?";
    /**
     * Pattern matching a method call line.
     */
    public static final Pattern METHOD_CALL = Pattern.compile(
            "^" + WS + METHOD_NAME
                    + WS + OPEN_BRACKET + WS + METHOD_CALL_PARAM_LIST + WS + CLOSE_BRACKET
                    + WS + ";" + WS + "$"
    );

    /**
     * Pattern matching a return statement line.
     */
    public static final Pattern RETURN = Pattern.compile("^\\s*return\\s*;\\s*$");
}
