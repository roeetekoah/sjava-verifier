# Design walkthrough

The original joint implementation has four packages. It validates complete source lines with composed regular expressions and then interprets their captured tokens. There is no token stream or AST; the small line-oriented grammar makes that a reasonable boundary, while also explaining the literal/comma limitation.

| Component | Responsibility |
|---|---|
| `main.Sjavac`, `ArgumentParser` | Validate one path, read UTF-8 lines, map handled exceptions to stdout/stderr |
| `syntax.LexicalPatterns`, `Patterns`, `LineClassifier` | Compose lexical forms into full-line patterns and classify each line |
| `verifier.Verifier` | Coordinate two passes, collect signatures, check structure, dispatch method bodies |
| `verifier.GlobalVerifier` | Process global declarations/assignments in source order |
| `verifier.MethodVerifier` | Walk one body using a stack of lexical scopes and a per-method initialization set |
| `verifier.ValueResolver` | Classify literal types or resolve a declared, initialized variable |
| `model.Scope`, symbol tables and symbols | Store names, types, finalness, initialization, and parent relationships |
| `model.TypeSystem` | Centralize allowed source-to-target type conversions |

## Two passes

Pass one classifies lines, tracks brace depth, rejects malformed lines and invalid nesting, collects every method signature, and verifies global statements. It skips the semantics of method bodies. Consequently, global statements are checked in source order, while all signatures and the completed global state are available for body verification.

Pass two finds each method, gives it an independent local environment, verifies the body, and skips to its closing brace. A method's outer scope has no parent; global resolution is an explicit fallback. Each nested `if`/`while` pushes a `Scope` whose parent is the current local scope. `Scope.resolve` walks nearest-to-farthest; duplicate insertion only checks the current scope.

Local symbols hold a mutable initialization flag. Assignments to an outer local update that actual symbol even inside a child scope. Global symbols keep only the completed global-level initialization state. A separate `globalsInitializedHere` set records assignments seen in the current method. That set is discarded when the method ends, preventing verification order from leaking initialization across methods.

`ValueResolver` checks literals first, then local scopes, then globals. It refuses unresolved/uninitialized names and returns the source type. The handlers ask `TypeSystem` whether the target can accept that source. Signature records hold ordered parameter types; calls use the same conversion rule as assignments.

## Follow the example

In [`walkthrough.sjava`](../corpus/valid/walkthrough.sjava), pass one stores global `total` as uninitialized, plus `inspect(int)` and `consume(double)` signatures. Pass two gives `inspect` an initialized `seed` parameter. `total = seed;` resolves `seed` locally, checks `int <- int`, and records `total` in that method's initialized-global set.

`if (total && true)` checks two boolean-compatible atoms before opening a child scope. `double total = .5;` declares a new initialized local, shadowing the global. `consume(total);` resolves that local as double and checks the previously collected signature even though `consume` appears later. On `}`, the local disappears. The method-level `return;` satisfies the terminal return rule. `consume` is then checked with its own fresh local scope and initialization set.

## Deliberate tradeoffs

The simple model supports forward calls and lexical name resolution with few moving parts. It does not analyze execution paths or interprocedural effects. Regex validation and extraction are separate, so changes must keep those patterns consistent. The two passes can report a later structural/global error before an earlier method semantic error. Several lines are classified repeatedly; there is no performance claim or large-input benchmark.

The portfolio repairs add missing source context to resolver errors, reject malformed lines during the structural pass, require a method-level final return, and normalize invalid paths into the existing CLI error channel. They do not introduce an AST or new language features. See [changes](../CHANGES.md) and the [test guide](../tests/README.md).
