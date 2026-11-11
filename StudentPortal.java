import java.time.LocalDate;
import java.util.List;

/** Everything a student can do: register or log in, book solo or group sessions, manage bookings. */
public class StudentPortal {
    private final TheoremHouseManager manager;
    private final ConsoleInput in;

    public StudentPortal(TheoremHouseManager manager, ConsoleInput in) {
        this.manager = manager;
        this.in = in;
    }

    public void run() throws InvalidInputException, DuplicateMemberException, MemberNotFoundException {
        System.out.println("\n--- Student Portal ---");
        System.out.println("1. Register as a new student | 2. Log in with my ID | 3. Back");
        String option = in.readLine("Choose (1-3): ");

        Student student;
        if ("1".equals(option)) {
            student = register();
        } else if ("2".equals(option)) {
            String id = InputValidator.normalizeId(in.readLine("Enter your Student ID: "));
            student = manager.findStudent(id);
        } else if ("3".equals(option)) {
            return;
        } else {
            System.out.println("[!] Invalid option.");
            return;
        }
        System.out.println("\nWelcome, " + student.getName() + "!");
        menuLoop(student.getId());
    }

    // ------------------------------------------------------------ registration

    private Student register() throws InvalidInputException, DuplicateMemberException {
        System.out.println("\n--- Register Student ---");
        String id = InputValidator.normalizeId(in.readLine("Enter Student ID (e.g., S01): "));
        if (manager.isIdTaken(id)) {
            throw new DuplicateMemberException("ID '" + id + "' already exists.");
        }
        String name = in.readLine("Enter Full Name: ");
        InputValidator.validateName(name);
        String contact = in.readLine("Enter Contact Number (11 digits): ");
        InputValidator.validateContact(contact);
        int grade = in.readInt("Enter Grade Level (1-10): ", "Grade level");
        InputValidator.validateGrade(grade);

        Student student = new Student(id, name, contact, grade, manager.today());
        manager.addMember(student);
        System.out.println("[OK] Student registered successfully! Your ID is " + student.getId() + ".");
        System.out.println("     Stay with us: your loyalty discount grows the longer your account is active.");
        return student;
    }

    // ---------------------------------------------------------------- menu loop

    private void menuLoop(String studentId) {
        boolean running = true;
        while (running) {
            printMenu();
            try {
                String option = in.readLine("Select an option (1-10): ");
                switch (option) {
                    case "1":
                        viewProfile(studentId);
                        break;
                    case "2":
                        updateProfile(studentId);
                        break;
                    case "3":
                        bookSession(studentId);
                        break;
                    case "4":
                        viewBookings(studentId);
                        break;
                    case "5":
                        cancelBooking(studentId);
                        break;
                    case "6":
                        Curriculum.browse(in);
                        break;
                    case "7":
                        Offerings.print();
                        break;
                    case "8":
                        setRegistrationDate(studentId);
                        break;
                    case "9":
                        if (deleteAccount(studentId)) {
                            running = false;
                        }
                        break;
                    case "10":
                        System.out.println("\nLogged out. Keep learning with Theorem House!");
                        running = false;
                        break;
                    default:
                        System.out.println("\n[!] Invalid choice. Pick 1 to 10.");
                }
            } catch (InvalidInputException | MemberNotFoundException | BookingException e) {
                System.out.println("\n[!] Error: " + e.getMessage());
            } catch (InputEndedException e) {
                throw e;
            } catch (Exception e) {
                System.out.println("\n[!] Unexpected Error: " + e.getMessage());
            } finally {
                System.out.println("--------------------------------------------------");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- STUDENT MENU ---");
        System.out.println("1. View my profile and loyalty discount");
        System.out.println("2. Update my profile");
        System.out.println("3. Book a session");
        System.out.println("4. View my bookings");
        System.out.println("5. Cancel a booking");
        System.out.println("6. View curriculum");
        System.out.println("7. What we offer");
        System.out.println("8. Demo: set my registration date (to test loyalty tiers)");
        System.out.println("9. Delete my account");
        System.out.println("10. Log out");
    }

    // ----------------------------------------------------------------- handlers

    private void viewProfile(String studentId) throws MemberNotFoundException {
        System.out.println("\n--- My Profile ---");
        Student student = manager.findStudent(studentId);
        student.displayInfo(true);
        int[] next = student.getNextTier(manager.today());
        if (next == null) {
            System.out.println("  You have reached our top loyalty tier. Thank you for staying with us!");
        } else {
            System.out.println("  Next tier: " + next[1] + "% off after " + next[0] + " months with us.");
        }
    }

    private void updateProfile(String studentId) throws MemberNotFoundException, InvalidInputException {
        System.out.println("\n--- Update Profile ---");
        System.out.println("1. Update Name | 2. Update Contact | 3. Update Grade Level");
        String option = in.readLine("Choose field (1-3): ");
        if ("1".equals(option)) {
            String name = in.readLine("Enter New Name: ");
            InputValidator.validateName(name);
            manager.updateMemberName(studentId, name);
            System.out.println("[OK] Name updated.");
        } else if ("2".equals(option)) {
            String contact = in.readLine("Enter New 11-digit Contact: ");
            InputValidator.validateContact(contact);
            manager.updateMemberContact(studentId, contact);
            System.out.println("[OK] Contact updated.");
        } else if ("3".equals(option)) {
            int grade = in.readInt("Enter New Grade Level (1-10): ", "Grade level");
            InputValidator.validateGrade(grade);
            manager.updateStudentGrade(studentId, grade);
            System.out.println("[OK] Grade level updated.");
        } else {
            System.out.println("[!] Invalid option.");
        }
    }

    private void bookSession(String studentId) throws MemberNotFoundException, InvalidInputException,
            BookingException {
        System.out.println("\n--- Book a Session ---");
        Student student = manager.findStudent(studentId);
        LocalDate today = manager.today();

        // 1. Session type
        System.out.println("Session types: 1. Solo (" + Billable.formatMoney(SessionType.SOLO.getBasePrice())
                + ") | 2. Group (" + Billable.formatMoney(SessionType.GROUP.getBasePrice()) + ")");
        int typeChoice = in.readInt("Choose (1-2): ", "Session type choice");
        InputValidator.validateChoice(typeChoice, 1, 2, "session type");
        SessionType type = typeChoice == 1 ? SessionType.SOLO : SessionType.GROUP;

        int discount = student.getLoyaltyDiscountPercent(today);
        System.out.println("Your price: " + Billable.formatMoney(student.computePrice(type, today))
                + " (" + discount + "% loyalty discount)");

        // 2. Topic: grade, then unit
        String gradeText = in.readLine("Grade to study (1-10, press Enter for Grade " + student.getGradeLevel() + "): ");
        int grade = student.getGradeLevel();
        if (!gradeText.isEmpty()) {
            try {
                grade = Integer.parseInt(gradeText);
            } catch (NumberFormatException e) {
                throw new InvalidInputException("Grade must be a whole number.");
            }
        }
        InputValidator.validateGrade(grade);
        Curriculum.printGradeUnits(grade);
        List<String> units = Curriculum.getUnits(grade);
        int unitChoice = in.readInt("Choose a topic (1-" + units.size() + "): ", "Topic choice");
        InputValidator.validateChoice(unitChoice, 1, units.size(), "topic");
        String unit = units.get(unitChoice - 1);

        // 3. Open slots
        List<Slot> open = manager.getOpenSlots(type, grade);
        if (open.isEmpty()) {
            System.out.println("\nNo open " + type.getLabel().toLowerCase() + " slots for Grade " + grade
                    + " right now. Try another session type or check back later.");
            return;
        }
        System.out.println("\nOpen " + type.getLabel().toLowerCase() + " slots:");
        for (Slot s : open) {
            System.out.println("  " + s.getId() + " | " + s.getTimeLabel() + " | Tutor: "
                    + manager.findMemberById(s.getTutorId()).getName() + " | " + s.getStatusLabel());
        }
        String slotText = in.readLine("Enter Slot ID to book (or press Enter to cancel): ");
        if (slotText.isEmpty()) {
            System.out.println("Booking cancelled.");
            return;
        }
        Slot slot = manager.findSlot(InputValidator.normalizeId(slotText));

        // 4. Confirm and book
        double price = student.computePrice(type, today);
        if (!in.readYesNo("Confirm booking " + slot.getId() + " (" + slot.getTimeLabel() + ") for "
                + Billable.formatMoney(price) + "? (Y/N): ")) {
            System.out.println("Booking cancelled.");
            return;
        }
        Booking booking = manager.bookSlot(studentId, slot.getId(), grade, unit);
        System.out.println("[OK] Booking " + booking.getId() + " recorded: Grade " + grade + " - " + unit);
        if (type == SessionType.SOLO) {
            System.out.println("     Your solo session is confirmed. Total: " + Billable.formatMoney(booking.getPrice()));
        } else if (booking.getStatus() == Booking.Status.CONFIRMED) {
            System.out.println("     The class is Scheduled (" + slot.getStatusLabel() + "). Total: "
                    + Billable.formatMoney(booking.getPrice()));
        } else {
            int needed = Slot.GROUP_MIN - slot.activeCount();
            System.out.println("     You joined the class (" + slot.getStatusLabel() + "). It needs " + needed
                    + " more student(s) to be Scheduled. You will be charged "
                    + Billable.formatMoney(booking.getPrice()) + " once it is Scheduled.");
        }
    }

    private void viewBookings(String studentId) throws MemberNotFoundException, BookingException {
        System.out.println("\n--- My Bookings ---");
        List<Booking> bookings = manager.getBookingsForStudent(studentId);
        if (bookings.isEmpty()) {
            System.out.println("You have no bookings yet.");
            return;
        }
        for (Booking b : bookings) {
            String when = "-";
            String tutorName = "-";
            String classStatus = "";
            if (b.isActive()) {
                Slot slot = manager.findSlot(b.getSlotId());
                when = slot.getTimeLabel();
                tutorName = manager.findMemberById(slot.getTutorId()).getName();
                classStatus = slot.isGroup() ? " | Class: " + slot.getStatusLabel() : "";
            }
            System.out.println("  " + b.getId() + " | " + b.getSlotId() + " | " + when + " | Tutor: " + tutorName
                    + " | " + b.getType().getLabel() + " | Grade " + b.getGrade() + ": " + b.getUnit()
                    + " | " + Billable.formatMoney(b.getPrice()) + " (" + b.getDiscountPercent() + "% off)"
                    + " | " + b.getStatus() + classStatus);
        }
        System.out.println("\nNote: group bookings are charged only once the class is Scheduled.");
    }

    private void cancelBooking(String studentId) throws MemberNotFoundException, BookingException,
            InvalidInputException {
        System.out.println("\n--- Cancel Booking ---");
        List<Booking> active = manager.getActiveBookingsForStudent(studentId);
        if (active.isEmpty()) {
            System.out.println("You have no active bookings.");
            return;
        }
        for (Booking b : active) {
            Slot slot = manager.findSlot(b.getSlotId());
            System.out.println("  " + b.getId() + " | " + slot.getTimeLabel() + " | " + b.getType().getLabel()
                    + " | Grade " + b.getGrade() + ": " + b.getUnit() + " | " + b.getStatus());
        }
        String id = InputValidator.normalizeId(in.readLine("Enter Booking ID to cancel: "));
        if (in.readYesNo("Cancel booking " + id + "? (Y/N): ")) {
            Booking cancelled = manager.cancelBooking(studentId, id);
            System.out.println("[OK] Booking " + cancelled.getId() + " cancelled.");
            Slot slot = null;
            try {
                slot = manager.findSlot(cancelled.getSlotId());
            } catch (BookingException ignored) {
                // slot already gone; nothing more to report
            }
            if (slot != null && slot.isGroup() && slot.activeCount() < Slot.GROUP_MIN) {
                System.out.println("     That group class is back to " + slot.getStatusLabel() + ".");
            }
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    private void setRegistrationDate(String studentId) throws MemberNotFoundException, InvalidInputException {
        System.out.println("\n--- Demo: Set Registration Date ---");
        System.out.println("For testing only. Moves your account start date so you can see the loyalty tiers.");
        LocalDate date = InputValidator.parseDate(in.readLine("New registration date (yyyy-MM-dd): "));
        manager.setRegistrationDate(studentId, date);
        Student student = manager.findStudent(studentId);
        LocalDate today = manager.today();
        System.out.println("[OK] Registration date set to " + date + ". Account age: "
                + student.getAccountAgeMonths(today) + " month(s) | Loyalty discount: "
                + student.getLoyaltyDiscountPercent(today) + "%");
    }

    private boolean deleteAccount(String studentId) throws MemberNotFoundException, BookingException {
        System.out.println("\n--- Delete Account ---");
        Student student = manager.findStudent(studentId);
        int active = manager.getActiveBookingsForStudent(studentId).size();
        String warning = active > 0 ? " This will cancel " + active + " booking(s)." : "";
        if (in.readYesNo("Permanently delete " + student.getName() + "'s account?" + warning + " (Y/N): ")) {
            manager.deleteMember(studentId);
            System.out.println("[OK] Account deleted.");
            return true;
        }
        System.out.println("Operation cancelled.");
        return false;
    }
}
