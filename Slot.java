import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A weekly availability slot published by a tutor, for either a solo session or a group class.
 * A solo slot holds at most one booking. A group slot holds up to GROUP_MAX bookings and is
 * Scheduled once it has GROUP_MIN. Only active (non-cancelled) bookings are stored here.
 */
public class Slot {
    public static final int GROUP_MIN = 5;
    public static final int GROUP_MAX = 8;

    private final String id;
    private final String tutorId;
    private final DayOfWeek day;
    private final LocalTime start;
    private final LocalTime end;
    private final SessionType type;
    private int grade; // group classes only: grade of the first student who joined, 0 while empty
    private final List<Booking> bookings = new ArrayList<>();

    public Slot(String id, String tutorId, DayOfWeek day, LocalTime start, LocalTime end, SessionType type) {
        this.id = id;
        this.tutorId = tutorId;
        this.day = day;
        this.start = start;
        this.end = end;
        this.type = type;
    }

    public String getId() { return id; }
    public String getTutorId() { return tutorId; }
    public DayOfWeek getDay() { return day; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
    public SessionType getType() { return type; }
    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public boolean isGroup() { return type == SessionType.GROUP; }

    public List<Booking> getBookings() { return new ArrayList<>(bookings); }
    public void addBooking(Booking booking) { bookings.add(booking); }
    public void removeBooking(Booking booking) { bookings.remove(booking); }
    public int activeCount() { return bookings.size(); }

    /** True for a group class that has reached its minimum size. */
    public boolean isScheduled() { return isGroup() && bookings.size() >= GROUP_MIN; }

    public String getTimeLabel() {
        return day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + start + "-" + end;
    }

    public String getStatusLabel() {
        int n = bookings.size();
        if (!isGroup()) {
            return n == 0 ? "Open" : "Booked";
        }
        if (n < GROUP_MIN) {
            return "Pending (" + n + "/" + GROUP_MIN + ")";
        }
        if (n < GROUP_MAX) {
            return "Scheduled (" + n + "/" + GROUP_MAX + ")";
        }
        return "Full (" + n + "/" + GROUP_MAX + ")";
    }

    public static boolean timesOverlap(DayOfWeek d1, LocalTime s1, LocalTime e1,
                                       DayOfWeek d2, LocalTime s2, LocalTime e2) {
        return d1 == d2 && s1.isBefore(e2) && s2.isBefore(e1);
    }

    public boolean overlaps(Slot other) {
        return timesOverlap(day, start, end, other.day, other.start, other.end);
    }
}
