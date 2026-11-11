/**
 * Thrown when standard input is closed (Ctrl+D, end of piped input).
 * Fixes the old gym app's infinite menu loop on end-of-input.
 */
public class InputEndedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InputEndedException() {
        super("Input ended.");
    }
}
