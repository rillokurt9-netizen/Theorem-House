/**
 * The two kinds of tutoring session Theorem House sells, with their base price in USD.
 * Solo is one student with one tutor; Group is 5 to 8 students sharing one tutor.
 */
public enum SessionType {
    SOLO("Solo", 15.00),
    GROUP("Group", 10.00);

    private final String label;
    private final double basePrice;

    SessionType(String label, double basePrice) {
        this.label = label;
        this.basePrice = basePrice;
    }

    public String getLabel() {
        return label;
    }

    public double getBasePrice() {
        return basePrice;
    }
}
