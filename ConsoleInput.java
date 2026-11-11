import java.io.InputStream;
import java.util.Scanner;

/** Thin wrapper over Scanner so every prompt handles end-of-input and number parsing the same way. */
public class ConsoleInput {
    private final Scanner scanner;

    public ConsoleInput(InputStream stream) {
        this.scanner = new Scanner(stream);
    }

    /** Prints the prompt and returns the trimmed line. Throws InputEndedException at end of input. */
    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new InputEndedException();
        }
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt, String fieldName) throws InvalidInputException {
        String text = readLine(prompt);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InvalidInputException(fieldName + " must be a whole number.");
        }
    }

    public boolean readYesNo(String prompt) {
        return readLine(prompt).equalsIgnoreCase("Y");
    }

    public void close() {
        scanner.close();
    }
}
