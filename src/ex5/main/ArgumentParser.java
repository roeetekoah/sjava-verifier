package ex5.main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;

/**
 * Validates:
 *  - exactly 1 argument
 *  - file exists
 *  - file ends with .sjava
 */
final class ArgumentParser {

    private static final String SJAVA_SUFFIX = ".sjava";

    private ArgumentParser() {}

    static Path parseArgs(String[] args) throws InvalidArgumentsException, InvalidFileException {
        if (args == null || args.length != 1) {
            throw new InvalidArgumentsException("Expected exactly one argument: <source_file_name>");
        }
        Path p;
        try {
            p = Path.of(args[0]);
        } catch (InvalidPathException e) {
            throw new InvalidFileException("Invalid source path");
        }
        if (!args[0].endsWith(SJAVA_SUFFIX)) {
            throw new InvalidFileException("Wrong file format: expected a .sjava file");
        }
        if (!Files.exists(p)) {
            throw new InvalidFileException("Invalid file name: file does not exist");
        }
        return p;
    }
}
