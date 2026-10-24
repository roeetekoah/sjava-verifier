package ex5.verifier;

import ex5.syntax.SjavaSyntaxException;
import ex5.model.*;
import ex5.syntax.LexicalPatterns;
import ex5.syntax.LineClassifier;
import ex5.syntax.LineKind;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies in two passes:
 *  Pass 1: collect method signatures + structural checks (Syntax)
 *  Pass 2: validate global scope + each method body (Semantics)
 */
public final class Verifier {

    // extractor for method header: void name(params) {
    // groups: 1=methodName, 2=paramsRaw
    private static final Pattern METHOD_DECL_EXTRACT = Pattern.compile(
            "^" + LexicalPatterns.WS +
                    "void\\s+(" + LexicalPatterns.METHOD_NAME_CORE_RE + ")" +
                    LexicalPatterns.WS + "\\(" + LexicalPatterns.WS +
                    "(.*?)" +
                    LexicalPatterns.WS + "\\)" +
                    LexicalPatterns.WS + "\\{" + LexicalPatterns.WS + "$"
    );

    // extractor for one parameter: [final] type name
    // groups: 1=final?, 2=type, 3=name
    private static final Pattern PARAM_EXTRACT = Pattern.compile(
            "^" + LexicalPatterns.WS +
                    "(?:(final)\\s+)?" +
                    "(" + LexicalPatterns.TYPE + ")\\s+" +
                    "(" + LexicalPatterns.VAR_NAME_CORE_RE + ")" +
                    LexicalPatterns.WS + "$"
    );

    /**
     * Verifies the given lines of Sjava code.
     * @param lines the lines of code to verify
     * @throws SjavaSyntaxException if a syntax or semantic error is found
     */
    public void verify(List<String> lines) throws SjavaSyntaxException {
        MethodTable methods = new MethodTable();
        GlobalSymbolTable globals = new GlobalSymbolTable();

        verifyGlobal(lines, globals, methods);                // first pass
        verifyLocal(lines, globals, methods);                 // second pass
    }

    private void verifyGlobal(List<String> lines, GlobalSymbolTable globals, MethodTable methods)
            throws SjavaSyntaxException {

        GlobalVerifier gv = new GlobalVerifier();
        int braceDepth = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            LineKind kind = LineClassifier.classify(line);

            switch (kind) {

                case EMPTY:
                case COMMENT:
                    break;

                case ILLEGAL:
                    throw SjavaSyntaxException.atLine(i + 1,
                            braceDepth == 0 ? "Illegal statement in global scope" : "Illegal statement in method");

                case METHOD_DECL:
                    if (braceDepth != 0) {
                        throw SjavaSyntaxException.atLine(i + 1,
                                "Method declared inside a block");
                    }

                    MethodSymbol ms = parseMethodSignature(line, i);
                    try {
                        methods.add(ms);
                    } catch (DuplicateMethodException e) {
                        throw SjavaSyntaxException.atLine(i + 1, e.getMessage());
                    }

                    braceDepth++; // method opens a block
                    break;

                case IF_WHILE_OPEN:
                    if (braceDepth == 0) {
                        throw SjavaSyntaxException.atLine(i + 1,
                                "if/while declared outside a method");
                    }

                    braceDepth++;
                    break;

                case CLOSE_BRACE:
                    braceDepth--;
                    if (braceDepth < 0) {
                        throw SjavaSyntaxException.atLine(i + 1,
                                "Unmatched '}'");
                    }

                    break;

                default:
                    // Any other statement
                    if (braceDepth != 0) {
                        break;
                    }

                    gv.verifyGlobalLine(line, i, globals);
            }
        }

        if (braceDepth != 0) {
            throw SjavaSyntaxException.atLine(Math.max(1, lines.size()), "Unclosed block(s) at end of file");
        }
    }

    private MethodSymbol parseMethodSignature(String headerLine, int lineIndex) throws SjavaSyntaxException {
        Matcher m = METHOD_DECL_EXTRACT.matcher(headerLine);


        if (!m.matches()) {
            // validator said it's METHOD_DECL, but extractor didn't match -> inconsistency
            throw SjavaSyntaxException.atLine(lineIndex + 1, "Bad method declaration header");
        }

        String methodName = m.group(1);
        String paramsRaw = m.group(2).trim();

        List<ParamSymbol> params = new ArrayList<>();
        if (!paramsRaw.isEmpty()) {
            // per spec, commas won't appear in string/char literals in general,
            // and params have no literals at all, so this split is safe.
            String[] parts = paramsRaw.split("\\s*,\\s*");
            for (String p : parts) {
                Matcher pm = PARAM_EXTRACT.matcher(p);
                if (!pm.matches()) {
                    throw SjavaSyntaxException.atLine(lineIndex + 1, "Illegal parameter: " + p);
                }
                boolean isFinal = pm.group(1) != null;
                SType type = SType.fromKeyword(pm.group(2));
                String name = pm.group(3);

                params.add(new ParamSymbol(name, type, isFinal));
            }
        }

        // duplicate parameter names are illegal (same block)
        for (int a = 0; a < params.size(); a++) {
            for (int b = a + 1; b < params.size(); b++) {
                if (params.get(a).name().equals(params.get(b).name())) {
                    throw SjavaSyntaxException.atLine(lineIndex + 1,
                            "Duplicate parameter name: " + params.get(a).name());
                }
            }
        }

        return new MethodSymbol(methodName, params);
    }

    private void verifyLocal(List<String> lines, GlobalSymbolTable globals, MethodTable methods)
            throws SjavaSyntaxException {
        MethodVerifier mv = new MethodVerifier();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            LineKind kind = LineClassifier.classify(line);

            if (kind == LineKind.EMPTY || kind == LineKind.COMMENT) continue;

            if (kind == LineKind.METHOD_DECL) {
                // get signature (already collected), then verify body and skip it
                MethodSymbol ms = extractMethodNameAndGet(methods, line, i);

                mv.verifyMethod(lines, i, ms, globals, methods);

                i = findMethodEnd(lines, i); // for-loop will i++ to next line
                continue;
            }
        }
    }

    private MethodSymbol extractMethodNameAndGet(MethodTable methods, String headerLine, int lineIndex)
            throws SjavaSyntaxException {
        Matcher m = METHOD_DECL_EXTRACT.matcher(headerLine);
        if (!m.matches()) {
            throw SjavaSyntaxException.atLine(lineIndex + 1, "Bad method declaration header");
        }

        String name = m.group(1);
        MethodSymbol ms = methods.get(name);

        if (ms == null) {
            throw SjavaSyntaxException.atLine(lineIndex + 1,
                    "Internal error: missing method signature for " + name);
        }

        return ms;
    }

    /**
     * Finds the index of the closing brace '}' that ends the method whose header is at headerIndex.
     * Uses brace depth tracking: METHOD_DECL and IF_WHILE_OPEN increment; CLOSE_BRACE decrements.
     */
    private int findMethodEnd(List<String> lines, int headerIndex) throws SjavaSyntaxException {
        int depth = 1; // method header opens one '{'
        for (int i = headerIndex + 1; i < lines.size(); i++) {
            LineKind kind = LineClassifier.classify(lines.get(i));
            if (kind == LineKind.IF_WHILE_OPEN) depth++;
            else if (kind == LineKind.CLOSE_BRACE) depth--;

            if (depth == 0) return i;
            if (depth < 0) throw SjavaSyntaxException.atLine(i + 1, "Unmatched '}' while scanning method");
        }
        throw SjavaSyntaxException.atLine(lines.size(), "Unclosed method block");
    }
}
