package ex5.main;

import ex5.syntax.SjavaSyntaxException;
import ex5.verifier.Verifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 Entry point for the Sjava semantic verifier (sjavac).
 */
public final class Sjavac {

    private Sjavac() {}

    /**
     * The main method of the SJava verifier.
     * Prints 0 if the code is legal, 1 if it's illegal, and 2 on IO errors.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            Path file = ArgumentParser.parseArgs(args);
            List<String> lines = Files.readAllLines(file);

            new Verifier().verify(lines);                   // throws SjavaSyntaxException on illegal code

            System.out.println(0);
        } catch (InvalidArgumentsException | InvalidFileException e) {
            System.err.println(e.getMessage());
            System.out.println(2);
        } catch (SjavaSyntaxException e) {
            System.err.println(e.getMessage());
            System.out.println(1);
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
            System.out.println(2);
        }
    }
}
