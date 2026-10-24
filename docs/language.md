# Language and semantic contract

This describes the curated verifier edition. It is an independently written account of the implemented coursework subset, not a redistribution of the assignment handout or a claim of full Java conformance. Examples under `corpus/` are executable witnesses; rule IDs below map to `corpus/cases.tsv`.

## File layout (`FILE`, `LAYOUT`)

A file contains any mixture of global declarations, global assignments, and void method definitions. Empty files and globals-only files are accepted. No entry point is required. Each statement/header occupies one physical line; its semicolon or opening brace is on that same line. Every closing brace occupies a separate line. Blank lines are ignored. Only comments beginning with `//` in column one are ignored; indented comments, trailing comments, and block comments are rejected.

Whitespace may surround tokens. It must separate `final` from the type, a type from a variable name, and `void` from a method name. Tokens cannot be split across whitespace or lines. Files are read as UTF-8; LF and CRLF are tested. A UTF-8 BOM is not removed and therefore makes the first non-comment statement illegal.

## Lexical forms (`NAME`, `LITERAL`)

Identifiers are case-sensitive ASCII letters, digits, and underscores. Variable names start with a letter, or one underscore followed by a letter/digit. A single `_`, a leading digit, and two leading underscores are illegal. Method names must start with a letter. The remaining characters may include underscores. Variable and method names use separate namespaces.

| Literal | Accepted form |
|---|---|
| Integer | Optional `+`/`-`, then one or more decimal digits; leading zeros allowed |
| Double | Integer form, or decimal form with at least one digit; `.5`, `2.`, `-.3` allowed |
| Boolean | `true`/`false`; numeric tokens also assign to boolean |
| Character | One character between single quotes; no backslash or single quote payload |
| String | Zero or more characters between double quotes; no backslash or double quote payload |

Numbers are classified lexically, not parsed into machine values. There is no integer range, floating-point overflow, or value evaluation check. Exponents, hexadecimal forms, suffixes, escape sequences, and multi-line literals are unsupported. A supplementary Unicode character represented by a surrogate pair is not a guaranteed single `char`.

**Literal boundary:** the original exercise assumes no commas, backslashes, or either quote character inside literal payloads. The supported portable subset retains that assumption. Commas in particular are outside the contract: call arguments are split on commas without quote awareness, even though declaration regexes may accept a comma inside a string. Do not infer general string-literal support from incidental acceptance. Quote characters opposite to the delimiter can also be accepted incidentally. These cases are not an extension of the language.

**Keyword boundary:** reserved words are not excluded by the identifier regexes. This was unspecified by the exercise. `true` and `false` are resolved as literals before identifier lookup, even if a variable with that spelling was declared. Keyword-shaped identifiers are therefore outside the portable naming contract; ordinary non-keyword names are recommended. No Java keyword validation is claimed.

## Declarations and assignments (`DECL`, `ASSIGN`, `FINAL`, `TYPE`)

```text
[final] type name [= value] [, name [= value]]* ;
name = value [, name = value]* ;
```

Here `value` is a single literal or variable name, never an expression or call. Declarations and assignment pairs are processed left to right. A later item can use an earlier initialized item on the same line. Mixed initialized/uninitialized declarations are supported. All variables on a `final` declaration line must be initialized and cannot subsequently be reassigned. Non-final variables may remain unused and uninitialized.

| Target type | Allowed source types |
|---|---|
| `int` | `int` |
| `double` | `int`, `double` |
| `boolean` | `int`, `double`, `boolean` |
| `char` | `char` |
| `String` | `String` |

This matrix applies to initializers, later assignments, and method arguments. There are no casts or implicit conversions beyond this table. `a = b = 1;`, tuple assignment, arithmetic, and string concatenation are illegal.

## Visibility and initialization (`SCOPE`, `INIT`, `GLOBAL`)

A name must be declared before local use. Duplicate names in the same scope are illegal regardless of type. Method parameters share the method's outermost local scope. Nested and sibling `if`/`while` blocks have their own scopes and may shadow outer locals or globals. A name resolves to the nearest declared local, then to the global table; an uninitialized local blocks a same-name initialized global. Inner declarations disappear at the closing brace.

An initializer is resolved before its new symbol is inserted. Thus `int x = x;` with no outer `x` is illegal; a new inner `x` may use an existing outer `x` as its initializer. This differs from Java.

At global level, declarations and initialization are available only after their source line/item. Inside methods, every global declaration is visible regardless of file position. Methods start with the global initialization state obtained after processing **all global statements** in source order.

Parameters begin initialized. Locals must be initialized before use as values, conditions, or arguments. Assignment changes the resolved symbol's initialization state; assigning an outer local inside a nested block remains visible after that block. Reachability is ignored: assignment in `if (false)` counts. There is no branch intersection, loop analysis, or constant folding.

Assignments to globals inside a method are recorded separately for that method. They count for later uses anywhere in the same method, including after leaving a nested block. They do not change the shared global table and do not initialize a global for other methods. Calling a method has no modeled initialization effect. This is static source-order checking, not execution simulation.

## Methods (`CALL`)

```text
void methodName ([final] type parameter [, [final] type parameter]*) {
    ...
    return;
}
methodName ([value [, value]*]);
```

Parameter and argument lists may be empty. Parameters are typed and have no initializer syntax. Parameter names must be unique. A final parameter cannot be assigned within its method; the corresponding caller argument need not be final.

Method names are unique; overloading is rejected even if parameters differ. Signatures are collected before bodies are checked, so forward calls and recursion are allowed. Argument count and positional types must match the signature under the conversion matrix. Calls are legal only inside methods/blocks. Nested method declarations and non-void methods are illegal.

## Control flow and return (`FLOW`, `RETURN`)

```text
if (atom [(&& | ||) atom]*) {
    ...
}
while (atom [(&& | ||) atom]*) {
    ...
}
```

Atoms are `true`, `false`, numeric literals, or initialized numeric/boolean variables. String and char variables cannot be conditions. Mixed `&&`/`||` chains are accepted but never evaluated; no runtime precedence or short-circuit behavior is modeled. Comparisons, negation, and extra parentheses are unsupported. Blocks may nest and must occur inside methods. Practical depth is limited by memory and regex/resource constraints; 300 nested scopes are tested, not unbounded nesting.

`return;` may appear anywhere in a method or its blocks, but the final meaningful line before the **method's own** closing brace must be a method-level `return;`. A return only inside a nested block is insufficient. Empty lines and column-one comments may follow it. Earlier returns do not exempt subsequent statements from verification, and unreachable code is not rejected merely for being unreachable. Returning a value is illegal.

## Errors and exclusions (`EXCLUDE`)

The CLI prints exactly one result line: `0` accepted, `1` language violation, `2` argument/path/read failure. It prints one error to stderr for `1`/`2`. Language errors include a one-based line; an unclosed block uses the final source line as the end-of-file location. Only the first error encountered by the verifier is reported. Because globals/structure are checked before method semantics, this need not be the earliest erroneous line in the file. Diagnostics are explanatory text, not stable machine-readable error codes.

There are no imports, packages, classes, interfaces, arrays, object construction, field access, exceptions, modifiers other than `final`, arithmetic, general expressions, `else`, `for`, `do`, `switch`, `break`, `continue`, or value-returning calls. Files do not interact. The verifier does not execute code, prove termination, enforce Java numeric ranges, or provide resource isolation for hostile inputs. Unexpected JVM/runtime failures are outside the `0/1/2` contract.
