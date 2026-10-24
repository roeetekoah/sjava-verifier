package ex5.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The only supported s-Java types.
 */
public enum SType {
    /** Lexically classified integer type; numeric range is not checked. */
    INT("int"),

    /** Lexically classified decimal type; numeric range is not checked. */
    DOUBLE("double"),

    /** Boolean type: true or false. */
    BOOLEAN("boolean"),

    /** Single character type. */
    CHAR("char"),

    /** String literal type. */
    STRING("String");

    private final String stringForm;

    SType(String stringForm) {
        this.stringForm = stringForm;
    }

    /**
     * Returns the keyword representing this type.
     * @return the keyword representing this type
     */
    public String keyword() {
        return stringForm;
    }

    /**
     * Returns the SType represented by the given keyword.
     * @param keyword the keyword representing the type
     * @return the SType represented by the given keyword
     */
    public static SType fromKeyword(String keyword) {
        for (SType t : values()) {
            if (t.stringForm.equals(keyword)) {
                return t;
            }
        }

        return null;
    }


    /**
     *Returns a regex alternation of all supported s-Java type keywords.
     * @return the regex (?:int|double|boolean|char|String)
     */
    public static String regexAlternation() {
        return Arrays.stream(values())
                .map(SType::keyword)
                .collect(Collectors.joining("|", "(?:", ")"));
    }

}
