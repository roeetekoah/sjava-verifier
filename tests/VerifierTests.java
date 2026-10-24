import ex5.main.Sjavac;
import ex5.syntax.SjavaSyntaxException;
import ex5.verifier.Verifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Black-box corpus, CLI contract, and specification-derived semantic properties. */
public final class VerifierTests {
    private static int checks;
    private record Output(int exit, String out, String err) {}
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static Output run(String... args) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> command = new ArrayList<>(List.of(java, "-jar", "build/sjava-verifier.jar"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("CLI timeout: " + command);
        }
        return new Output(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }
    private static void expectOutput(Output output, int status, String label) {
        // Assignment-compatible result is stdout; the Java process normally exits zero.
        check(output.exit == 0, label + ": process exit " + output.exit);
        check(output.out.equals(status + System.lineSeparator()), label + ": stdout " + output.out);
        check(status == 0 ? output.err.isEmpty() : !output.err.isBlank(), label + ": stderr " + output.err);
    }
    private static void corpus() throws Exception {
        List<String> rows = Files.readAllLines(Path.of("corpus/cases.tsv"), StandardCharsets.UTF_8);
        Set<String> seen = new HashSet<>();
        for (String row : rows.subList(1, rows.size())) {
            String[] fields = row.split("\t", -1);
            check(fields.length == 5, "Malformed corpus manifest row");
            check(seen.add(fields[0]), "Duplicate corpus path: " + fields[0]);
            Output output = run("corpus/" + fields[0]);
            int status = Integer.parseInt(fields[1]);
            expectOutput(output, status, fields[0]);
            if (status == 1) {
                check(output.err.contains(fields[3]), fields[0] + ": diagnostic " + output.err);
                check(output.err.contains("at line " + fields[4] + ":"),
                        fields[0] + ": source location " + output.err);
            }
        }
        try (var programs = Files.walk(Path.of("corpus"))) {
            Set<String> actual = new HashSet<>();
            programs.filter(p -> p.toString().endsWith(".sjava"))
                    .forEach(p -> actual.add(Path.of("corpus").relativize(p).toString().replace('\\', '/')));
            check(actual.equals(seen), "Manifest must cover every corpus program exactly once");
        }
        System.out.println("PASS " + seen.size() + " CLI corpus programs");
    }
    private static void cliErrors() throws Exception {
        expectOutput(run(), 2, "missing argument");
        expectOutput(run("a", "b"), 2, "extra argument");
        expectOutput(run("README.md"), 2, "wrong suffix");
        expectOutput(run("missing-source.sjava"), 2, "missing file");
        Path dir = Files.createTempDirectory("sjava-cli-");
        try {
            Path sourceDir = Files.createDirectory(dir.resolve("directory.sjava"));
            expectOutput(run(sourceDir.toString()), 2, "directory instead of source");
            Path malformed = dir.resolve("invalid-utf8.sjava");
            Files.write(malformed, new byte[]{(byte)0xc3, (byte)0x28});
            expectOutput(run(malformed.toString()), 2, "invalid UTF-8");
            Path uppercase = dir.resolve("upper.SJAVA");
            Files.writeString(uppercase, "");
            expectOutput(run(uppercase.toString()), 2, "suffix is case-sensitive");
            Path spaces = dir.resolve("source with spaces.sjava");
            Files.writeString(spaces, "int value = 1;\n");
            expectOutput(run(spaces.toString()), 0, "spaces in source path");
            Path crlf = dir.resolve("crlf.sjava");
            Files.writeString(crlf, "void inspect() {\r\nreturn;\r\n}\r\n");
            expectOutput(run(crlf.toString()), 0, "CRLF input");
            // NUL cannot be supplied by a process launcher; invoke main to check path handling.
            PrintStream originalOut = System.out, originalErr = System.err;
            var stdout = new ByteArrayOutputStream();
            var stderr = new ByteArrayOutputStream();
            try (var out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
                 var err = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
                System.setOut(out);
                System.setErr(err);
                Sjavac.main(new String[]{"bad\0path.sjava"});
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
            expectOutput(new Output(0, stdout.toString(StandardCharsets.UTF_8),
                    stderr.toString(StandardCharsets.UTF_8)), 2, "invalid path");
        } finally {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("PASS 10 CLI input/error cases");
    }
    private static void expectSemantic(Verifier verifier, String code, boolean accepted, String label) {
        boolean actual;
        try {
            verifier.verify(code.lines().toList());
            actual = true;
        } catch (SjavaSyntaxException e) {
            actual = false;
        }
        check(actual == accepted, label + ": expected acceptance " + accepted);
    }
    private static String method(String statement) {
        return "void inspect() {\n" + statement + "\nreturn;\n}\n";
    }
    private static void properties() throws Exception {
        Verifier verifier = new Verifier();
        String[] types = {"int", "double", "boolean", "char", "String"};
        String[] literals = {"1", ".5", "true", "'x'", "\"text\""};
        // Rows are target types; columns are source types. Independent expected contract.
        boolean[][] assignable = {
                {true, false, false, false, false},
                {true, true, false, false, false},
                {true, true, true, false, false},
                {false, false, false, true, false},
                {false, false, false, false, true}};
        for (int target = 0; target < types.length; target++) {
            for (int source = 0; source < types.length; source++) {
                String prefix = types[source] + " source = " + literals[source] + ";\n";
                String declaration = types[target] + " target = source;";
                expectSemantic(verifier, method(prefix + declaration), assignable[target][source], "declaration matrix");
                expectSemantic(verifier, method(prefix + types[target] + " target;\ntarget = source;"),
                        assignable[target][source], "assignment matrix");
                String callee = "void consume(" + types[target] + " value) {\nreturn;\n}\n";
                expectSemantic(verifier, callee + method(prefix + "consume(source);"),
                        assignable[target][source], "call matrix");
                expectSemantic(verifier, types[target]+" target = "+literals[source]+";\n",
                        assignable[target][source], "literal matrix");
            }
        }
        String base = Files.readString(Path.of("corpus/valid/walkthrough.sjava"));
        expectSemantic(verifier, base.replace("total", "_counter9").replace("consume", "consume2"), true, "alpha rename");
        String formatted = base.lines().map(s -> s.isBlank() || s.startsWith("//") ? s : "\t"+s+"  ")
                .reduce("", (a,b) -> a+b+"\n");
        expectSemantic(verifier, formatted, true, "whitespace transformation");
        String first = "void first() {\nsecond();\nreturn;\n}\n";
        String second = "void second() {\nreturn;\n}\n";
        expectSemantic(verifier, first+second, true, "method order A");
        expectSemantic(verifier, second+first, true, "method order B");
        expectSemantic(verifier, method("int x = missing;"), false, "reused verifier rejects");
        expectSemantic(verifier, method("int x = 1;"), true, "reused verifier recovers");
        expectSemantic(verifier, method("int x;\nx = 1;\nint y = x;"), true, "init mutation baseline");
        expectSemantic(verifier, method("int x;\nint y = x;"), false, "removed initialization");
        String isolation = "int shared;\nvoid write() {\nshared = 1;\nreturn;\n}\n"
                + "void read() {\nint x = shared;\nreturn;\n}\n";
        expectSemantic(verifier, isolation, false, "method effects do not leak");
        StringBuilder nested = new StringBuilder("void inspect() {\nint value = 1;\n");
        for (int depth = 0; depth < 300; depth++) nested.append("if (value) {\n");
        nested.append("int copy = value;\n");
        nested.append("}\n".repeat(300)).append("return;\n}\n");
        expectSemantic(verifier, nested.toString(), true, "300 nested scopes");
        StringBuilder many = new StringBuilder("int global = 1;\n");
        for (int i = 0; i < 60; i++) {
            many.append("void method").append(i).append("() {\nint local = global;\nreturn;\n}\n");
        }
        expectSemantic(verifier, many.toString(), true, "60 independent methods");
        System.out.println("PASS 111 generated semantic cases (100 conversion checks + 11 properties)");
    }
    public static void main(String[] args) throws Exception {
        corpus();
        cliErrors();
        properties();
        System.out.println("PASS all checks: " + checks);
    }
}
