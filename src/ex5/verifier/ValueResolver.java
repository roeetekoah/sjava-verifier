package ex5.verifier;

import ex5.syntax.SjavaSyntaxException;
import ex5.model.GlobalSymbolTable;
import ex5.model.SType;
import ex5.model.Scope;
import ex5.model.VariableSymbol;
import ex5.syntax.Literals;

import java.util.Set;

/**
 * Resolves a token (literal or variable name) to a type, while enforcing "initialized" constraints.
 */
public final class ValueResolver {

    private ValueResolver() {}


    /**
     * An overload to the GlobalVerifier:
     * Returns the token's type, resolving literals and variables in the current scope and global scope.
     * @param token the token to resolve
     * @param currentScope the current scope to look for variables
     * @param globals the global symbol table to look for global variables
     * @return the type of the token (literal type or variable type)
     */
    public static SType resolveType(String token, Scope currentScope, GlobalSymbolTable globals)
            throws SjavaSyntaxException {
        return resolveType(token, currentScope, globals, null);
    }

    /**
     * Returns the token's type, resolving literals and variables in the current scope and global scope.
     * @param token the token to resolve
     * @param currentScope the current scope to look for variables
     * @param globals the global symbol table to look for global variables
     * @param globalsInitializedHere per method - which globals are initialized
     * @return the type of the token (literal type or variable type)
     */
    public static SType resolveType(String token, Scope currentScope,
                                    GlobalSymbolTable globals, Set<String> globalsInitializedHere)
            throws SjavaSyntaxException {
        token = normalize(token);

        SType lit = literalTypeOrNull(token);
        if (lit != null) return lit;

        // 1) Try local first
        VariableSymbol v = currentScope.resolve(token);
        boolean isLocal = (v != null);

        // 2) If not local, try global
        if (!isLocal && globals != null) {
            v = globals.scope().resolve(token);
        }

        if (v == null) throw new SjavaSyntaxException("Unknown identifier: " + token);

        // 3) Initialization rule differs for locals vs globals
        if (isLocal) {
            if (!v.isInitialized()) {
                throw new SjavaSyntaxException("Variable used before initialization: " + token);
            }
        } else {
            boolean initNow =
                    v.isInitialized() || (globalsInitializedHere != null &&
                                                    globalsInitializedHere.contains(token));
            if (!initNow) {
                throw new SjavaSyntaxException("Variable used before initialization: " + token);
            }
        }

        return v.type();
    }


    private static String normalize(String token) throws SjavaSyntaxException {
        if (token == null) throw new SjavaSyntaxException("Null token");
        String t = token.trim();
        if (t.isEmpty()) throw new SjavaSyntaxException("Empty token");
        return t;
    }

    private static SType literalTypeOrNull(String token) {
        // INT must come before DOUBLE because DOUBLE accepts ints
        if (Literals.INT_LITERAL.matcher(token).matches()) return SType.INT;
        if (Literals.STRING_LITERAL.matcher(token).matches()) return SType.STRING;
        if (Literals.CHAR_LITERAL.matcher(token).matches()) return SType.CHAR;
        // boolean allows numbers too, but treat true/false as BOOLEAN and numeric-only as DOUBLE/INT above
        if (Literals.DOUBLE_LITERAL.matcher(token).matches()) return SType.DOUBLE;
        if (Literals.BOOLEAN_LITERAL.matcher(token).matches()) return SType.BOOLEAN;
        // NOTE: We do NOT return BOOLEAN for numeric tokens; boolean accepts numbers via TypeSystem rules.
        // If it's not a literal we recognize, return null.
        return null;
    }
}
