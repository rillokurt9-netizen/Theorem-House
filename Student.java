import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

/**
 * A student looking for a tutor. Implements Billable: the loyalty discount depends on how long
 * the account has existed (months since the registration date).
 */
public class Student extends Person implements Billable {

    /** {months of membership needed, discount percent}, in ascending order. */
    public static final int[][] LOYALTY_TIERS = {{3, 5}, {6, 10}, {12, 15}};

    private int gradeLevel;
    private LocalDate registrationDate;

    public Student(String id, String name, String contactNumber, int gradeLevel, LocalDate registrationDate)
            throws InvalidInputException {
        super(id, name, contactNumber);
        InputValidator.validateGrade(gradeLevel);
        if (registrationDate == null) {
            throw new InvalidInputException("Registration date is required.");
        }
        this.gradeLevel = gradeLevel;
        this.registrationDate = registrationDate;
    }

    public int getGradeLevel() { return gradeLevel; }
    public LocalDate getRegistrationDate() { return registrationDate; }

    public void setGradeLevel(int gradeLevel) throws InvalidInputException {
        InputValidator.validateGrade(gradeLevel);
        this.gradeLevel = gradeLevel;
    }

    public void setRegistrationDate(LocalDate registrationDate) throws InvalidInputException {
        if (registrationDate == null) {
            throw new InvalidInputException("Registration date is required.");
        }
        this.registrationDate = registrationDate;
    }

    /** Whole months between registration and the given day (0 if the day is before registration). */
    public int getAccountAgeMonths(LocalDate today) {
        if (today.isBefore(registrationDate)) {
            return 0;
        }
        return (int) Period.between(registrationDate, today).toTotalMonths();
    }

    /** Discount percent earned after the given number of whole months. */
    public static int discountForMonths(int months) {
        int percent = 0;
        for (int[] tier : LOYALTY_TIERS) {
            if (months >= tier[0]) {
                percent = tier[1];
            }
        }
        return percent;
    }

    /** The next loyalty tier as {months, percent}, or null if the student is already at the top. */
    public int[] getNextTier(LocalDate today) {
        int months = getAccountAgeMonths(today);
        for (int[] tier : LOYALTY_TIERS) {
            if (months < tier[0]) {
                return tier;
            }
        }
        return null;
    }

    @Override
    public int getLoyaltyDiscountPercent(LocalDate today) {
        return discountForMonths(getAccountAgeMonths(today));
    }

    @Override
    public double computePrice(SessionType type, LocalDate today) {
        int percent = getLoyaltyDiscountPercent(today);
        return BigDecimal.valueOf(type.getBasePrice())
                .multiply(BigDecimal.valueOf(100 - percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public String getRole() {
        return "Student (Grade " + gradeLevel + ")";
    }

    @Override
    public void displayInfo() {
        super.displayInfo();
        LocalDate today = LocalDate.now();
        System.out.println("  Registered: " + registrationDate
                + " | Account age: " + getAccountAgeMonths(today) + " month(s)"
                + " | Loyalty discount: " + getLoyaltyDiscountPercent(today) + "%");
    }
}
