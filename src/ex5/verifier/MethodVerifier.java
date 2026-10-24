package ex5.verifier;

import ex5.syntax.SjavaSyntaxException;
import ex5.model.*;
import ex5.syntax.LexicalPatterns;
import ex5.syntax.LineClassifier;
import ex5.syntax.LineKind;
import ex5.syntax.Patterns;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies method bodies line by line, maintaining a scope stack.
 */
public final class MethodVerifier {

    // Hold current method context
    private Ctx ctx;

    // ---------------- Extractors (Option A: separate from validator regex) ----------------
    private static final String WS = LexicalPatterns.WS;
    private static final String WS_P = "\\s+";
    private static final String TYPE = LexicalPatterns.TYPE;                // alternation (?:int|double|...)
    private static final String VAR_NAME = LexicalPatterns.VAR_NAME_CORE_RE; // no ^$
    private static final String METHOD_NAME = LexicalPatterns.METHOD_NAME_CORE_RE;
    private static final String VALUE_TOKEN = LexicalPatterns.VALUE_TOKEN;   // identifier OR literal (no ^$)

    // var decl: [final] <type> <list> ;
    // groups: 1=final?  2=typeKeyword  3=list
    private static final Pattern VAR_DECL_EXTRACT = Pattern.compile(
            "^" + WS + "(?:(final)" + WS_P + ")?(" + TYPE + ")" + WS_P + "(.+)" + WS + ";" + WS + "$"
    );

    // one item in list: <name> [= <valueToken>] (followed by comma or end)
    // groups: 1=name  2=valueToken?
    private static final Pattern VAR_ITEM_FIND = Pattern.compile(
            WS + "(" + VAR_NAME + ")" + WS + "(?:=" + WS + "(" + VALUE_TOKEN + "))?" + WS + "(?=,|$)"
    );

    // method call: <name>(<args>);
    // groups: 1=methodName  2=argsRaw (might be empty)
    private static final Pattern METHOD_CALL_EXTRACT = Pattern.compile(
            "^" + WS + "(" + METHOD_NAME + ")" + WS + "\\(" + WS + "(.*?)" + WS + "\\)" + WS + ";" + WS + "$"
    );

    // if/while: (if|while)(<cond>) {
    // group: 1=condRaw
    private static final Pattern IF_WHILE_EXTRACT = Pattern.compile(
            "^" + WS + "(?:if|while)" + WS + "\\(" + WS + "(.*?)" + WS + "\\)" + WS + "\\{" + WS + "$"
    );

    // ---------------- Context ----------------
    private static final class Ctx {
        final List<String> lines;
        final MethodSymbol method;
        final GlobalSymbolTable globals;
        final MethodTable methods;

        int i;                 // current line index
        int openBlocks = 1;    // method header already opened '{'
        boolean sawReturn = false;
        LineKind lastExecutable = null; // track last non-brace executable statement

        final Deque<Scope> scopes = new ArrayDeque<>();

        final Set<String> globalsInitializedHere = new HashSet<>();

        Ctx(List<String> lines, int startIndex, MethodSymbol method,
            GlobalSymbolTable globals, MethodTable methods) {
            this.lines = lines;
            this.i = startIndex;
            this.method = method;
            this.globals = globals;
            this.methods = methods;
        }

        Scope scope() { return scopes.peek(); }

        void pushBlockScope() {
            scopes.push(new Scope(scope()));
            openBlocks++;
        }

        void popCloseBraceOrFail() throws SjavaSyntaxException {
            if (scopes.isEmpty()) throw SjavaSyntaxException.atLine(i + 1, "Unmatched '}'");
            scopes.pop();
            openBlocks--;
        }

        boolean methodEnded() { return openBlocks == 0; }
    }

    /**
     * Verifies one method body.
     * @param lines                 all source lines
     * @param methodHeaderLineIndex index of the method header line
     * @param method                the method symbol
     * @param globals               global symbol table (for resolving globals)
     * @param methods               method table (for resolving method calls)
     * @throws SjavaSyntaxException on syntax error
     */
    public void verifyMethod(List<String> lines,
                             int methodHeaderLineIndex,
                             MethodSymbol method,
                             GlobalSymbolTable globals,
                             MethodTable methods) throws SjavaSyntaxException {

        if (lines == null) throw new SjavaSyntaxException("Null lines");
        if (method == null) throw new SjavaSyntaxException("Null method");
        if (methods == null) throw new SjavaSyntaxException("Null method table");
        if (methodHeaderLineIndex < 0 || methodHeaderLineIndex >= lines.size()) {
            throw new SjavaSyntaxException("Bad method header index: " + methodHeaderLineIndex);
        }

        this.ctx = new Ctx(lines, methodHeaderLineIndex + 1, method, globals, methods);

        try {
            initMethodScopeWithParams();

            for (; ctx.i < ctx.lines.size(); ctx.i++) {
                String line = ctx.lines.get(ctx.i);

                LineKind kind = LineClassifier.classify(line);

                // Ignore empty/comment always.
                if (kind == LineKind.EMPTY || kind == LineKind.COMMENT) continue;

                switch (kind) {
                    case CLOSE_BRACE -> {
                        ctx.popCloseBraceOrFail();
                        if (ctx.methodEnded()) {
                            // method ends only after we've seen return;
                            if (!ctx.sawReturn || ctx.lastExecutable != LineKind.RETURN) {
                                throw SjavaSyntaxException.atLine(ctx.i + 1,
                                        "Method " + ctx.method.name() + " must end with return;");
                            }
                            return;
                        }
                        // A nested closing brace cannot replace the method-level final return.
                        ctx.lastExecutable = LineKind.CLOSE_BRACE;
                        continue;
                    }

                    case RETURN -> {
                        ctx.sawReturn = true;
                        ctx.lastExecutable = LineKind.RETURN;
                        continue;
                    }

                    case VAR_DECL -> {
                        handleVarDecl(line);
                        ctx.lastExecutable = LineKind.VAR_DECL;

                        continue;
                    }

                    case ASSIGNMENT -> {
                        handleAssignment(line);
                        ctx.lastExecutable = LineKind.ASSIGNMENT;
                        continue;
                    }

                    case METHOD_CALL -> {
                        handleMethodCall(line);
                        ctx.lastExecutable = LineKind.METHOD_CALL;
                        continue;
                    }

                    case IF_WHILE_OPEN -> {
                        handleIfWhile(line);
                        ctx.lastExecutable = LineKind.IF_WHILE_OPEN;
                        ctx.pushBlockScope(); // block scope opened by '{' at end of line
                        continue;
                    }

                    default -> throw SjavaSyntaxException.atLine(ctx.i + 1, "Illegal statement in method");
                }
            }

            throw SjavaSyntaxException.atLine(ctx.lines.size(),
                    "Unclosed method block for " + ctx.method.name());

        } catch (SjavaSyntaxException e) {
            throw e.withLineIfAbsent(ctx.i + 1);
        } finally {
            this.ctx = null; // prevent accidental reuse
        }
    }

    private void initMethodScopeWithParams() throws SjavaSyntaxException {
        Scope methodScope = new Scope(null);
        ctx.scopes.push(methodScope);

        for (ParamSymbol p : ctx.method.params()) {
            try {
                methodScope.declare(new VariableSymbol(p.name(), p.type(), p.isFinal(), true));
            } catch (DuplicateVarException e) {
                throw SjavaSyntaxException.atLine(ctx.i, "Duplicate parameter: " + p.name());
            }
        }
    }

    // ---------------- Handlers ----------------
    private void handleVarDecl(String line) throws SjavaSyntaxException {
        // Validator already matched Patterns.VAR_DECL, but we still extract safely.
        Matcher m = VAR_DECL_EXTRACT.matcher(line);
        if (!m.matches()) {
            throw SjavaSyntaxException.atLine(ctx.i + 1, "Bad variable declaration");
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

            // reject junk between items (besides commas/whitespace)
            String gap = list.substring(last, it.start()).trim();
            if (!gap.isEmpty() && !gap.equals(",")) {
                throw SjavaSyntaxException.atLine(ctx.i + 1, "Bad declaration list syntax");
            }
            last = it.end();

            String name = it.group(1);
            String rhs = it.group(2); // optional

            boolean initialized = false;
            if (rhs != null) {
                SType rhsType = ValueResolver.resolveType(rhs, ctx.scope(),
                        ctx.globals, ctx.globalsInitializedHere);
                if (!TypeSystem.canAssign(type, rhsType)) {
                    throw SjavaSyntaxException.atLine(ctx.i + 1,
                            "Type mismatch: cannot assign " + rhsType + " to " + type);
                }
                initialized = true;
            } else if (isFinal) {
                throw SjavaSyntaxException.atLine(ctx.i + 1, "Final variable must be initialized: " + name);
            }

            try {
                ctx.scope().declare(new VariableSymbol(name, type, isFinal, initialized));
            } catch (DuplicateVarException e) {
                throw SjavaSyntaxException.atLine(ctx.i + 1, e.getMessage());
            }
        }

        String tail = list.substring(last).trim();
        if (!tail.isEmpty()) {
            throw SjavaSyntaxException.atLine(ctx.i + 1, "Bad declaration list syntax");
        }
        if (!any) {
            throw SjavaSyntaxException.atLine(ctx.i + 1, "Empty variable declaration");
        }
    }

    private void handleAssignment(String line) throws SjavaSyntaxException {
        // Validator already matched Patterns.ASSIGNMENT; now iterate pairs.
        Matcher pair = Patterns.ASSIGN_PAIR_FIND.matcher(line);
        boolean any = false;

        while (pair.find()) {
            any = true;
            String lhs = pair.group(1).trim();
            String rhs = pair.group(2).trim();

            VariableSymbol target = ctx.scope().resolve(lhs);
            if (target == null && ctx.globals != null) target = ctx.globals.scope().resolve(lhs);
            if (target == null) throw SjavaSyntaxException.atLine(ctx.i + 1, "Unknown LHS: " + lhs);

            boolean alreadyInit =
                    target.isInitialized()              // initialized in global scope (only!)
                            || ctx.globalsInitializedHere.contains(lhs); // initialized earlier in THIS method

            if (target.isFinal() && alreadyInit) {
                throw SjavaSyntaxException.atLine(ctx.i + 1, "Reassignment to final: " + lhs);
            }

            SType rhsType = ValueResolver.resolveType(rhs, ctx.scope(), ctx.globals,
                                                                 ctx.globalsInitializedHere);
            if (!TypeSystem.canAssign(target.type(), rhsType)) {
                throw SjavaSyntaxException.atLine(ctx.i + 1,
                        "Type mismatch: cannot assign " + rhsType + " to " + target.type());
            }

            boolean isLocal = (ctx.scope().resolve(lhs) != null);
            if (isLocal) {
                target.markInitialized();
            } else {
                // target came from globals
                ctx.globalsInitializedHere.add(lhs);
            }
        }

        if (!any) throw SjavaSyntaxException.atLine(ctx.i + 1, "Empty assignment");
    }

    private void handleMethodCall(String line) throws SjavaSyntaxException {
        Matcher m = METHOD_CALL_EXTRACT.matcher(line);
        if (!m.matches()) throw SjavaSyntaxException.atLine(ctx.i + 1, "Bad method call");

        // someMethod(a, b, c, d);
        String name = m.group(1);
        String argsRaw = m.group(2).trim();

        MethodSymbol callee = ctx.methods.get(name);
        if (callee == null) throw SjavaSyntaxException.atLine(ctx.i + 1, "Unknown method: " + name);

        String[] args = argsRaw.isEmpty() ? new String[0] : argsRaw.split("\\s*,\\s*");
        if (args.length != callee.params().size()) {
            throw SjavaSyntaxException.atLine(ctx.i + 1, "Wrong number of arguments for " + name);
        }

        for (int k = 0; k < args.length; k++) {
            SType argType = ValueResolver.resolveType(args[k], ctx.scope(),
                                                ctx.globals, ctx.globalsInitializedHere);
            SType paramType = callee.params().get(k).type();
            if (!TypeSystem.canAssign(paramType, argType)) {
                throw SjavaSyntaxException.atLine(ctx.i + 1,
                        "Arg #" + (k + 1) + " type mismatch for " + name);
            }
        }
    }

    private void handleIfWhile(String line) throws SjavaSyntaxException {
        Matcher m = IF_WHILE_EXTRACT.matcher(line);
        if (!m.matches()) throw SjavaSyntaxException.atLine(ctx.i + 1, "Bad if/while");

        String cond = m.group(1).trim();
        if (cond.isEmpty()) throw SjavaSyntaxException.atLine(ctx.i + 1, "Empty condition");

        // regex already validated operator placement; validate atom semantics.
        String[] atoms = cond.split("\\s*(?:\\|\\||&&)\\s*");
        for (String a : atoms) {
            String atom = a.trim();
            if (atom.isEmpty()) throw SjavaSyntaxException.atLine(ctx.i + 1, "Bad condition");

            SType t = ValueResolver.resolveType(atom, ctx.scope(), ctx.globals, ctx.globalsInitializedHere);
            if (!(t == SType.BOOLEAN || t == SType.INT || t == SType.DOUBLE)) {
                throw SjavaSyntaxException.atLine(ctx.i + 1, "Illegal condition atom type: " + t);
            }
        }
    }
}
