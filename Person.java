/**
 * Base class for everyone in Theorem House (kept from the gym project's Person).
 * Fields are validated on construction and on every setter, so a Person can never hold bad data.
 */
public abstract class Person {
    private final String id;
    private String name;
    private String contactNumber;

    protected Person(String id, String name, String contactNumber) throws InvalidInputException {
        this.id = InputValidator.normalizeId(id);
        InputValidator.validateName(name);
        InputValidator.validateContact(contactNumber);
        this.name = name.trim();
        this.contactNumber = contactNumber.trim();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getContactNumber() { return contactNumber; }

    public void setName(String name) throws InvalidInputException {
        InputValidator.validateName(name);
        this.name = name.trim();
    }

    public void setContactNumber(String contactNumber) throws InvalidInputException {
        InputValidator.validateContact(contactNumber);
        this.contactNumber = contactNumber.trim();
    }

    public abstract String getRole();

    public void displayInfo() {
        System.out.println("ID: " + id + " | Name: " + name + " | Role: " + getRole());
    }

    // Overloaded method (Polymorphism), kept from the original
    public void displayInfo(boolean showContact) {
        displayInfo();
        if (showContact) {
            System.out.println("  Contact: " + contactNumber);
        }
    }
}
