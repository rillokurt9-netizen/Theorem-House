import java.util.Collections;

/** The "What we offer" screen. All figures come from the same constants the booking logic uses. */
public final class Offerings {

    private Offerings() {
    }

    public static void print() {
        System.out.println("\n=== WHAT THEOREM HOUSE OFFERS ===");
        System.out.println("Online one-to-one and small-group tutoring.");
        System.out.println("Subjects: " + String.join(", ", Curriculum.SUBJECTS)
                + " (Grades " + Curriculum.MIN_GRADE + "-" + Curriculum.MAX_GRADE + ")");

        System.out.println("\nSession types (price per session, per student):");
        System.out.println("  Solo  : " + Billable.formatMoney(SessionType.SOLO.getBasePrice())
                + " - one student with one tutor");
        System.out.println("  Group : " + Billable.formatMoney(SessionType.GROUP.getBasePrice())
                + " - " + Slot.GROUP_MIN + " to " + Slot.GROUP_MAX + " students with one tutor");

        System.out.println("\nHow group classes work:");
        System.out.println("  - A class stays Pending until " + Slot.GROUP_MIN + " students have joined.");
        System.out.println("  - At " + Slot.GROUP_MIN + " students it becomes Scheduled;"
                + " up to " + Slot.GROUP_MAX + " can join, then it is Full.");
        System.out.println("  - You are charged only once the class is Scheduled.");

        System.out.println("\nLoyalty discount (the longer your account, the lower your price):");
        System.out.println("  " + padRight("Account age", 18) + "Discount");
        System.out.println("  " + padRight("Under " + Student.LOYALTY_TIERS[0][0] + " months", 18) + "0%");
        for (int i = 0; i < Student.LOYALTY_TIERS.length; i++) {
            int[] tier = Student.LOYALTY_TIERS[i];
            System.out.println("  " + padRight(tier[0] + " months or more", 18) + tier[1] + "%");
        }
        System.out.println("  Applies to both solo and group sessions.");

        System.out.println("\nExample prices at the top loyalty tier ("
                + Student.LOYALTY_TIERS[Student.LOYALTY_TIERS.length - 1][1] + "% off):");
        int top = Student.LOYALTY_TIERS[Student.LOYALTY_TIERS.length - 1][1];
        for (SessionType type : SessionType.values()) {
            double price = type.getBasePrice() * (100 - top) / 100.0;
            System.out.println("  " + type.getLabel() + ": " + Billable.formatMoney(price));
        }
        System.out.println("\nFor the full list of topics, see the Curriculum screen.");
    }

    private static String padRight(String text, int width) {
        return text + String.join("", Collections.nCopies(Math.max(0, width - text.length()), " "));
    }
}
