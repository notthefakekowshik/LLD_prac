package com.lldprep.systems.splitwise.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

/**
 * A hands-on tour of BigDecimal. Run it top-to-bottom; each section prints what it demonstrates.
 * The recurring theme: BigDecimal tracks BOTH a value AND a scale (number of decimal places),
 * and most surprises come from the scale mattering when you didn't expect it to.
 */
public class BigDecimalDemo {

    public static void main(String[] args) {
        construction();       // 1. String vs double vs valueOf — the #1 bug
        scaleAndPrecision();  // 2. what "scale" and "precision" actually mean
        equalsVsCompareTo();  // 3. the one you asked about
        zeroComparison();     // 4. why Splitwise uses compareTo(ZERO), never equals
        hashSetTrap();        // 5. equals-with-scale bites HashSet/HashMap
        immutability();       // 6. every op returns a NEW object
        divisionTrap();       // 7. divide can throw — you MUST give scale+rounding
        roundingModes();      // 8. your section: HALF_UP vs HALF_DOWN vs HALF_EVEN
        roundingUnnecessary();// 9. UNNECESSARY throws if rounding was actually needed
        stripTrailingZeros(); // 10. the sneaky 1E+2 scientific-notation gotcha
        handyOperations();    // 11. negate/abs/min/max/movePoint/pow/constants
    }

    // 1. NEVER build money from a double literal — the double is already imprecise
    //    before BigDecimal ever sees it, and the constructor faithfully copies that garbage.
    private static void construction() {
        header("1. Construction: String vs double vs valueOf");
        System.out.println("new BigDecimal(0.1)        = " + new BigDecimal(0.1));       // 0.1000000000000000055511...
        System.out.println("new BigDecimal(\"0.1\")      = " + new BigDecimal("0.1"));    // exactly 0.1
        System.out.println("BigDecimal.valueOf(0.1)    = " + BigDecimal.valueOf(0.1));    // 0.1 (uses Double.toString)
        System.out.println("Rule: build from String (or valueOf for a known-clean double). Never `new BigDecimal(aDouble)`.");
    }

    // 2. scale = digits after the decimal point; precision = total significant digits;
    //    unscaledValue = the integer you'd get if you ignored the point.
    private static void scaleAndPrecision() {
        header("2. Scale / precision / unscaledValue");
        BigDecimal v = new BigDecimal("12.340");
        System.out.println("value          = " + v);
        System.out.println("scale()        = " + v.scale());          // 3  (three digits after '.')
        System.out.println("precision()    = " + v.precision());      // 5  (1,2,3,4,0 all count)
        System.out.println("unscaledValue()= " + v.unscaledValue());  // 12340
        System.out.println("So the number is literally unscaledValue * 10^-scale = 12340 * 10^-3.");
    }

    // 3. equals() compares value AND scale. compareTo() compares value only.
    //    2.0 and 2.00 are the SAME number but DIFFERENT objects to equals().
    private static void equalsVsCompareTo() {
        header("3. equals() vs compareTo()  <-- the big one");
        BigDecimal a = new BigDecimal("2.0");
        BigDecimal b = new BigDecimal("2.00");
        System.out.println("a = " + a + " (scale " + a.scale() + "),  b = " + b + " (scale " + b.scale() + ")");
        System.out.println("a.equals(b)    = " + a.equals(b));     // false — scales differ (1 vs 2)
        System.out.println("a.compareTo(b) = " + a.compareTo(b));  // 0 — numerically equal
        System.out.println("Takeaway: use compareTo() for 'is it the same amount?'. equals() only if scale must match too.");
    }

    // 4. Corollary of #3: ZERO has scale 0, so ZERO.equals("0.00") is false.
    //    Every zero/sign check in Splitwise uses compareTo for exactly this reason.
    private static void zeroComparison() {
        header("4. Comparing to zero");
        BigDecimal balance = new BigDecimal("0.00");
        System.out.println("BigDecimal.ZERO.equals(0.00) = " + BigDecimal.ZERO.equals(balance));       // false!
        System.out.println("balance.compareTo(ZERO)      = " + balance.compareTo(BigDecimal.ZERO));     // 0  -> "is zero"
        System.out.println("Idiom: x.compareTo(BigDecimal.ZERO) > 0  (positive),  == 0 (zero),  < 0 (negative).");
    }

    // 5. Because equals() includes scale, 2.0 and 2.00 are two DIFFERENT keys.
    //    This is why the Splitwise code normalizes everything to scale 2 before storing.
    private static void hashSetTrap() {
        header("5. HashSet / HashMap trap");
        Set<BigDecimal> set = new HashSet<>();
        set.add(new BigDecimal("2.0"));
        set.add(new BigDecimal("2.00"));
        System.out.println("set = " + set + "  size = " + set.size());  // size 2 — same value, two entries
        System.out.println("Fix: normalize scale (e.g. setScale(2)) before using BigDecimal as a key.");
    }

    // 6. BigDecimal is immutable. Operations RETURN a result; they never mutate in place.
    //    Forgetting to assign is a silent no-op bug.
    private static void immutability() {
        header("6. Immutability");
        BigDecimal price = new BigDecimal("100.00");
        price.add(new BigDecimal("50.00"));                 // result thrown away!
        System.out.println("after ignored add()   = " + price);                     // still 100.00
        BigDecimal total = price.add(new BigDecimal("50.00"));
        System.out.println("assigned add() result = " + total);                      // 150.00
    }

    // 7. divide() with no scale throws if the result can't be represented exactly
    //    (non-terminating decimal like 1/3). You must supply a scale + RoundingMode.
    private static void divisionTrap() {
        header("7. Division trap");
        BigDecimal ten = new BigDecimal("10");
        System.out.println("10 / 2 (exact)   = " + ten.divide(new BigDecimal("2")));  // 5 — terminates, fine
        try {
            new BigDecimal("1").divide(new BigDecimal("3"));                          // 0.333... never ends
        } catch (ArithmeticException e) {
            System.out.println("1 / 3 (no scale) -> ArithmeticException: " + e.getMessage());
        }
        System.out.println("1 / 3 (scale 4)  = " + new BigDecimal("1").divide(new BigDecimal("3"), 4, RoundingMode.HALF_UP));
    }

    // 8. Your original section. HALF_UP/HALF_DOWN only differ on an EXACT tie (…5 with nothing after).
    //    HALF_EVEN ("banker's rounding") breaks ties toward the even neighbour — the finance default,
    //    because it doesn't bias sums upward over many roundings.
    private static void roundingModes() {
        header("8. Rounding modes");
        // Not a tie -> every mode agrees:
        System.out.println("12.567 HALF_UP   = " + scaled("12.567", RoundingMode.HALF_UP));   // 12.57
        System.out.println("12.564 HALF_UP   = " + scaled("12.564", RoundingMode.HALF_UP));   // 12.56
        // Exact tie at 12.565 -> modes diverge:
        System.out.println("12.565 HALF_UP   = " + scaled("12.565", RoundingMode.HALF_UP));   // 12.57 (away from zero)
        System.out.println("12.565 HALF_DOWN = " + scaled("12.565", RoundingMode.HALF_DOWN)); // 12.56 (toward zero)
        System.out.println("12.565 HALF_EVEN = " + scaled("12.565", RoundingMode.HALF_EVEN)); // 12.56 (6 is even)
        System.out.println("12.575 HALF_EVEN = " + scaled("12.575", RoundingMode.HALF_EVEN)); // 12.58 (8 is even)
    }

    // 9. RoundingMode.UNNECESSARY asserts "no rounding should be needed here" — and throws if it is.
    //    Splitwise uses it as ZERO.setScale(2, UNNECESSARY): a scale-2 zero that's guaranteed exact.
    private static void roundingUnnecessary() {
        header("9. RoundingMode.UNNECESSARY");
        System.out.println("12.50 -> scale2 UNNECESSARY = " + scaled("12.50", RoundingMode.UNNECESSARY)); // fine, no rounding
        try {
            scaled("12.567", RoundingMode.UNNECESSARY);                                                    // needs rounding
        } catch (ArithmeticException e) {
            System.out.println("12.567 -> scale2 UNNECESSARY -> ArithmeticException (rounding WAS needed)");
        }
    }

    // 10. stripTrailingZeros() can flip an integer into scientific notation (100 -> 1E+2)
    //     because it drops the point entirely (scale goes negative). toPlainString() undoes it.
    private static void stripTrailingZeros() {
        header("10. stripTrailingZeros + toPlainString");
        BigDecimal money = new BigDecimal("600.00").stripTrailingZeros();
        System.out.println("600.00 stripped .toString()      = " + money);                    // 6E+2  (surprise!)
        System.out.println("600.00 stripped .toPlainString() = " + money.toPlainString());    // 600
        System.out.println("Lesson: for display, prefer setScale(2) or toPlainString(); avoid raw stripTrailingZeros().");
    }

    // 11. The everyday toolkit — all immutable, all return new BigDecimals.
    private static void handyOperations() {
        header("11. Handy operations");
        BigDecimal x = new BigDecimal("-7.25");
        System.out.println("abs(-7.25)        = " + x.abs());                         // 7.25
        System.out.println("negate(-7.25)     = " + x.negate());                      // 7.25
        System.out.println("min(3, 5)         = " + new BigDecimal("3").min(new BigDecimal("5")));  // 3
        System.out.println("max(3, 5)         = " + new BigDecimal("3").max(new BigDecimal("5")));  // 5
        System.out.println("12.34 movePointLeft(2)  = " + new BigDecimal("12.34").movePointLeft(2));  // 0.1234
        System.out.println("2 pow 10          = " + new BigDecimal("2").pow(10));     // 1024
        System.out.println("constants: ZERO=" + BigDecimal.ZERO + " ONE=" + BigDecimal.ONE + " TEN=" + BigDecimal.TEN);
    }

    private static BigDecimal scaled(String value, RoundingMode mode) {
        return new BigDecimal(value).setScale(2, mode);
    }

    private static void header(String title) {
        System.out.println("\n===== " + title + " =====");
    }
}
