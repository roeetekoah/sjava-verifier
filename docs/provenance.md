# Authorship, source editions, and release boundary

## Joint coursework

The verifier was developed jointly by **Roee Tekoah and Ofri Mordechai** for the Hebrew University of Jerusalem object-oriented programming course, exercise 5 (January 2026). The original submission README credits both partners. This repository preserves that joint credit, including Ofri's work; it does not present the implementation as a solo project.

The available history is not detailed enough to establish a reliable class-by-class or line-by-line contribution split. Archive filenames are evidence of editions, not proof that every changed line belongs to the named person. No more specific individual attribution is claimed.

## Canonical edition

The completed submission directory is the baseline: its 24 implementation Java files match the implementation in its submission ZIP. The working directory has the same Java token streams; five files differ only in wrapping/comments, and the duplicate-style variable-exception filename is a filesystem accident. The older archives preserve development history. Their method verifier mutates shared global initialization state; the final submission instead isolates those effects per method, avoiding verification-order leakage. The Git index and HEAD contain earlier/incomplete work and are not the release baseline.

Original files and Git state were left in place. This is a curated extraction with a small, documented patch set, not an export of the private repository's history. A local internal audit retains edition hashes and diffs; those records and personal submission metadata are outside this repository.

## Supplied material versus implementation

The restricted language, CLI class/package convention (`ex5.main.Sjavac`), and numeric result protocol come from the course assignment. The layered verifier and model implementation are retained as the jointly credited coursework. The local collection includes assignment instructions, supplied presubmission tests, a school reference solution, community/shared test files, and IDE/submission artifacts. None of those teaching/reference artifacts is distributed here. No supplied implementation library or skeleton file was identified among the extracted 24 classes; this is a source inspection finding, not an independently verified authorship certificate.

## Portfolio preparation (October 2026)

Build/test tooling, the 93 curated examples, specification/design documentation, and the bounded fixes listed in `CHANGES.md` were prepared for Roee's portfolio with Codex assistance. These later additions are distinct from the January joint coursework and are not attributed to Ofri. The architecture diagram is newly authored from the retained source; the original UML PDF/image is not bundled.

No reuse license was found for the extracted coursework. No new general license or grant on behalf of either collaborator has been added. Publication and licensing decisions remain separate from this local preparation. See [NOTICE.md](../NOTICE.md).
