public class Main {
    private static final TheoremHouseManager manager = new TheoremHouseManager();
    private static final ConsoleInput in = new ConsoleInput(System.in);

    public static void main(String[] args) {
        TutorPortal tutorPortal = new TutorPortal(manager, in);
        StudentPortal studentPortal = new StudentPortal(manager, in);
        int choice = 0;

        System.out.println("\nTHEOREM HOUSE - Online Math Tutoring");

        try {
            do {
                choice = 0;
                printMenu();
                try {
                    choice = in.readInt("Select an option (1-5): ", "Menu choice");

                    switch (choice) {
                        case 1:
                            tutorPortal.run();
                            break;
                        case 2:
                            studentPortal.run();
                            break;
                        case 3:
                            Curriculum.browse(in);
                            break;
                        case 4:
                            Offerings.print();
                            break;
                        case 5:
                            System.out.println("\nExiting Theorem House. Keep solving!");
                            break;
                        default:
                            System.out.println("\n[!] Invalid choice. Pick 1 to 5.");
                    }
                } catch (InvalidInputException | DuplicateMemberException | MemberNotFoundException e) {
                    System.out.println("\n[!] Error: " + e.getMessage());
                } catch (InputEndedException e) {
                    throw e;
                } catch (Exception e) {
                    System.out.println("\n[!] Unexpected Error: " + e.getMessage());
                } finally {
                    System.out.println("--------------------------------------------------");
                }
            } while (choice != 5);
        } catch (InputEndedException e) {
            System.out.println("\n[!] Input ended. Exiting Theorem House.");
        } finally {
            in.close();
        }
    }

    private static void printMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. I am a Tutor");
        System.out.println("2. I am a Student (looking for a tutor)");
        System.out.println("3. View our Curriculum");
        System.out.println("4. What we offer (prices and loyalty discount)");
        System.out.println("5. Exit");
    }
}
