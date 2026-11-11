/**
 * One student's seat in one slot. The price and discount are locked in at the moment of booking.
 * Solo bookings are CONFIRMED immediately; group bookings stay PENDING until the class has
 * enough students to be Scheduled, and drop back to PENDING if it falls below the minimum again.
 */
public class Booking {

    public enum Status { PENDING, CONFIRMED, CANCELLED }

    private final String id;
    private final String studentId;
    private final String slotId;
    private final int grade;
    private final String unit;
    private final SessionType type;
    private final double price;
    private final int discountPercent;
    private Status status = Status.PENDING;

    public Booking(String id, String studentId, String slotId, int grade, String unit,
                   SessionType type, double price, int discountPercent) {
        this.id = id;
        this.studentId = studentId;
        this.slotId = slotId;
        this.grade = grade;
        this.unit = unit;
        this.type = type;
        this.price = price;
        this.discountPercent = discountPercent;
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getSlotId() { return slotId; }
    public int getGrade() { return grade; }
    public String getUnit() { return unit; }
    public SessionType getType() { return type; }
    public double getPrice() { return price; }
    public int getDiscountPercent() { return discountPercent; }
    public Status getStatus() { return status; }

    public void setStatus(Status status) { this.status = status; }

    public boolean isActive() { return status != Status.CANCELLED; }
}
