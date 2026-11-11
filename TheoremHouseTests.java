import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Self-contained test suite (no JUnit needed). Run with:
 *   javac *.java && java TheoremHouseTests
 * Exits with status 1 if any check fails.
 */
public class TheoremHouseTests {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);
    private static int passed = 0;
    private static int failed = 0;

    interface Body { void run() throws Exception; }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("  FAIL: " + name);
        }
    }

    private static void checkMoney(String name, double actual, double expected) {
        check(name + " (expected " + expected + ", got " + actual + ")", Math.abs(actual - expected) < 0.0001);
    }

    private static void expectThrows(String name, Class<? extends Exception> type, Body body) {
        try {
            body.run();
            failed++;
            System.out.println("  FAIL: " + name + " (no exception thrown)");
        } catch (Exception e) {
            if (type.isInstance(e)) {
                passed++;
            } else {
                failed++;
                System.out.println("  FAIL: " + name + " (threw " + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
            }
        }
    }

    private static TheoremHouseManager newManager() {
        return new TheoremHouseManager(() -> TODAY);
    }

    private static Tutor addTutor(TheoremHouseManager m, String id, boolean solo, boolean group) throws Exception {
        Tutor t = new Tutor(id, "Tutor " + id, "09170000000", Arrays.asList("Math"), solo, group);
        m.addMember(t);
        return t;
    }

    private static Student addStudent(TheoremHouseManager m, String id, int grade) throws Exception {
        Student s = new Student(id, "Student " + id, "09171111111", grade, TODAY);
        m.addMember(s);
        return s;
    }

    public static void main(String[] args) throws Exception {
        testLoyaltyAndPricing();
        testValidation();
        testRegistrationAndIds();
        testSlotRules();
        testSoloBooking();
        testGroupLifecycle();
        testBookingConflicts();
        testCancellationsAndCascades();
        testDemoDate();
        testCurriculumData();

        System.out.println("\nTests passed: " + passed + " | failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // -------------------------------------------------------------------------

    private static void testLoyaltyAndPricing() throws Exception {
        System.out.println("Loyalty discount and pricing");
        // {months back, expected discount, solo price, group price}
        Object[][] matrix = {
            {0, 0, 15.00, 10.00},
            {2, 0, 15.00, 10.00},
            {3, 5, 14.25, 9.50},
            {5, 5, 14.25, 9.50},
            {6, 10, 13.50, 9.00},
            {11, 10, 13.50, 9.00},
            {12, 15, 12.75, 8.50},
            {36, 15, 12.75, 8.50},
        };
        for (Object[] row : matrix) {
            int months = (Integer) row[0];
            Student s = new Student("S1", "Ann", "09171111111", 5, TODAY.minusMonths(months));
            check(months + " months -> " + row[1] + "% discount", s.getLoyaltyDiscountPercent(TODAY) == (Integer) row[1]);
            checkMoney(months + " months solo", s.computePrice(SessionType.SOLO, TODAY), (Double) row[2]);
            checkMoney(months + " months group", s.computePrice(SessionType.GROUP, TODAY), (Double) row[3]);
        }
        // Boundaries around the 3-month mark
        Student exact = new Student("S1", "Ann", "09171111111", 5, TODAY.minusMonths(3));
        Student dayShort = new Student("S2", "Ann", "09171111111", 5, TODAY.minusMonths(3).plusDays(1));
        Student dayOver = new Student("S3", "Ann", "09171111111", 5, TODAY.minusMonths(3).minusDays(1));
        check("exactly 3 months earns 5%", exact.getLoyaltyDiscountPercent(TODAY) == 5);
        check("one day short of 3 months earns 0%", dayShort.getLoyaltyDiscountPercent(TODAY) == 0);
        check("one day past 3 months earns 5%", dayOver.getLoyaltyDiscountPercent(TODAY) == 5);
        Student future = new Student("S4", "Ann", "09171111111", 5, TODAY.plusDays(5));
        check("registration after today counts as 0 months", future.getAccountAgeMonths(TODAY) == 0);
        Student fresh = new Student("S5", "Ann", "09171111111", 5, TODAY);
        check("fresh account next tier is 3 months / 5%", Arrays.equals(fresh.getNextTier(TODAY), new int[]{3, 5}));
        Student mid = new Student("S6", "Ann", "09171111111", 5, TODAY.minusMonths(7));
        check("7-month account next tier is 12 months / 15%", Arrays.equals(mid.getNextTier(TODAY), new int[]{12, 15}));
        Student top = new Student("S7", "Ann", "09171111111", 5, TODAY.minusMonths(20));
        check("top-tier account has no next tier", top.getNextTier(TODAY) == null);
        check("money formatting", Billable.formatMoney(13.5).equals("$13.50"));
    }

    private static void testValidation() throws Exception {
        System.out.println("Input validation");
        check("ID is trimmed and upper-cased", InputValidator.normalizeId("  t01 ").equals("T01"));
        expectThrows("empty ID", InvalidInputException.class, () -> InputValidator.normalizeId("  "));
        expectThrows("null ID", InvalidInputException.class, () -> InputValidator.normalizeId(null));
        expectThrows("ID with space", InvalidInputException.class, () -> InputValidator.normalizeId("T 01"));
        expectThrows("ID with symbol", InvalidInputException.class, () -> InputValidator.normalizeId("T@1"));
        expectThrows("ID too long", InvalidInputException.class, () -> InputValidator.normalizeId("A12345678901234567890"));
        expectThrows("empty name", InvalidInputException.class, () -> InputValidator.validateName("   "));
        expectThrows("name too long", InvalidInputException.class,
                () -> InputValidator.validateName(String.join("", java.util.Collections.nCopies(61, "a"))));
        InputValidator.validateContact("09171234567");
        expectThrows("contact too short", InvalidInputException.class, () -> InputValidator.validateContact("0917123"));
        expectThrows("contact with letters", InvalidInputException.class, () -> InputValidator.validateContact("0917123456a"));
        expectThrows("contact null", InvalidInputException.class, () -> InputValidator.validateContact(null));
        InputValidator.validateGrade(1);
        InputValidator.validateGrade(10);
        expectThrows("grade 0", InvalidInputException.class, () -> InputValidator.validateGrade(0));
        expectThrows("grade 11", InvalidInputException.class, () -> InputValidator.validateGrade(11));
        expectThrows("offering none", InvalidInputException.class, () -> InputValidator.validateOffering(false, false));

        check("day Mon", InputValidator.parseDay("Mon") == DayOfWeek.MONDAY);
        check("day monday", InputValidator.parseDay("monday") == DayOfWeek.MONDAY);
        check("day THU", InputValidator.parseDay("THU") == DayOfWeek.THURSDAY);
        check("day 7", InputValidator.parseDay("7") == DayOfWeek.SUNDAY);
        check("day sat", InputValidator.parseDay(" sat ") == DayOfWeek.SATURDAY);
        expectThrows("day t (ambiguous)", InvalidInputException.class, () -> InputValidator.parseDay("t"));
        expectThrows("day funday", InvalidInputException.class, () -> InputValidator.parseDay("funday"));
        expectThrows("day 8", InvalidInputException.class, () -> InputValidator.parseDay("8"));
        expectThrows("day empty", InvalidInputException.class, () -> InputValidator.parseDay(""));

        check("time 9:05", InputValidator.parseTime("9:05").equals(LocalTime.of(9, 5)));
        check("time 15:30", InputValidator.parseTime("15:30").equals(LocalTime.of(15, 30)));
        check("time 00:00", InputValidator.parseTime("00:00").equals(LocalTime.MIDNIGHT));
        expectThrows("time 24:00", InvalidInputException.class, () -> InputValidator.parseTime("24:00"));
        expectThrows("time 10:60", InvalidInputException.class, () -> InputValidator.parseTime("10:60"));
        expectThrows("time 3pm", InvalidInputException.class, () -> InputValidator.parseTime("3pm"));
        expectThrows("time null", InvalidInputException.class, () -> InputValidator.parseTime(null));

        check("date ok", InputValidator.parseDate("2026-05-11").equals(LocalDate.of(2026, 5, 11)));
        expectThrows("date bad", InvalidInputException.class, () -> InputValidator.parseDate("11/05/2026"));
        expectThrows("date impossible", InvalidInputException.class, () -> InputValidator.parseDate("2026-02-30"));
    }

    private static void testRegistrationAndIds() throws Exception {
        System.out.println("Registration and IDs");
        TheoremHouseManager m = newManager();
        addTutor(m, "T01", true, true);
        addStudent(m, "S01", 7);
        check("two members registered", m.getAllMembers().size() == 2);
        check("one tutor, one student", m.getTutors().size() == 1 && m.getStudents().size() == 1);
        expectThrows("duplicate tutor ID", DuplicateMemberException.class, () -> addTutor(m, "T01", true, true));
        expectThrows("duplicate ID differing only by case", DuplicateMemberException.class,
                () -> m.addMember(new Student("s01", "Other", "09172222222", 3, TODAY)));
        expectThrows("student cannot reuse a tutor's ID", DuplicateMemberException.class,
                () -> m.addMember(new Student("t01", "Other", "09172222222", 3, TODAY)));
        check("isIdTaken ignores case", m.isIdTaken("t01") && !m.isIdTaken("T99"));
        expectThrows("find tutor with a student ID", MemberNotFoundException.class, () -> m.findTutor("S01"));
        expectThrows("find student with a tutor ID", MemberNotFoundException.class, () -> m.findStudent("T01"));
        expectThrows("find unknown member", MemberNotFoundException.class, () -> m.findMemberById("NOPE"));
        check("lookup is case-insensitive", m.findStudent("s01").getId().equals("S01"));
        expectThrows("tutor with no subjects", InvalidInputException.class,
                () -> new Tutor("T02", "X", "09170000000", Arrays.<String>asList(), true, true));
        expectThrows("tutor offering nothing", InvalidInputException.class,
                () -> new Tutor("T02", "X", "09170000000", Arrays.asList("Math"), false, false));
        expectThrows("student with bad grade", InvalidInputException.class,
                () -> new Student("S02", "X", "09170000000", 11, TODAY));

        m.updateMemberName("S01", "  New Name ");
        check("name updated and trimmed", m.findStudent("S01").getName().equals("New Name"));
        expectThrows("blank name update", InvalidInputException.class, () -> m.updateMemberName("S01", " "));
        check("name unchanged after failed update", m.findStudent("S01").getName().equals("New Name"));
        m.updateMemberContact("S01", "09998887777");
        check("contact updated", m.findStudent("S01").getContactNumber().equals("09998887777"));
        expectThrows("bad contact update", InvalidInputException.class, () -> m.updateMemberContact("S01", "123"));
        m.updateStudentGrade("S01", 9);
        check("grade updated", m.findStudent("S01").getGradeLevel() == 9);
        expectThrows("bad grade update", InvalidInputException.class, () -> m.updateStudentGrade("S01", 0));
        check("tutor role mentions Math", m.findTutor("T01").getRole().contains("Math"));
    }

    private static void testSlotRules() throws Exception {
        System.out.println("Slot rules");
        TheoremHouseManager m = newManager();
        addTutor(m, "T01", true, true);
        addTutor(m, "T02", false, true); // group only
        Slot a = m.addSlot("T01", DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(16, 0), SessionType.SOLO);
        check("first slot ID is SL001", a.getId().equals("SL001"));
        check("new solo slot is Open", a.getStatusLabel().equals("Open"));
        Slot g = m.addSlot("T01", DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), SessionType.GROUP);
        check("new group slot is Pending (0/5)", g.getStatusLabel().equals("Pending (0/5)"));
        check("group slot waits in the queue", m.getQueueCount() == 1 && m.getPendingGroupClasses().get(0).getId().equals(g.getId()));
        check("solo slot is not queued", m.getPendingGroupClasses().size() == 1);

        expectThrows("overlapping slot", BookingException.class,
                () -> m.addSlot("T01", DayOfWeek.MONDAY, LocalTime.of(15, 30), LocalTime.of(16, 30), SessionType.SOLO));
        expectThrows("fully contained slot", BookingException.class,
                () -> m.addSlot("T01", DayOfWeek.MONDAY, LocalTime.of(15, 10), LocalTime.of(15, 50), SessionType.SOLO));
        m.addSlot("T01", DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(17, 0), SessionType.SOLO); // back-to-back is fine
        m.addSlot("T01", DayOfWeek.WEDNESDAY, LocalTime.of(15, 0), LocalTime.of(16, 0), SessionType.SOLO); // other day is fine
        check("back-to-back and other-day slots accepted", m.getSlotsForTutor("T01").size() == 4);
        expectThrows("end before start", InvalidInputException.class,
                () -> m.addSlot("T01", DayOfWeek.FRIDAY, LocalTime.of(16, 0), LocalTime.of(15, 0), SessionType.SOLO));
        expectThrows("zero-length slot", InvalidInputException.class,
                () -> m.addSlot("T01", DayOfWeek.FRIDAY, LocalTime.of(16, 0), LocalTime.of(16, 0), SessionType.SOLO));
        expectThrows("solo slot from group-only tutor", InvalidInputException.class,
                () -> m.addSlot("T02", DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), SessionType.SOLO));
        expectThrows("slot for unknown tutor", MemberNotFoundException.class,
                () -> m.addSlot("T99", DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), SessionType.SOLO));
        check("slots sorted by day then time", m.getSlotsForTutor("T01").get(0).getDay() == DayOfWeek.MONDAY
                && m.getSlotsForTutor("T01").get(1).getStart().equals(LocalTime.of(16, 0)));

        expectThrows("cannot switch off group while group slots exist", BookingException.class,
                () -> m.updateTutorOffering("T01", true, false));
        m.updateTutorOffering("T01", true, true);
        expectThrows("offering none", InvalidInputException.class, () -> m.updateTutorOffering("T01", false, false));
        m.removeSlot("T01", g.getId());
        m.updateTutorOffering("T01", true, false);
        check("group switched off once its slots are gone", !m.findTutor("T01").offersGroup());
        check("removed group slot leaves the queue", m.getQueueCount() == 0);
        expectThrows("remove someone else's slot", BookingException.class, () -> m.removeSlot("T02", a.getId()));
        expectThrows("remove unknown slot", BookingException.class, () -> m.removeSlot("T01", "SL999"));
    }

    private static void testSoloBooking() throws Exception {
        System.out.println("Solo booking");
        TheoremHouseManager m = newManager();
        addTutor(m, "T01", true, true);
        Student loyal = new Student("S01", "Loyal", "09171111111", 8, TODAY.minusMonths(6));
        m.addMember(loyal);
        addStudent(m, "S02", 8);
        Slot slot = m.addSlot("T01", DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(16, 0), SessionType.SOLO);

        check("slot listed as open before booking", m.getOpenSlots(SessionType.SOLO, 8).size() == 1);
        Booking b = m.bookSlot("S01", slot.getId(), 8, "Systems of linear equations");
        check("booking ID is BK001", b.getId().equals("BK001"));
        check("solo booking is CONFIRMED immediately", b.getStatus() == Booking.Status.CONFIRMED);
        checkMoney("6-month student pays $13.50 solo", b.getPrice(), 13.50);
        check("10% discount recorded on the booking", b.getDiscountPercent() == 10);
        check("slot shows Booked", slot.getStatusLabel().equals("Booked"));
        check("booked solo slot no longer open", m.getOpenSlots(SessionType.SOLO, 8).isEmpty());
        expectThrows("second student on a booked solo slot", BookingException.class,
                () -> m.bookSlot("S02", slot.getId(), 8, "Linear inequalities"));
        expectThrows("same student rebooking own slot", BookingException.class,
                () -> m.bookSlot("S01", slot.getId(), 8, "Linear inequalities"));
        expectThrows("booking unknown slot", BookingException.class, () -> m.bookSlot("S02", "SL999", 8, "x"));
        expectThrows("booking with blank topic", InvalidInputException.class,
                () -> m.bookSlot("S02", slot.getId(), 8, "  "));
        expectThrows("booking with bad grade", InvalidInputException.class,
                () -> m.bookSlot("S02", slot.getId(), 12, "Topic"));
        expectThrows("tutor ID used as student", MemberNotFoundException.class,
                () -> m.bookSlot("T01", slot.getId(), 8, "Topic"));

        // The price is locked when booked: moving the registration date later must not change it.
        m.setRegistrationDate("S01", TODAY);
        checkMoney("price stays locked after the discount changes", b.getPrice(), 13.50);

        Booking cancelled = m.cancelBooking("S01", b.getId());
        check("cancelled booking has CANCELLED status", cancelled.getStatus() == Booking.Status.CANCELLED);
        check("slot reopens after cancel", slot.getStatusLabel().equals("Open") && m.getOpenSlots(SessionType.SOLO, 8).size() == 1);
        expectThrows("cancel twice", BookingException.class, () -> m.cancelBooking("S01", b.getId()));
        expectThrows("cancel someone else's booking", BookingException.class, () -> m.cancelBooking("S02", b.getId()));
        Booking again = m.bookSlot("S02", slot.getId(), 8, "Linear inequalities");
        check("another student can take the reopened slot", again.getStatus() == Booking.Status.CONFIRMED);
        check("history keeps the cancelled booking", m.getBookingsForStudent("S01").size() == 1
                && m.getActiveBookingsForStudent("S01").isEmpty());
    }

    private static void testGroupLifecycle() throws Exception {
        System.out.println("Group class lifecycle");
        TheoremHouseManager m = newManager();
        addTutor(m, "T01", false, true);
        Slot slot = m.addSlot("T01", DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), SessionType.GROUP);
        for (int i = 1; i <= 9; i++) {
            addStudent(m, "S0" + i, 6);
        }
        List<Student> students = m.getStudents();
        check("empty class accepts any grade", m.getOpenSlots(SessionType.GROUP, 3).size() == 1);

        Booking first = m.bookSlot("S01", slot.getId(), 6, "Equations and inequalities");
        check("first joiner locks the class to Grade 6", slot.getGrade() == 6);
        check("class not open to another grade", m.getOpenSlots(SessionType.GROUP, 7).isEmpty());
        check("class open to the same grade", m.getOpenSlots(SessionType.GROUP, 6).size() == 1);
        expectThrows("different grade cannot join", BookingException.class,
                () -> m.bookSlot("S02", slot.getId(), 7, "Percent applications"));
        check("first booking is PENDING", first.getStatus() == Booking.Status.PENDING);
        checkMoney("group price for a new account is $10.00", first.getPrice(), 10.00);

        for (int i = 2; i <= 4; i++) {
            m.bookSlot("S0" + i, slot.getId(), 6, "Percent and consumer mathematics");
        }
        check("4 students: Pending (4/5)", slot.getStatusLabel().equals("Pending (4/5)"));
        check("4 students: everyone still PENDING", slot.getBookings().stream().allMatch(b -> b.getStatus() == Booking.Status.PENDING));
        check("pending class still queued", m.getQueueCount() == 1);

        m.bookSlot("S05", slot.getId(), 6, "Algebraic expressions");
        check("5th student: Scheduled (5/8)", slot.getStatusLabel().equals("Scheduled (5/8)"));
        check("5th student: everyone CONFIRMED", slot.getBookings().stream().allMatch(b -> b.getStatus() == Booking.Status.CONFIRMED));
        check("scheduled class leaves the pending queue", m.getQueueCount() == 0);

        m.bookSlot("S06", slot.getId(), 6, "Algebraic expressions");
        m.bookSlot("S07", slot.getId(), 6, "Algebraic expressions");
        check("7 students still Scheduled and open", slot.getStatusLabel().equals("Scheduled (7/8)")
                && m.getOpenSlots(SessionType.GROUP, 6).size() == 1);
        m.bookSlot("S08", slot.getId(), 6, "Algebraic expressions");
        check("8 students: Full (8/8)", slot.getStatusLabel().equals("Full (8/8)"));
        check("full class no longer listed", m.getOpenSlots(SessionType.GROUP, 6).isEmpty());
        expectThrows("9th student rejected when full", BookingException.class,
                () -> m.bookSlot("S09", slot.getId(), 6, "Algebraic expressions"));

        // Falling back below the minimum returns the class to Pending
        String s08Booking = m.getBookingsForStudent("S08").get(0).getId();
        m.cancelBooking("S08", s08Booking);
        check("back to 7: Scheduled", slot.getStatusLabel().equals("Scheduled (7/8)"));
        for (int i = 7; i >= 5; i--) {
            m.cancelBooking("S0" + i, m.getBookingsForStudent("S0" + i).get(0).getId());
        }
        check("down to 4: Pending (4/5)", slot.getStatusLabel().equals("Pending (4/5)"));
        check("down to 4: bookings revert to PENDING", slot.getBookings().stream().allMatch(b -> b.getStatus() == Booking.Status.PENDING));
        check("class re-enters the pending queue", m.getQueueCount() == 1);
        m.bookSlot("S09", slot.getId(), 6, "Geometry and measurement");
        check("a new joiner re-schedules it", slot.getStatusLabel().equals("Scheduled (5/8)"));

        // When everyone leaves, the grade lock resets
        for (Booking b : slot.getBookings()) {
            m.cancelBooking(b.getStudentId(), b.getId());
        }
        check("empty again: Pending (0/5), grade unlocked", slot.getStatusLabel().equals("Pending (0/5)") && slot.getGrade() == 0);
        check("unlocked class open to another grade", m.getOpenSlots(SessionType.GROUP, 9).size() == 1);
        check("students list intact", students.size() == 9);
    }

    private static void testBookingConflicts() throws Exception {
        System.out.println("Booking conflicts");
        TheoremHouseManager m = newManager();
        addTutor(m, "T01", true, true);
        addTutor(m, "T02", true, true);
        addStudent(m, "S01", 5);
        Slot a = m.addSlot("T01", DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(16, 0), SessionType.SOLO);
        Slot clash = m.addSlot("T02", DayOfWeek.MONDAY, LocalTime.of(15, 30), LocalTime.of(16, 30), SessionType.SOLO);
        Slot touching = m.addSlot("T02", DayOfWeek.MONDAY, LocalTime.of(16, 30), LocalTime.of(17, 30), SessionType.SOLO);
        Slot group = m.addSlot("T02", DayOfWeek.MONDAY, LocalTime.of(17, 30), LocalTime.of(18, 30), SessionType.GROUP);
        m.bookSlot("S01", a.getId(), 5, "Fraction operations");
        expectThrows("overlapping booking with a different tutor", BookingException.class,
                () -> m.bookSlot("S01", clash.getId(), 5, "Decimal operations"));
        m.bookSlot("S01", touching.getId(), 5, "Decimal operations");
        m.bookSlot("S01", group.getId(), 5, "Percentages and financial mathematics");
        check("non-overlapping bookings accepted", m.getActiveBookingsForStudent("S01").size() == 3);
        m.cancelBooking("S01", m.getBookingsForStudent("S01").get(0).getId());
        m.bookSlot("S01", clash.getId(), 5, "Decimal operations");
        check("clash resolved after cancelling the other booking", m.getActiveBookingsForStudent("S01").size() == 3);
    }

    private static void testCancellationsAndCascades() throws Exception {
        System.out.println("Cancellations and cascading deletes");
        TheoremHouseManager m = newManager();
        addTutor(m, "T01", true, true);
        addTutor(m, "T02", true, true);
        addStudent(m, "S01", 5);
        addStudent(m, "S02", 5);
        Slot solo = m.addSlot("T01", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), SessionType.SOLO);
        Slot group = m.addSlot("T01", DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), SessionType.GROUP);
        Slot other = m.addSlot("T02", DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), SessionType.SOLO);
        m.bookSlot("S01", solo.getId(), 5, "Fraction operations");
        m.bookSlot("S01", group.getId(), 5, "Fraction operations");
        m.bookSlot("S02", group.getId(), 5, "Fraction operations");
        m.bookSlot("S02", other.getId(), 5, "Fraction operations");

        int cancelled = m.removeSlot("T01", group.getId());
        check("removing a group slot cancels its 2 bookings", cancelled == 2);
        check("students' bookings in it are CANCELLED", m.getActiveBookingsForStudent("S01").size() == 1
                && m.getActiveBookingsForStudent("S02").size() == 1);
        expectThrows("removed slot can no longer be found", BookingException.class, () -> m.findSlot(group.getId()));

        int viaDelete = m.deleteMember("T01");
        check("deleting a tutor cancels their remaining booking", viaDelete == 1);
        check("tutor and slots are gone", m.getTutors().size() == 1 && m.getSlotsForTutor("T01").isEmpty());
        expectThrows("deleted tutor cannot be found", MemberNotFoundException.class, () -> m.findTutor("T01"));
        check("S01 has no active bookings left", m.getActiveBookingsForStudent("S01").isEmpty());

        int viaStudent = m.deleteMember("S02");
        check("deleting a student cancels their booking", viaStudent == 1);
        check("the freed solo slot reopens", other.getStatusLabel().equals("Open"));
        check("student removed from lists", m.getStudents().size() == 1 && !m.isIdTaken("S02"));
        expectThrows("delete unknown member", MemberNotFoundException.class, () -> m.deleteMember("S02"));
        addStudent(m, "S02", 4); // the ID can be reused after deletion
        check("deleted ID can be registered again", m.isIdTaken("S02"));
    }

    private static void testDemoDate() throws Exception {
        System.out.println("Demo registration date");
        TheoremHouseManager m = newManager();
        addStudent(m, "S01", 5);
        check("new account has 0% discount", m.findStudent("S01").getLoyaltyDiscountPercent(TODAY) == 0);
        m.setRegistrationDate("S01", TODAY.minusMonths(13));
        check("13 months back gives 15%", m.findStudent("S01").getLoyaltyDiscountPercent(TODAY) == 15);
        m.setRegistrationDate("S01", TODAY);
        check("today is allowed", m.findStudent("S01").getAccountAgeMonths(TODAY) == 0);
        expectThrows("future date rejected", InvalidInputException.class, () -> m.setRegistrationDate("S01", TODAY.plusDays(1)));
        expectThrows("null date rejected", InvalidInputException.class, () -> m.setRegistrationDate("S01", null));
        expectThrows("unknown student", MemberNotFoundException.class, () -> m.setRegistrationDate("S99", TODAY));
    }

    private static void testCurriculumData() {
        System.out.println("Curriculum data");
        check("grades 1 to 10", Curriculum.MIN_GRADE == 1 && Curriculum.MAX_GRADE == 10);
        int[] expectedUnits = {8, 8, 8, 8, 8, 8, 8, 8, 8, 10};
        for (int g = 1; g <= 10; g++) {
            check("grade " + g + " has " + expectedUnits[g - 1] + " units", Curriculum.getUnits(g).size() == expectedUnits[g - 1]);
            check("grade " + g + " has a title and focus", !Curriculum.getTitle(g).isEmpty() && !Curriculum.getFocus(g).isEmpty());
        }
        check("grade 1 title", Curriculum.getTitle(1).equals("Mathematical Foundations"));
        check("grade 8 first unit", Curriculum.getUnits(8).get(0).equals("Real numbers, roots, and exponents"));
        check("grade 10 last unit", Curriculum.getUnits(10).get(9).equals("Statistics, probability, and capstone"));
        check("grade 9 focus mentions quadratic", Curriculum.getFocus(9).contains("quadratic"));
        check("Math is the only subject", Curriculum.SUBJECTS.equals(Arrays.asList("Math")));
        boolean threw = false;
        try {
            Curriculum.getTitle(11);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("grade 11 rejected by curriculum", threw);
        threw = false;
        try {
            Curriculum.getUnits(0).add("x");
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            threw = true;
        }
        check("unit lists are read-only / grade 0 rejected", threw);
    }
}
