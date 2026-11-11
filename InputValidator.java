import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class InputValidator {

    /** Trims, upper-cases and checks an ID, so "t01" and "T01" are the same ID. */
    public static String normalizeId(String id) throws InvalidInputException {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidInputException("ID cannot be empty.");
        }
        String normalized = id.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_-]{1,20}")) {
            throw new InvalidInputException(
                    "ID may only contain letters, digits, '-' or '_' (max 20 characters, no spaces).");
        }
        return normalized;
    }

    public static void validateId(String id) throws InvalidInputException {
        normalizeId(id);
    }

    public static void validateName(String name) throws InvalidInputException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Name cannot be empty.");
        }
        if (name.trim().length() > 60) {
            throw new InvalidInputException("Name is too long (max 60 characters).");
        }
    }

    public static void validateContact(String contact) throws InvalidInputException {
        if (contact == null || !contact.trim().matches("\\d{11}")) {
            throw new InvalidInputException("Contact number must contain exactly 11 digits.");
        }
    }

    public static void validateGrade(int grade) throws InvalidInputException {
        if (grade < Curriculum.MIN_GRADE || grade > Curriculum.MAX_GRADE) {
            throw new InvalidInputException("Grade level must be between "
                    + Curriculum.MIN_GRADE + " and " + Curriculum.MAX_GRADE + ".");
        }
    }

    public static void validateOffering(boolean offersSolo, boolean offersGroup) throws InvalidInputException {
        if (!offersSolo && !offersGroup) {
            throw new InvalidInputException("A tutor must offer at least one session type (solo or group).");
        }
    }

    public static void validateChoice(int choice, int min, int max, String what) throws InvalidInputException {
        if (choice < min || choice > max) {
            throw new InvalidInputException("Invalid " + what + ". Choose " + min + " to " + max + ".");
        }
    }

    /** Accepts Mon..Sun, full day names (any case), or 1 (Monday) to 7 (Sunday). */
    public static DayOfWeek parseDay(String text) throws InvalidInputException {
        if (text == null || text.trim().isEmpty()) {
            throw new InvalidInputException("Day cannot be empty. Use Mon, Tue, Wed, Thu, Fri, Sat or Sun.");
        }
        String t = text.trim().toLowerCase(Locale.ROOT);
        if (t.matches("[1-7]")) {
            return DayOfWeek.of(Integer.parseInt(t));
        }
        if (t.length() >= 3) {
            for (DayOfWeek d : DayOfWeek.values()) {
                if (d.name().toLowerCase(Locale.ROOT).startsWith(t)) {
                    return d;
                }
            }
        }
        throw new InvalidInputException("Invalid day '" + text.trim()
                + "'. Use Mon, Tue, Wed, Thu, Fri, Sat or Sun.");
    }

    /** Accepts 24-hour times such as 9:00, 09:00 or 15:30. */
    public static LocalTime parseTime(String text) throws InvalidInputException {
        if (text == null || !text.trim().matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
            throw new InvalidInputException("Invalid time. Use 24-hour HH:mm, for example 15:30.");
        }
        String[] parts = text.trim().split(":");
        return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /** Accepts ISO dates, yyyy-MM-dd. */
    public static LocalDate parseDate(String text) throws InvalidInputException {
        try {
            return LocalDate.parse(text == null ? "" : text.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Invalid date. Use yyyy-MM-dd, for example 2026-05-11.");
        }
    }
}
