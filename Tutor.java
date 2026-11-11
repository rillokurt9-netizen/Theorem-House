import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** A tutor: teaches one or more subjects and offers solo sessions, group classes, or both. */
public class Tutor extends Person {
    private final Set<String> subjects;
    private boolean offersSolo;
    private boolean offersGroup;

    public Tutor(String id, String name, String contactNumber, Collection<String> subjects,
                 boolean offersSolo, boolean offersGroup) throws InvalidInputException {
        super(id, name, contactNumber);
        if (subjects == null || subjects.isEmpty()) {
            throw new InvalidInputException("A tutor must teach at least one subject.");
        }
        InputValidator.validateOffering(offersSolo, offersGroup);
        this.subjects = new LinkedHashSet<>(subjects);
        this.offersSolo = offersSolo;
        this.offersGroup = offersGroup;
    }

    public Set<String> getSubjects() {
        return Collections.unmodifiableSet(subjects);
    }

    public boolean offersSolo() { return offersSolo; }
    public boolean offersGroup() { return offersGroup; }

    public boolean offers(SessionType type) {
        return type == SessionType.SOLO ? offersSolo : offersGroup;
    }

    public void setOffering(boolean offersSolo, boolean offersGroup) throws InvalidInputException {
        InputValidator.validateOffering(offersSolo, offersGroup);
        this.offersSolo = offersSolo;
        this.offersGroup = offersGroup;
    }

    public String getOfferingLabel() {
        if (offersSolo && offersGroup) {
            return "Solo and Group";
        }
        return offersSolo ? "Solo only" : "Group only";
    }

    @Override
    public String getRole() {
        return "Tutor (" + String.join(", ", subjects) + ")";
    }

    @Override
    public void displayInfo() {
        super.displayInfo();
        System.out.println("  Offers: " + getOfferingLabel());
    }
}
