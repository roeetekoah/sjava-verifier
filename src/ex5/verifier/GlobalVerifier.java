package ex5.verifier;

import ex5.syntax.SjavaSyntaxException;
import ex5.model.*;
import ex5.syntax.LexicalPatterns;
import ex5.syntax.LineClassifier;
import ex5.syntax.LineKind;
import ex5.syntax.Patterns;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies lines that are in the global scope:
 *  - variable declarations
 *  - assignments
 *  - forbids method calls and if/while blocks
 */
public final class GlobalVerifier {

    private Ctx ctx;

    // -------- Extractors (same approach as MethodVerifier) --------
    private static final String WS = LexicalPatterns.WS;
    private static final String WS_P = "\\s+";
    private static final String TYPE = LexicalPatterns.TYPE;
    private static final String VAR_NAME = LexicalPatterns.VAR_NAME_CORE_RE;
    private static final String VALUE_TOKEN = LexicalPatterns.VALUE_TOKEN;

    // groups: 1=final? 2=type 3=list
    private static final Pattern VAR_DECL_EXTRACT = Pattern.compile(
            "^" + WS + "(?:(final)" + WS_P + ")?(" + TYPE + ")" + WS_P + "(.+)" + WS + ";" + WS + "$"
    );

    // groups: 1=name 2=valueToken?
    private static final Pattern VAR_ITEM_FIND = Pattern.compile(
            WS + "(" + VAR_NAME + ")" + WS + "(?:=" + WS + "(" + VALUE_TOKEN + "))?" + WS + "(?=,|$)"
    );

    // -------- Context --------
    private static final class Ctx {
        final GlobalSymbolTable globals;
        int i;

        Ctx(GlobalSymbolTable globals) {
            this.globals = globals;
        }
    }

    /**
     * Verifies one line assuming we are in global context.
     * @param line      The line to verify
     * @param lineIndex The line index (0-based) in the source file
     * @param globals   The global symbol table
     * @throws SjavaSyntaxException on syntax error
     */
    public void verifyGlobalLine(String line, int lineIndex, GlobalSymbolTable globals)
            throws SjavaSyntaxException {

        if (globals == null) throw new SjavaSyntaxException("Null globals");
        if (this.ctx == null || this.ctx.globals != globals) {
            // Allow calling without prior verifyGlobalLines(), but keep line numbers correct.
            this.ctx = new Ctx(globals);
        }

        this.ctx.i = lineIndex;

        LineKind kind = LineClassifier.classify(line);

        try {
            switch (kind) {
                case EMPTY, COMMENT -> {
                    return;
                }

                case METHOD_DECL -> {
                    // Global verifier does nothing for method headers; Verifier will dispatch to MethodVerifier.
                    return;
                }

                case VAR_DECL -> {
                    handleVarDecl(line);
                    return;
                }

                case ASSIGNMENT -> {
                    handleAssignment(line);
                    return;
                }

                // Everything else is forbidden globally
                case METHOD_CALL, IF_WHILE_OPEN, RETURN, CLOSE_BRACE, ILLEGAL -> {
                    throw SjavaSyntaxException.atLine(lineIndex + 1, "Illegal statement in global scope");
                }
            }
        } catch (SjavaSyntaxException e) {
            throw e.withLineIfAbsent(lineIndex + 1);
        }
    }


    private void handleVarDecl(String line) throws SjavaSyntaxException {

        Matcher m = VAR_DECL_EXTRACT.matcher(line);
        if (!m.matches()) {
            throw SjavaSyntaxException.atLine(ctx.i + 1, "Bad global variable declaration");
        }

        boolean isFinal = m.group(1) != null;
        SType type = SType.fromKeyword(m.group(2));
        if(type == null) {
            throw SjavaSyntaxException.atLine(ctx.i + 1, "Bad variable declaration");
        }
        String list = m.group(3);

        Matcher it = VAR_ITEM_FIND.matcher(list);
        int last = 0;
        boolean any = false;

        while (it.find()) {

            any = true;

            String gap = list.substring(last, it.start()).trim();
            if (!gap.isEmpty() && !gap.equals(",")) {
                throw SjavaSyntaxException.atLine(ctx.i + 1, "Bad declaration list syntax");
            }
            last = it.end();

            String name = it.group(1);
            String rhs = it.group(2); // optional

            boolean initialized = false;
            if (rhs != null) {
                // In global scope, identifiers must refer to already-defined & initialized globals.
                SType rhsType = ValueResolver.resolveType(rhs, ctx.globals.scope(), ctx.globals);
                if (!TypeSystem.canAssign(type, rhsType)) {
                    throw SjavaSyntaxException.atLine(ctx.i + 1,
                            "Type mismatch: cannot assign " + rhsType + " to " + type);
                }
                initialized = true;
            } else if (isFinal) {
                throw SjavaSyntaxException.atLine(ctx.i + 1, "Final global must be initialized: " + name);
            }

            try {
                ctx.globals.declareGlobal(new VariableSymbol(name, type, isFinal, initialized));
            } catch (DuplicateVarException e) {
                throw SjavaSyntaxException.atLine(ctx.i + 1, e.getMessage());
            }
        }

        String tail = list.substring(last).trim();
        if (!tail.isEmpty()) {
            throw SjavaSyntaxException.atLine(ctx.i + 1, "Bad declaration list syntax");
        }

        if (!any) {
            throw SjavaSyntaxException.atLine(ctx.i + 1, "Empty global variable declaration");
        }
    }

    private void handleAssignment(String line) throws SjavaSyntaxException {
        Matcher pair = Patterns.ASSIGN_PAIR_FIND.matcher(line);
        boolean any = false;

        while (pair.find()) {
            any = true;
            String lhs = pair.group(1).trim();
            String rhs = pair.group(2).trim();

            VariableSymbol target = ctx.globals.scope().resolve(lhs);
            if (target == null) {
                throw SjavaSyntaxException.atLine(ctx.i + 1, "Assignment to unknown global: " + lhs);
            }
            if (target.isFinal() && target.isInitialized()) {
                throw SjavaSyntaxException.atLine(ctx.i + 1, "Reassignment to final global: " + lhs);
            }

            SType rhsType = ValueResolver.resolveType(rhs, ctx.globals.scope(), ctx.globals);
            if (!TypeSystem.canAssign(target.type(), rhsType)) {
                throw SjavaSyntaxException.atLine(ctx.i + 1,
                        "Type mismatch: cannot assign " + rhsType + " to " + target.type());
            }

            target.markInitialized();
        }

        if (!any) throw SjavaSyntaxException.atLine(ctx.i + 1, "Empty assignment");
    }
}
