import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * All of Theorem House's data and rules (built from the gym project's MembershipManager).
 * Same three collections, new jobs:
 *   1. List  - tutors and students in registration order
 *   2. Map   - O(1) lookup of any person by ID
 *   3. Queue - group classes still waiting to reach the minimum number of students (oldest first)
 * Slots and bookings live in their own collections. Time comes from an injectable clock so the
 * loyalty tiers can be tested.
 */
public class TheoremHouseManager {
    private final List<Person> memberList = new ArrayList<>();
    private final Map<String, Person> memberMap = new HashMap<>();
    private final Map<String, Slot> slotMap = new LinkedHashMap<>();
    private final List<Booking> bookingList = new ArrayList<>();
    private final Queue<String> pendingGroupQueue = new LinkedList<>();
    private final Supplier<LocalDate> clock;
    private int slotCounter = 0;
    private int bookingCounter = 0;

    public TheoremHouseManager() {
        this(LocalDate::now);
    }

    public TheoremHouseManager(Supplier<LocalDate> clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return clock.get();
    }

    private static String key(String id) {
        return id == null ? "" : id.trim().toUpperCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- people

    public boolean isIdTaken(String id) {
        return memberMap.containsKey(key(id));
    }

    /** Adds a tutor or student. IDs are unique across both, ignoring case. */
    public void addMember(Person member) throws DuplicateMemberException {
        if (memberMap.containsKey(member.getId())) {
            throw new DuplicateMemberException("ID '" + member.getId() + "' already exists.");
        }
        memberList.add(member);
        memberMap.put(member.getId(), member);
    }

    public List<Person> getAllMembers() {
        return new ArrayList<>(memberList);
    }

    public List<Tutor> getTutors() {
        List<Tutor> result = new ArrayList<>();
        for (Person p : memberList) {
            if (p instanceof Tutor) {
                result.add((Tutor) p);
            }
        }
        return result;
    }

    public List<Student> getStudents() {
        List<Student> result = new ArrayList<>();
        for (Person p : memberList) {
            if (p instanceof Student) {
                result.add((Student) p);
            }
        }
        return result;
    }

    public Person findMemberById(String id) throws MemberNotFoundException {
        Person member = memberMap.get(key(id));
        if (member == null) {
            throw new MemberNotFoundException("No member found with ID: " + id);
        }
        return member;
    }

    public Tutor findTutor(String id) throws MemberNotFoundException {
        Person member = memberMap.get(key(id));
        if (!(member instanceof Tutor)) {
            throw new MemberNotFoundException("No tutor found with ID: " + id);
        }
        return (Tutor) member;
    }

    public Student findStudent(String id) throws MemberNotFoundException {
        Person member = memberMap.get(key(id));
        if (!(member instanceof Student)) {
            throw new MemberNotFoundException("No student found with ID: " + id);
        }
        return (Student) member;
    }

    public void updateMemberName(String id, String newName)
            throws MemberNotFoundException, InvalidInputException {
        findMemberById(id).setName(newName);
    }

    public void updateMemberContact(String id, String newContact)
            throws MemberNotFoundException, InvalidInputException {
        findMemberById(id).setContactNumber(newContact);
    }

    public void updateStudentGrade(String id, int grade) throws MemberNotFoundException, InvalidInputException {
        findStudent(id).setGradeLevel(grade);
    }

    /** Changes which session types a tutor offers. Refuses to switch off a type that has slots. */
    public void updateTutorOffering(String id, boolean solo, boolean group)
            throws MemberNotFoundException, InvalidInputException, BookingException {
        Tutor tutor = findTutor(id);
        InputValidator.validateOffering(solo, group);
        for (Slot slot : getSlotsForTutor(tutor.getId())) {
            boolean stillOffered = slot.getType() == SessionType.SOLO ? solo : group;
            if (!stillOffered) {
                throw new BookingException("Remove your " + slot.getType().getLabel().toLowerCase(Locale.ROOT)
                        + " slots before turning that session type off.");
            }
        }
        tutor.setOffering(solo, group);
    }

    /**
     * Deletes a tutor or student. A student's active bookings are cancelled; a tutor's slots are
     * removed (cancelling every booking in them). Returns how many bookings were cancelled.
     */
    public int deleteMember(String id) throws MemberNotFoundException, BookingException {
        Person member = findMemberById(id);
        int cancelled = 0;
        if (member instanceof Student) {
            for (Booking b : getActiveBookingsForStudent(member.getId())) {
                cancelBooking(member.getId(), b.getId());
                cancelled++;
            }
        } else if (member instanceof Tutor) {
            for (Slot s : getSlotsForTutor(member.getId())) {
                cancelled += removeSlot(member.getId(), s.getId());
            }
        }
        memberMap.remove(member.getId());
        memberList.remove(member);
        return cancelled;
    }

    /** Testing/demo helper: moves a student's registration date so loyalty tiers can be checked. */
    public void setRegistrationDate(String studentId, LocalDate date)
            throws MemberNotFoundException, InvalidInputException {
        Student student = findStudent(studentId);
        if (date == null) {
            throw new InvalidInputException("Registration date is required.");
        }
        if (date.isAfter(today())) {
            throw new InvalidInputException("Registration date cannot be in the future.");
        }
        student.setRegistrationDate(date);
    }

    // ----------------------------------------------------------------- slots

    public Slot findSlot(String slotId) throws BookingException {
        Slot slot = slotMap.get(key(slotId));
        if (slot == null) {
            throw new BookingException("No slot found with ID: " + slotId);
        }
        return slot;
    }

    public List<Slot> getSlotsForTutor(String tutorId) {
        List<Slot> result = new ArrayList<>();
        for (Slot s : slotMap.values()) {
            if (s.getTutorId().equals(key(tutorId))) {
                result.add(s);
            }
        }
        result.sort(Comparator.comparing(Slot::getDay).thenComparing(Slot::getStart));
        return result;
    }

    public Slot addSlot(String tutorId, DayOfWeek day, LocalTime start, LocalTime end, SessionType type)
            throws MemberNotFoundException, InvalidInputException, BookingException {
        Tutor tutor = findTutor(tutorId);
        if (day == null || start == null || end == null || type == null) {
            throw new InvalidInputException("Day, start time, end time and session type are required.");
        }
        if (!end.isAfter(start)) {
            throw new InvalidInputException("End time must be after start time.");
        }
        if (!tutor.offers(type)) {
            throw new InvalidInputException("You do not offer " + type.getLabel().toLowerCase(Locale.ROOT)
                    + " sessions. Update your profile first.");
        }
        for (Slot existing : getSlotsForTutor(tutor.getId())) {
            if (Slot.timesOverlap(day, start, end, existing.getDay(), existing.getStart(), existing.getEnd())) {
                throw new BookingException("This overlaps your slot " + existing.getId()
                        + " (" + existing.getTimeLabel() + ").");
            }
        }
        Slot slot = new Slot(String.format("SL%03d", ++slotCounter), tutor.getId(), day, start, end, type);
        slotMap.put(slot.getId(), slot);
        if (slot.isGroup()) {
            pendingGroupQueue.offer(slot.getId());
        }
        return slot;
    }

    /** Removes a slot and cancels every booking in it. Returns the number of bookings cancelled. */
    public int removeSlot(String tutorId, String slotId) throws BookingException {
        Slot slot = findSlot(slotId);
        if (!slot.getTutorId().equals(key(tutorId))) {
            throw new BookingException("Slot " + slot.getId() + " does not belong to you.");
        }
        int cancelled = 0;
        for (Booking b : slot.getBookings()) {
            b.setStatus(Booking.Status.CANCELLED);
            slot.removeBooking(b);
            cancelled++;
        }
        slotMap.remove(slot.getId());
        pendingGroupQueue.remove(slot.getId());
        return cancelled;
    }

    /** Slots a student could still book for the given type (and grade, for group classes). */
    public List<Slot> getOpenSlots(SessionType type, int grade) {
        List<Slot> result = new ArrayList<>();
        for (Slot s : slotMap.values()) {
            if (s.getType() != type) {
                continue;
            }
            if (type == SessionType.SOLO) {
                if (s.activeCount() == 0) {
                    result.add(s);
                }
            } else if (s.activeCount() < Slot.GROUP_MAX && (s.getGrade() == 0 || s.getGrade() == grade)) {
                result.add(s);
            }
        }
        result.sort(Comparator.comparing(Slot::getDay).thenComparing(Slot::getStart).thenComparing(Slot::getId));
        return result;
    }

    /** Group classes still below the minimum, oldest first. */
    public List<Slot> getPendingGroupClasses() {
        List<Slot> result = new ArrayList<>();
        for (String id : pendingGroupQueue) {
            Slot s = slotMap.get(id);
            if (s != null) {
                result.add(s);
            }
        }
        return result;
    }

    public int getQueueCount() {
        return pendingGroupQueue.size();
    }

    // -------------------------------------------------------------- bookings

    public Booking bookSlot(String studentId, String slotId, int grade, String unit)
            throws MemberNotFoundException, InvalidInputException, BookingException {
        Student student = findStudent(studentId);
        InputValidator.validateGrade(grade);
        if (unit == null || unit.trim().isEmpty()) {
            throw new InvalidInputException("Please choose a topic.");
        }
        Slot slot = findSlot(slotId);

        for (Booking b : slot.getBookings()) {
            if (b.getStudentId().equals(student.getId())) {
                throw new BookingException("You already have a booking in this slot.");
            }
        }
        if (slot.getType() == SessionType.SOLO) {
            if (slot.activeCount() > 0) {
                throw new BookingException("This solo slot is already booked.");
            }
        } else {
            if (slot.activeCount() >= Slot.GROUP_MAX) {
                throw new BookingException("This group class is full (" + Slot.GROUP_MAX + "/" + Slot.GROUP_MAX + ").");
            }
            if (slot.getGrade() != 0 && slot.getGrade() != grade) {
                throw new BookingException("This group class is for Grade " + slot.getGrade() + ".");
            }
        }
        for (Booking other : getActiveBookingsForStudent(student.getId())) {
            Slot otherSlot = slotMap.get(other.getSlotId());
            if (otherSlot != null && slot.overlaps(otherSlot)) {
                throw new BookingException("You already have a booking at that time (" + otherSlot.getId()
                        + ", " + otherSlot.getTimeLabel() + ").");
            }
        }

        LocalDate today = today();
        Booking booking = new Booking(String.format("BK%03d", ++bookingCounter), student.getId(), slot.getId(),
                grade, unit.trim(), slot.getType(), student.computePrice(slot.getType(), today),
                student.getLoyaltyDiscountPercent(today));
        slot.addBooking(booking);
        bookingList.add(booking);
        if (slot.isGroup()) {
            if (slot.getGrade() == 0) {
                slot.setGrade(grade);
            }
            refreshGroup(slot);
        } else {
            booking.setStatus(Booking.Status.CONFIRMED);
        }
        return booking;
    }

    public Booking cancelBooking(String studentId, String bookingId)
            throws MemberNotFoundException, BookingException {
        Student student = findStudent(studentId);
        Booking booking = null;
        for (Booking b : bookingList) {
            if (b.getId().equals(key(bookingId)) && b.getStudentId().equals(student.getId())) {
                booking = b;
                break;
            }
        }
        if (booking == null) {
            throw new BookingException("No booking found with ID: " + bookingId);
        }
        if (!booking.isActive()) {
            throw new BookingException("Booking " + booking.getId() + " is already cancelled.");
        }
        booking.setStatus(Booking.Status.CANCELLED);
        Slot slot = slotMap.get(booking.getSlotId());
        if (slot != null) {
            slot.removeBooking(booking);
            if (slot.isGroup()) {
                if (slot.activeCount() == 0) {
                    slot.setGrade(0);
                }
                refreshGroup(slot);
            }
        }
        return booking;
    }

    public List<Booking> getBookingsForStudent(String studentId) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookingList) {
            if (b.getStudentId().equals(key(studentId))) {
                result.add(b);
            }
        }
        return result;
    }

    public List<Booking> getActiveBookingsForStudent(String studentId) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : getBookingsForStudent(studentId)) {
            if (b.isActive()) {
                result.add(b);
            }
        }
        return result;
    }

    /**
     * Keeps a group class's bookings and its place in the pending queue consistent with its size:
     * below the minimum everyone is PENDING and the class waits in the queue; at the minimum or
     * above everyone is CONFIRMED (the class is Scheduled) and it leaves the queue.
     */
    private void refreshGroup(Slot slot) {
        boolean scheduled = slot.activeCount() >= Slot.GROUP_MIN;
        for (Booking b : slot.getBookings()) {
            b.setStatus(scheduled ? Booking.Status.CONFIRMED : Booking.Status.PENDING);
        }
        if (scheduled) {
            pendingGroupQueue.remove(slot.getId());
        } else if (!pendingGroupQueue.contains(slot.getId())) {
            pendingGroupQueue.offer(slot.getId());
        }
    }
}
