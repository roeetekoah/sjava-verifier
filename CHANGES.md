# Portfolio edition changes

Compared with the completed January 2026 submission:

- Extracted the 24 implementation classes into `src/ex5/`, preserving package names and joint authorship. Omitted course materials, private submission metadata, IDE files, generated classes, archives, and old test utilities.
- Added a JDK-only build/test route, a runnable local JAR, 93 original corpus programs, conversion/property tests, and concise specification/design/provenance documentation.
- Required a **method-level** terminal `return;`. Previously, the last return inside a nested block could incorrectly satisfy the method's final-return requirement.
- Added source-line context to name-resolution/initialization diagnostics. Existing line-qualified errors retain their original location. Unclosed blocks now report the end-of-file source line.
- Rejected malformed lines during the structural pass. Previously, a malformed block header could cause a misleading later brace/global error.
- Mapped invalid filesystem paths to the existing `2` result instead of leaving an unchecked path exception.
- Added exception serialization IDs for warning-clean compilation; removed a stale import, unused/commented code, and misleading numeric-range/compiler terminology. No new language feature was added.

Stdout remains the coursework-compatible `0`/`1`/`2` protocol. Language diagnostics now use `Verification error at line N: ...`; diagnostic prose is not a stable API.
