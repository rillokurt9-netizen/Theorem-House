# Theorem House - Online Math Tutor Booking (Java console app)

Built from the Fitness-Membership-Java project. Plain Java, no libraries (Java 8+).

## Run
```
javac *.java
java Main
```

## Run the tests
```
javac *.java
java TheoremHouseTests
```

## What it does
- Choose **Tutor** or **Student** at the start; both can register and log in with an ID.
- **Pricing:** Solo $15, Group $10, per session (per student for group).
- **Loyalty discount** (by account age, both session types): under 3 months 0%, 3+ months 5%, 6+ months 10%, 12+ months 15%.
- **Group classes:** min 5, max 8 students. Pending until 5 join, then Scheduled, Full at 8. Dropping below 5 returns it to Pending. Charged only once Scheduled.
- **Tutors:** register, add/remove weekly slots (solo, group or both), view bookings, update or delete profile.
- **Students:** register (grade 1-10), book a topic from the curriculum, view/cancel bookings, see their loyalty discount.
- **Curriculum screen:** Grades 1-10 Math overview, plus unit titles per grade (from the supplied PDF). **Offerings screen:** prices, discount tiers, group rules.
- Data is kept in memory only and is lost on exit.

## Testing the loyalty tiers
A new account is always 0%. Student menu option 8 ("Demo: set my registration date") moves the account start date into the past, e.g. `2026-05-11`, so the 5%, 10% and 15% tiers can be seen.

## Changes from the gym project
- Kept: abstract `Person` + subclasses, `Billable`, the List/Map/Queue manager design, `InputValidator`, the three custom exceptions, the menu loop with central error handling.
- Replaced: Basic/Premium/VIP tiers, trainer add-on, bonus months and `Renewable` with Tutor/Student, session pricing, loyalty tiers, slots and bookings.
- Fixed: the menu no longer loops forever when input ends; IDs are case-insensitive; setters validate (no silent ignores); the Queue now tracks pending group classes instead of a misleading "checked in" counter.
- New: `Slot`, `Booking`, `SessionType`, `Curriculum`, `Offerings`, `ConsoleInput`, `TutorPortal`, `StudentPortal`, `BookingException`, `InputEndedException`, and the test suite.
