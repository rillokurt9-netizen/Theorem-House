import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Everything a tutor can do: register or log in, manage profile and slots, see bookings. */
public class TutorPortal {
    private final TheoremHouseManager manager;
    private final ConsoleInput in;

    public TutorPortal(TheoremHouseManager manager, ConsoleInput in) {
        this.manager = manager;
        this.in = in;
    }

    public void run() throws InvalidInputException, DuplicateMemberException, MemberNotFoundException {
        System.out.println("\n--- Tutor Portal ---");
        System.out.println("1. Register as a new tutor | 2. Log in with my ID | 3. Back");
        String option = in.readLine("Choose (1-3): ");

        Tutor tutor;
        if ("1".equals(option)) {
            tutor = register();
        } else if ("2".equals(option)) {
            String id = InputValidator.normalizeId(in.readLine("Enter your Tutor ID: "));
            tutor = manager.findTutor(id);
        } else if ("3".equals(option)) {
            return;
        } else {
            System.out.println("[!] Invalid option.");
            return;
        }
        System.out.println("\nWelcome, " + tutor.getName() + "!");
        menuLoop(tutor.getId());
    }

    // ------------------------------------------------------------ registration

    private Tutor register() throws InvalidInputException, DuplicateMemberException {
        System.out.println("\n--- Register Tutor ---");
        String id = InputValidator.normalizeId(in.readLine("Enter Tutor ID (e.g., T01): "));
        if (manager.isIdTaken(id)) {
            throw new DuplicateMemberException("ID '" + id + "' already exists.");
        }
        String name = in.readLine("Enter Full Name: ");
        InputValidator.validateName(name);
        String contact = in.readLine("Enter Contact Number (11 digits): ");
        InputValidator.validateContact(contact);

        List<String> subjects = chooseSubjects();

        System.out.println("\nSession types: 1. Solo only ($15) | 2. Group only ($10) | 3. Both");
        int choice = in.readInt("Choose (1-3): ", "Session type choice");
        InputValidator.validateChoice(choice, 1, 3, "session type");
        boolean solo = choice == 1 || choice == 3;
        boolean group = choice == 2 || choice == 3;

        Tutor tutor = new Tutor(id, name, contact, subjects, solo, group);
        manager.addMember(tutor);
        System.out.println("[OK] Tutor registered successfully! Your ID is " + tutor.getId() + ".");
        return tutor;
    }

    private List<String> chooseSubjects() throws InvalidInputException {
        List<String> available = Curriculum.SUBJECTS;
        if (available.size() == 1) {
            System.out.println("Subject: " + available.get(0) + " (currently the only subject offered)");
            return new ArrayList<>(available);
        }
        System.out.println("\nSubjects:");
        for (int i = 0; i < available.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + available.get(i));
        }
        String text = in.readLine("Enter subject numbers, separated by commas: ");
        List<String> chosen = new ArrayList<>();
        for (String part : text.split(",")) {
            int n;
            try {
                n = Integer.parseInt(part.trim());
            } catch (NumberFormatException e) {
                throw new InvalidInputException("Subjects must be entered as numbers, e.g. 1,2.");
            }
            InputValidator.validateChoice(n, 1, available.size(), "subject");
            if (!chosen.contains(available.get(n - 1))) {
                chosen.add(available.get(n - 1));
            }
        }
        return chosen;
    }

    // ---------------------------------------------------------------- menu loop

    private void menuLoop(String tutorId) {
        boolean running = true;
        while (running) {
            printMenu();
            try {
                String option = in.readLine("Select an option (1-8): ");
                switch (option) {
                    case "1":
                        viewProfile(tutorId);
                        break;
                    case "2":
                        updateProfile(tutorId);
                        break;
                    case "3":
                        addSlot(tutorId);
                        break;
                    case "4":
                        removeSlot(tutorId);
                        break;
                    case "5":
                        viewSlotsAndBookings(tutorId);
                        break;
                    case "6":
                        Curriculum.browse(in);
                        break;
                    case "7":
                        if (deleteAccount(tutorId)) {
                            running = false;
                        }
                        break;
                    case "8":
                        System.out.println("\nLogged out. Thank you for teaching with Theorem House!");
                        running = false;
                        break;
                    default:
                        System.out.println("\n[!] Invalid choice. Pick 1 to 8.");
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
        System.out.println("\n--- TUTOR MENU ---");
        System.out.println("1. View my profile");
        System.out.println("2. Update my profile");
        System.out.println("3. Add an availability slot");
        System.out.println("4. Remove / cancel a slot");
        System.out.println("5. View my slots and bookings");
        System.out.println("6. View curriculum");
        System.out.println("7. Delete my account");
        System.out.println("8. Log out");
    }

    // ----------------------------------------------------------------- handlers

    private void viewProfile(String tutorId) throws MemberNotFoundException {
        System.out.println("\n--- My Profile ---");
        manager.findTutor(tutorId).displayInfo(true);
    }

    private void updateProfile(String tutorId) throws MemberNotFoundException, InvalidInputException,
            BookingException {
        System.out.println("\n--- Update Profile ---");
        System.out.println("1. Update Name | 2. Update Contact | 3. Change Session Types");
        String option = in.readLine("Choose field (1-3): ");
        if ("1".equals(option)) {
            String name = in.readLine("Enter New Name: ");
            InputValidator.validateName(name);
            manager.updateMemberName(tutorId, name);
            System.out.println("[OK] Name updated.");
        } else if ("2".equals(option)) {
            String contact = in.readLine("Enter New 11-digit Contact: ");
            InputValidator.validateContact(contact);
            manager.updateMemberContact(tutorId, contact);
            System.out.println("[OK] Contact updated.");
        } else if ("3".equals(option)) {
            System.out.println("1. Solo only ($15) | 2. Group only ($10) | 3. Both");
            int choice = in.readInt("Choose (1-3): ", "Session type choice");
            InputValidator.validateChoice(choice, 1, 3, "session type");
            manager.updateTutorOffering(tutorId, choice == 1 || choice == 3, choice == 2 || choice == 3);
            System.out.println("[OK] Session types updated: " + manager.findTutor(tutorId).getOfferingLabel() + ".");
        } else {
            System.out.println("[!] Invalid option.");
        }
    }

    private void addSlot(String tutorId) throws InvalidInputException, MemberNotFoundException, BookingException {
        System.out.println("\n--- Add Availability Slot ---");
        Tutor tutor = manager.findTutor(tutorId);

        DayOfWeek day = InputValidator.parseDay(in.readLine("Day (Mon-Sun): "));
        LocalTime start = InputValidator.parseTime(in.readLine("Start time (24-hour HH:mm, e.g. 15:00): "));
        LocalTime end = InputValidator.parseTime(in.readLine("End time (24-hour HH:mm, e.g. 16:00): "));

        SessionType type;
        if (tutor.offersSolo() && tutor.offersGroup()) {
            int choice = in.readInt("Session type (1. Solo, 2. Group): ", "Session type choice");
            InputValidator.validateChoice(choice, 1, 2, "session type");
            type = choice == 1 ? SessionType.SOLO : SessionType.GROUP;
        } else {
            type = tutor.offersSolo() ? SessionType.SOLO : SessionType.GROUP;
            System.out.println("Session type: " + type.getLabel() + " (the only type you offer)");
        }

        Slot slot = manager.addSlot(tutorId, day, start, end, type);
        System.out.println("[OK] Slot " + slot.getId() + " added: " + slot.getTimeLabel()
                + " | " + type.getLabel() + " | " + slot.getStatusLabel());
    }

    private void removeSlot(String tutorId) throws MemberNotFoundException, BookingException,
            InvalidInputException {
        System.out.println("\n--- Remove / Cancel Slot ---");
        List<Slot> slots = manager.getSlotsForTutor(tutorId);
        if (slots.isEmpty()) {
            System.out.println("You have no slots.");
            return;
        }
        printSlots(slots);
        String slotId = InputValidator.normalizeId(in.readLine("Enter Slot ID to remove: "));
        Slot slot = manager.findSlot(slotId);
        if (!slot.getTutorId().equals(tutorId)) {
            throw new BookingException("Slot " + slot.getId() + " does not belong to you.");
        }
        String warning = slot.activeCount() > 0
                ? " This will cancel " + slot.activeCount() + " booking(s)." : "";
        if (in.readYesNo("Remove slot " + slot.getId() + " (" + slot.getTimeLabel() + ")?" + warning + " (Y/N): ")) {
            int cancelled = manager.removeSlot(tutorId, slot.getId());
            System.out.println("[OK] Slot removed." + (cancelled > 0 ? " " + cancelled + " booking(s) cancelled." : ""));
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    private void viewSlotsAndBookings(String tutorId) throws MemberNotFoundException {
        System.out.println("\n--- My Slots and Bookings ---");
        List<Slot> slots = manager.getSlotsForTutor(tutorId);
        if (slots.isEmpty()) {
            System.out.println("You have no slots yet. Add one from the menu.");
            return;
        }
        printSlots(slots);
        System.out.println();
        for (Slot slot : slots) {
            for (Booking b : slot.getBookings()) {
                Person student = manager.findMemberById(b.getStudentId());
                System.out.println("  " + slot.getId() + " <- " + student.getName() + " (" + student.getId() + ")"
                        + " | Grade " + b.getGrade() + ": " + b.getUnit() + " | " + b.getStatus());
            }
        }
    }

    private boolean deleteAccount(String tutorId) throws MemberNotFoundException, BookingException {
        System.out.println("\n--- Delete Account ---");
        Tutor tutor = manager.findTutor(tutorId);
        int affected = 0;
        for (Slot s : manager.getSlotsForTutor(tutorId)) {
            affected += s.activeCount();
        }
        String warning = affected > 0 ? " This will cancel " + affected + " booking(s)." : "";
        if (in.readYesNo("Permanently delete " + tutor.getName() + "'s account and all slots?" + warning + " (Y/N): ")) {
            manager.deleteMember(tutorId);
            System.out.println("[OK] Account deleted.");
            return true;
        }
        System.out.println("Operation cancelled.");
        return false;
    }

    private void printSlots(List<Slot> slots) {
        for (Slot s : slots) {
            System.out.println("  " + s.getId() + " | " + s.getTimeLabel() + " | " + s.getType().getLabel()
                    + " | " + s.getStatusLabel()
                    + (s.isGroup() && s.getGrade() > 0 ? " | Grade " + s.getGrade() : ""));
        }
    }
}
