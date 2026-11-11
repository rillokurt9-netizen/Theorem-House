/** Thrown when a booking or slot rule is broken (slot full, time clash, unknown slot, and so on). */
public class BookingException extends Exception {
    private static final long serialVersionUID = 1L;

    public BookingException(String message) {
        super(message);
    }
}
