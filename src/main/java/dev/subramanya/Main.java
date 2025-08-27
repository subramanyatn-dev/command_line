package dev.subramanya;

// Import the Picocli classes we need
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

// Import Java utilities
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * This is our main CLI program.
 * It has subcommands:
 *   - ls    → list files
 *   - show  → show file contents
 *   - shell → interactive mode
 */
@Command(
        name = "mini",                                    // The base command name
        description = "A very tiny CLI program",          // Help description
        version = "mini 1.0.0",                           // Version string
        mixinStandardHelpOptions = true,                  // Add --help, --version
        subcommands = {                                   // Register subcommands
                Main.LsCommand.class,
                Main.ShowCommand.class,
                Main.ShellCommand.class
        }
)
public class Main implements Runnable {

    /**
     * Java entry point.
     * This creates a Picocli "CommandLine" object around Main,
     * and passes the command line arguments to it.
     */
    public static void main(String[] args) {
        // Wrap Main in a CommandLine object
        CommandLine cli = new CommandLine(new Main());

        // Execute the CLI with the provided args
        int exitCode = cli.execute(args);

        // Exit program with that code
        System.exit(exitCode);
    }

    /**
     * If user types just "mini" with no arguments,
     * this will run and show help text.
     */
    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    // ======================
    // Subcommand: mini ls
    // ======================
    @Command(
            name = "ls",
            description = "List files in a directory (default: current directory)"
    )
    static class LsCommand implements Callable<Integer> {

        // Argument: the directory to list (optional)
        @Parameters(
                index = "0",                     // first argument
                arity = "0..1",                  // 0 or 1 args
                paramLabel = "DIR",              // label in help
                description = "Directory to list (default is current directory)"
        )
        Path dir = Path.of(".");                 // default value is "."

        @Override
        public Integer call() {
            try (Stream<Path> paths = Files.list(dir)) {
                // Print every file/directory name
                paths.forEach(path -> System.out.println(path.getFileName()));
                return 0; // success
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                return 1; // error exit code
            }
        }
    }

    // ======================
    // Subcommand: mini show <file>
    // ======================
    @Command(
            name = "show",
            description = "Show the contents of a file"
    )
    static class ShowCommand implements Callable<Integer> {

        // Argument: the file to display
        @Parameters(
                index = "0",                     // first and only argument
                paramLabel = "FILE",             // label in help
                description = "File to display"
        )
        Path file;                               // no default, must be given

        @Override
        public Integer call() {
            try {
                if (!Files.exists(file)) {
                    System.err.println("Error: file not found: " + file);
                    return 2;
                }

                // Read whole file into a string
                String contents = Files.readString(file);
                System.out.print(contents);
                return 0; // success
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                return 1; // error exit code
            }
        }
    }

    // ======================
    // Subcommand: mini shell
    // ======================
    @Command(
            name = "shell",
            description = "Interactive mode (type commands like 'ls', 'show file', 'exit')"
    )
    static class ShellCommand implements Callable<Integer> {

        @Override
        public Integer call() {
            // Create a root CLI parser that knows about all commands
            CommandLine root = new CommandLine(new Main());

            try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
                System.out.println("mygit shell — type 'exit' to quit");

                while (true) {
                    // Print prompt
                    System.out.print("mygit> ");

                    // Read a line from user
                    String line = in.readLine();

                    // End of input (Ctrl+D)
                    if (line == null) break;

                    line = line.trim();

                    // Ignore empty lines
                    if (line.isEmpty()) continue;

                    // Exit commands
                    if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                        break;
                    }

                    // Split input by spaces into arguments
                    String[] args = line.split("\\s+");

                    // Execute as if typed at CLI
                    try {
                        int code = root.execute(args);
                        if (code != 0) {
                            System.err.println("Command failed with exit code " + code);
                        }
                    } catch (Exception e) {
                        System.err.println("Error running command: " + e.getMessage());
                    }
                }

            } catch (Exception e) {
                System.err.println("Fatal error in shell: " + e.getMessage());
                return 1;
            }

            return 0; // normal exit
        }
    }
}