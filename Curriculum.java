import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Theorem House's Math curriculum, Grades 1 to 10, taken from the supplied
 * "Complete Grade 1 to 10 Mathematics Curriculum" PDF (grade titles, the "Curriculum at a Glance"
 * focus line, and the unit titles of each grade).
 */
public final class Curriculum {
    public static final int MIN_GRADE = 1;
    public static final int MAX_GRADE = 10;

    public static final String SUBJECT_MATH = "Math";
    /** Subjects currently offered. Only Math for now; add more names here to extend. */
    public static final List<String> SUBJECTS = Collections.unmodifiableList(Arrays.asList(SUBJECT_MATH));

    private static final String[] TITLES = {
        "Mathematical Foundations",
        "Arithmetic Fluency and Measurement",
        "Multiplication, Division, Fractions, and Area",
        "Multi-Digit Operations and Fraction/Decimal Development",
        "Fractions, Decimals, Percentages, Ratios, and Volume",
        "Integers, Ratios, Expressions, and Pre-Algebra",
        "Proportional Reasoning and Linear Algebra",
        "Linear Equations, Functions, Exponents, and Geometry",
        "Algebra I and Mathematical Modeling",
        "Geometry, Advanced Algebra, and Trigonometry Foundations"
    };

    private static final String[] FOCUS = {
        "Counting, place value, addition/subtraction, shapes, measurement, time, money, data",
        "Three-digit numbers, regrouping, multiplication/division foundations, fractions, measurement",
        "Multi-digit operations, multiplication/division fluency, fractions, decimals, area",
        "Written algorithms, factors/multiples, fraction operations, decimal place value, angles",
        "Fraction/decimal operations, percentages, ratios, area, volume, statistics",
        "Integers, rational numbers, ratios, percent applications, expressions, equations",
        "Proportional relationships, multi-step equations, inequalities, geometry, probability",
        "Exponents, linear equations/functions, systems, Pythagorean theorem, transformations",
        "Algebra I, polynomials, factoring, linear functions, systems, quadratic foundations",
        "Geometry and proofs, trigonometry, circles, quadratics, radicals, sequences, models"
    };

    private static final String[][] UNITS = {
        // Grade 1
        {"Counting and number sense", "Addition concepts and facts", "Subtraction concepts and facts",
         "Place value and two-digit operations", "Shapes and spatial reasoning",
         "Measurement and comparison", "Time and money", "Data and patterns"},
        // Grade 2
        {"Numbers to 1,000", "Addition and subtraction", "Multiplication foundations",
         "Division foundations", "Fractions", "Measurement and unit conversion",
         "Time and money", "Geometry, data, and patterns"},
        // Grade 3
        {"Place value, rounding, and estimation", "Addition and subtraction", "Multiplication and division",
         "Fractions", "Decimals and money", "Measurement, perimeter, and area",
         "Geometry and angles", "Time, data, and problem solving"},
        // Grade 4
        {"Whole numbers and place value", "Multi-digit multiplication and division",
         "Factors, multiples, and primes", "Fraction equivalence and operations",
         "Decimal place value and operations", "Measurement and geometry",
         "Coordinates, symmetry, and shape", "Data and mathematical reasoning"},
        // Grade 5
        {"Whole-number operations and numerical reasoning", "Fraction operations", "Decimal operations",
         "Percentages and financial mathematics", "Ratios, rates, and proportional thinking",
         "Measurement, area, and volume", "Coordinate plane and geometry",
         "Statistics and problem-solving project"},
        // Grade 6
        {"Number system and divisibility", "Operations with fractions, decimals, and integers",
         "Ratios, rates, and proportional relationships", "Percent and consumer mathematics",
         "Algebraic expressions", "Equations and inequalities", "Geometry and measurement",
         "Statistics and probability"},
        // Grade 7
        {"Rational numbers and numerical fluency", "Ratios, rates, and proportional relationships",
         "Percent applications", "Algebraic expressions and properties", "Equations and inequalities",
         "Geometry: angles and shapes", "Three-dimensional measurement and coordinates",
         "Statistics and probability"},
        // Grade 8
        {"Real numbers, roots, and exponents", "Expressions and linear equations", "Linear inequalities",
         "Functions and linear relationships", "Systems of linear equations",
         "Pythagorean theorem and geometry", "Transformations and coordinate geometry",
         "Statistics and bivariate data"},
        // Grade 9
        {"Algebra language and expression skills", "Exponents and scientific notation",
         "Polynomials and factoring", "Linear equations and inequalities", "Linear functions and graphs",
         "Systems of equations and inequalities", "Quadratic foundations",
         "Data, modeling, and cumulative review"},
        // Grade 10
        {"Geometric foundations and reasoning", "Lines, angles, and parallel lines",
         "Triangles, congruence, and similarity", "Right triangles and trigonometry",
         "Quadrilaterals and polygons", "Circles and coordinate geometry",
         "Transformations and solid geometry",
         "Quadratics, radicals, rational expressions, and sequences",
         "Exponential models and financial applications", "Statistics, probability, and capstone"}
    };

    private Curriculum() {
    }

    private static void checkGrade(int grade) {
        if (grade < MIN_GRADE || grade > MAX_GRADE) {
            throw new IllegalArgumentException("Grade must be between " + MIN_GRADE + " and " + MAX_GRADE);
        }
    }

    public static String getTitle(int grade) {
        checkGrade(grade);
        return TITLES[grade - 1];
    }

    public static String getFocus(int grade) {
        checkGrade(grade);
        return FOCUS[grade - 1];
    }

    public static List<String> getUnits(int grade) {
        checkGrade(grade);
        return Collections.unmodifiableList(Arrays.asList(UNITS[grade - 1]));
    }

    /** "Curriculum at a Glance": one entry per grade with its title and main focus. */
    public static void printGlance() {
        System.out.println("\n=== THEOREM HOUSE MATH CURRICULUM (Grades 1-10) ===");
        for (int g = MIN_GRADE; g <= MAX_GRADE; g++) {
            System.out.printf("Grade %-2d - %s%n", g, getTitle(g));
            System.out.println("           Focus: " + getFocus(g));
        }
    }

    /** The unit titles of one grade, numbered so a student can pick one as a topic. */
    public static void printGradeUnits(int grade) {
        System.out.println("\nGrade " + grade + " - " + getTitle(grade));
        List<String> units = getUnits(grade);
        for (int i = 0; i < units.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + units.get(i));
        }
    }

    /** Interactive curriculum browser: glance table, then unit titles for any grade chosen. */
    public static void browse(ConsoleInput in) throws InvalidInputException {
        printGlance();
        while (true) {
            String text = in.readLine("\nEnter a grade (1-10) to see its units, or 0 to go back: ");
            int grade;
            try {
                grade = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                throw new InvalidInputException("Grade must be a whole number.");
            }
            if (grade == 0) {
                return;
            }
            InputValidator.validateGrade(grade);
            printGradeUnits(grade);
        }
    }
}
