import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.*;

/** HTTP layer over TheoremHouseManager (all rules stay in the original classes). Java 11+, no libraries. */
public class ApiServer {
    static final TheoremHouseManager m = new TheoremHouseManager();

    public static void main(String[] args) throws Exception {
        seed();
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
        s.createContext("/api/", ApiServer::api);
        s.createContext("/", ApiServer::file);
        s.start();
        System.out.println("Theorem House is running at http://localhost:" + port);
    }

    // ---------------------------------------------------------------- json
    static class Raw { final String s; Raw(String s) { this.s = s; } }
    static String v(Object o) {
        if (o == null) return "null";
        if (o instanceof Raw) return ((Raw) o).s;
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        return "\"" + o.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
    }
    static String obj(Object... kv) {
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) { if (i > 0) b.append(','); b.append(v(kv[i])).append(':').append(v(kv[i + 1])); }
        return b.append('}').toString();
    }
    static Raw arr(List<String> l) { return new Raw("[" + String.join(",", l) + "]"); }

    static Map<String, String> body(HttpExchange x) throws IOException {
        String t = new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> r = new HashMap<>();
        Matcher mt = Pattern.compile("\"(\\w+)\"\\s*:\\s*(\"((?:[^\"\\\\]|\\\\.)*)\"|[^,}\\s]+)").matcher(t);
        while (mt.find()) r.put(mt.group(1), mt.group(3) != null ? mt.group(3).replace("\\\"", "\"").replace("\\\\", "\\") : mt.group(2));
        return r;
    }
    static Map<String, String> query(HttpExchange x) {
        Map<String, String> r = new HashMap<>();
        String q = x.getRequestURI().getRawQuery();
        if (q != null) for (String p : q.split("&")) {
            String[] kv = p.split("=", 2);
            r.put(kv[0], kv.length > 1 ? java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return r;
    }
    static void send(HttpExchange x, int code, String type, byte[] data) throws IOException {
        x.getResponseHeaders().set("Content-Type", type);
        x.sendResponseHeaders(code, data.length);
        try (OutputStream o = x.getResponseBody()) { o.write(data); }
    }

    // ------------------------------------------------------------- mappers
    static String person(Person p) {
        LocalDate t = m.today();
        if (p instanceof Student) {
            Student s = (Student) p; int[] n = s.getNextTier(t);
            return obj("role", "student", "id", s.getId(), "name", s.getName(), "contact", s.getContactNumber(),
                    "grade", s.getGradeLevel(), "registered", s.getRegistrationDate().toString(),
                    "months", s.getAccountAgeMonths(t), "discount", s.getLoyaltyDiscountPercent(t),
                    "nextMonths", n == null ? null : n[0], "nextDiscount", n == null ? null : n[1],
                    "solo", s.computePrice(SessionType.SOLO, t), "group", s.computePrice(SessionType.GROUP, t));
        }
        Tutor u = (Tutor) p;
        return obj("role", "tutor", "id", u.getId(), "name", u.getName(), "contact", u.getContactNumber(),
                "solo", u.offersSolo(), "group", u.offersGroup());
    }
    static String slot(Slot s, Student st) {
        String tutor = s.getTutorId();
        try { tutor = m.findTutor(s.getTutorId()).getName(); } catch (Exception e) { }
        List<String> bs = new ArrayList<>();
        for (Booking b : s.getBookings()) {
            String n = b.getStudentId();
            try { n = m.findStudent(b.getStudentId()).getName(); } catch (Exception e) { }
            bs.add(obj("student", n, "unit", b.getUnit(), "grade", b.getGrade()));
        }
        return obj("id", s.getId(), "tutorId", s.getTutorId(), "tutor", tutor,
                "day", s.getDay().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), "dayNum", s.getDay().getValue(),
                "start", s.getStart().toString(), "end", s.getEnd().toString(), "type", s.getType().name(),
                "status", s.getStatusLabel(), "count", s.activeCount(), "grade", s.getGrade(),
                "scheduled", s.isScheduled(), "price", st == null ? null : st.computePrice(s.getType(), m.today()),
                "bookings", arr(bs));
    }
    static String booking(Booking b) {
        String sl = null;
        try { sl = slot(m.findSlot(b.getSlotId()), null); } catch (Exception e) { }
        return obj("id", b.getId(), "slotId", b.getSlotId(), "grade", b.getGrade(), "unit", b.getUnit(),
                "type", b.getType().name(), "price", b.getPrice(), "discount", b.getDiscountPercent(),
                "status", b.getStatus().name(), "slot", sl == null ? null : new Raw(sl));
    }
    static String info() {
        List<String> tiers = new ArrayList<>(), grades = new ArrayList<>();
        for (int[] t : Student.LOYALTY_TIERS) tiers.add(obj("months", t[0], "percent", t[1]));
        for (int g = Curriculum.MIN_GRADE; g <= Curriculum.MAX_GRADE; g++) {
            List<String> u = new ArrayList<>();
            for (String s : Curriculum.getUnits(g)) u.add(v(s));
            grades.add(obj("grade", g, "title", Curriculum.getTitle(g), "focus", Curriculum.getFocus(g), "units", arr(u)));
        }
        return obj("solo", SessionType.SOLO.getBasePrice(), "group", SessionType.GROUP.getBasePrice(),
                "groupMin", Slot.GROUP_MIN, "groupMax", Slot.GROUP_MAX, "tiers", arr(tiers), "grades", arr(grades));
    }

    // -------------------------------------------------------------- routes
    static synchronized void api(HttpExchange x) throws IOException {
        String path = x.getRequestURI().getPath().substring(5), res;
        int code = 200;
        try {
            Map<String, String> b = x.getRequestMethod().equals("POST") ? body(x) : query(x);
            res = route(path, b);
        } catch (MemberNotFoundException e) { code = 404; res = obj("error", e.getMessage());
        } catch (Exception e) { code = 400; res = obj("error", String.valueOf(e.getMessage())); }
        send(x, code, "application/json; charset=utf-8", res.getBytes(StandardCharsets.UTF_8));
    }

    static String route(String path, Map<String, String> b) throws Exception {
        String id = b.get("id") != null ? b.get("id") : b.get("studentId") != null ? b.get("studentId") : b.get("tutorId");
        switch (path) {
            case "info": return info();
            case "me": return person(m.findMemberById(id));
            case "register": {
                if ("tutor".equals(b.get("role"))) {
                    boolean solo = "true".equals(b.get("solo")), grp = "true".equals(b.get("group"));
                    InputValidator.validateOffering(solo, grp);
                    if (m.isIdTaken(id)) throw new DuplicateMemberException("ID '" + id.toUpperCase() + "' is taken. Try another.");
                    m.addMember(new Tutor(id, b.get("name"), b.get("contact"), Curriculum.SUBJECTS, solo, grp));
                } else {
                    if (m.isIdTaken(id)) throw new DuplicateMemberException("ID '" + id.toUpperCase() + "' is taken. Try another.");
                    m.addMember(new Student(id, b.get("name"), b.get("contact"), Integer.parseInt(b.get("grade")), m.today()));
                }
                return person(m.findMemberById(id));
            }
            case "update": {
                Person p = m.findMemberById(id);
                if (b.get("name") != null) m.updateMemberName(id, b.get("name"));
                if (b.get("contact") != null) m.updateMemberContact(id, b.get("contact"));
                if (b.get("grade") != null) m.updateStudentGrade(id, Integer.parseInt(b.get("grade")));
                if (b.get("solo") != null) m.updateTutorOffering(id, "true".equals(b.get("solo")), "true".equals(b.get("group")));
                return person(m.findMemberById(id));
            }
            case "delete": return obj("cancelled", m.deleteMember(id));
            case "demo/date": m.setRegistrationDate(id, InputValidator.parseDate(b.get("date"))); return person(m.findMemberById(id));
            case "slots": {
                Student st = b.get("studentId") != null ? m.findStudent(b.get("studentId")) : null;
                SessionType t = SessionType.valueOf(b.get("type"));
                List<String> out = new ArrayList<>();
                for (Slot s : m.getOpenSlots(t, Integer.parseInt(b.get("grade")))) out.add(slot(s, st));
                return arr(out).s;
            }
            case "tutor/slots": {
                List<String> out = new ArrayList<>();
                for (Slot s : m.getSlotsForTutor(id)) out.add(slot(s, null));
                return arr(out).s;
            }
            case "tutor/slots/add": {
                Slot s = m.addSlot(id, InputValidator.parseDay(b.get("day")), InputValidator.parseTime(b.get("start")),
                        InputValidator.parseTime(b.get("end")), SessionType.valueOf(b.get("type")));
                return slot(s, null);
            }
            case "tutor/slots/remove": return obj("cancelled", m.removeSlot(id, b.get("slotId")));
            case "book": return booking(m.bookSlot(id, b.get("slotId"), Integer.parseInt(b.get("grade")), b.get("unit")));
            case "cancel": return booking(m.cancelBooking(id, b.get("bookingId")));
            case "bookings": {
                List<String> out = new ArrayList<>();
                for (Booking k : m.getBookingsForStudent(id)) out.add(booking(k));
                return arr(out).s;
            }
            default: throw new BookingException("Unknown endpoint: " + path);
        }
    }

    static void file(HttpExchange x) throws IOException {
        String p = x.getRequestURI().getPath();
        Path root = Paths.get("web").toAbsolutePath().normalize();
        Path f = root.resolve(p.equals("/") ? "index.html" : p.substring(1)).normalize();
        if (!f.startsWith(root) || !Files.isRegularFile(f)) { send(x, 404, "text/plain", "Not found".getBytes()); return; }
        send(x, 200, f.toString().endsWith(".html") ? "text/html; charset=utf-8" : "application/octet-stream", Files.readAllBytes(f));
    }

    // ---------------------------------------------------------------- demo data
    static void seed() throws Exception {
        m.addMember(new Tutor("AMARA", "Amara Okafor", "09170000001", Curriculum.SUBJECTS, true, true));
        m.addMember(new Tutor("LEO", "Leo Marquez", "09170000002", Curriculum.SUBJECTS, true, true));
        m.addSlot("AMARA", DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(17, 0), SessionType.SOLO);
        Slot wed = m.addSlot("AMARA", DayOfWeek.WEDNESDAY, LocalTime.of(16, 0), LocalTime.of(17, 0), SessionType.GROUP);
        m.addSlot("AMARA", DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), SessionType.GROUP);
        m.addSlot("LEO", DayOfWeek.TUESDAY, LocalTime.of(17, 0), LocalTime.of(18, 0), SessionType.SOLO);
        m.addSlot("LEO", DayOfWeek.THURSDAY, LocalTime.of(17, 0), LocalTime.of(18, 0), SessionType.SOLO);
        m.addSlot("LEO", DayOfWeek.FRIDAY, LocalTime.of(16, 30), LocalTime.of(17, 30), SessionType.GROUP);
        String[] kids = {"Ben", "Chloe", "Dev", "Ella"};
        for (int i = 0; i < kids.length; i++) {
            m.addMember(new Student("KID" + (i + 1), kids[i], "0918000000" + i, 5, m.today()));
            m.bookSlot("KID" + (i + 1), wed.getId(), 5, "Fraction operations");
        }
        m.addMember(new Student("DEMO", "Demo Student", "09180000099", 5, m.today()));
        m.setRegistrationDate("DEMO", m.today().minusMonths(7));
    }
}
