import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.ToolProvider;

/** Dependency-free build/test helper. Run from the repository root with a JDK. */
class Project {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !Set.of("build", "test").contains(args[0])) {
            System.err.println("Usage: java tools/Project.java <build|test>");
            System.exit(2);
        }
        if (!Files.isDirectory(Path.of("src/ex5"))) {
            throw new IllegalStateException("Run from the repository root");
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A full JDK 17 or newer is required");
        Path classes = Path.of("build/classes");
        Files.createDirectories(classes);
        List<String> options = new ArrayList<>(List.of("--release", "17", "-encoding", "UTF-8",
                "-Xlint:all", "-Werror", "-d", classes.toString()));
        try (var sources = Files.walk(Path.of("src"))) {
            sources.filter(p -> p.toString().endsWith(".java")).sorted()
                    .forEach(p -> options.add(p.toString()));
        }
        if (compiler.run(null, null, null, options.toArray(String[]::new)) != 0) System.exit(1);
        // Package only classes corresponding to current sources, never stale build files.
        Set<String> sourceNames = new HashSet<>();
        try (var sources = Files.walk(Path.of("src"))) {
            sources.filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> sourceNames.add(Path.of("src").relativize(p).toString()
                            .replace('\\', '/').replaceAll("\\.java$", "")));
        }
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Main-Class", "ex5.main.Sjavac");
        try (var jar = new JarOutputStream(Files.newOutputStream(Path.of("build/sjava-verifier.jar")), manifest);
             var entries = Files.walk(classes)) {
            for (Path file : entries.filter(Files::isRegularFile).sorted().toList()) {
                String name = classes.relativize(file).toString().replace('\\', '/');
                String owner = name.replaceAll("(?:\\$.*)?\\.class$", "");
                if (!sourceNames.contains(owner)) continue;
                JarEntry entry = new JarEntry(name);
                entry.setTime(0);
                jar.putNextEntry(entry);
                Files.copy(file, jar);
                jar.closeEntry();
            }
        }
        System.out.println("Built build/sjava-verifier.jar (Java 17 bytecode)");
        if (args[0].equals("test")) {
            Path testClasses = Path.of("build/test-classes");
            Files.createDirectories(testClasses);
            int result = compiler.run(null, null, null, "--release", "17", "-encoding", "UTF-8",
                    "-Xlint:all", "-Werror", "-cp", classes.toString(), "-d", testClasses.toString(),
                    "tests/VerifierTests.java");
            if (result != 0) System.exit(1);
            String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            String cp = classes + System.getProperty("path.separator") + testClasses;
            System.exit(new ProcessBuilder(java, "-cp", cp, "VerifierTests").inheritIO().start().waitFor());
        }
    }
}
