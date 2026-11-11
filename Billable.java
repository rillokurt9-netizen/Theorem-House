import java.time.LocalDate;
import java.util.Locale;

/**
 * Anything that can be charged for a tutoring session.
 * (Replaces the gym project's Billable: monthly fees became per-session prices.)
 */
public interface Billable {

    /** Loyalty discount (0-100) that applies on the given day. */
    int getLoyaltyDiscountPercent(LocalDate today);

    /** Price for one session of the given type after the loyalty discount, rounded to cents. */
    double computePrice(SessionType type, LocalDate today);

    /** Formats an amount as US dollars, e.g. $13.50, independent of the machine's locale. */
    static String formatMoney(double amount) {
        return String.format(Locale.US, "$%.2f", amount);
    }
}
