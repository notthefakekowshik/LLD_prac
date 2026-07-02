package com.kowshik.stringinternals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * String.intern() — Manual & Automatic Interning Explained
 *
 * ---------------------------------------------------------
 * What is intern()?
 * ---------------------------------------------------------
 * Every JVM maintains a "String pool" (aka String intern pool) — a fixed-size
 * internal hash table inside the JVM's native memory (not heap). Strings can be
 * placed into this pool either:
 *
 *   1. AUTOMATICALLY: All compile-time String literals ("hello") are interned
 *      at class-loading time.
 *
 *   2. MANUALLY: Calling s.intern() on a heap-allocated String.
 *
 * intern() logic in a nutshell:
 *   - If an "equal" String (by String.equals) already exists in the pool,
 *     return the pooled reference.
 *   - Otherwise, push a copy of this String into the pool and return that
 *     reference.
 *
 * The benefit: after interning, you can use `==` (reference equality) as a
 * fast, identity-level dedup mechanism instead of expensive `equals()` on long
 * strings.
 *
 * ---------------------------------------------------------
 * Why does == work after intern()?
 * ---------------------------------------------------------
 * Because EVERY String that is "equal" by .equals() returns the SAME pooled
 * instance after interning. No two distinct objects in the pool are .equals()
 * to each other — the pool is a deduplication set.
 *
 * ---------------------------------------------------------
 * Useful real-world scenarios
 * ---------------------------------------------------------
 * 1. Parsing huge CSVs where column names repeat millions of times — intern the
 *    column names and switch the processing to reference-equality comparison.
 * 2. XML/JSON parsers where tag names / keys repeat heavily. Intern them and
 *    use `==` for fast dispatch.
 * 3. Enum-like strings coming from external sources (config, network).
 * 4. Symbol tables in compilers/interpreters — String interning is effectively
 *    the "symbol table" pattern.
 *
 * ---------------------------------------------------------
 * When NOT to use intern()
 * ---------------------------------------------------------
 * - The String pool lives in native JVM memory (historically PermGen, now
 *   Metaspace/HotSpot's fixed-size table), and is NOT garbage-collected by
 *   normal GC cycles (pooled strings stay alive forever in older JVMs; in
 *   Java 7+ they are on the heap but still persist if reachable). Overloading
 *   it with millions of unique strings degrades JVM performance.
 * - intern() is a native call and has synchronization overhead — not free.
 * - If you only compare a string once or twice, the intern cost outweighs the
 *   benefit.
 *
 * ---------------------------------------------------------
 * Garbage Collection nuance (Java 7+)
 * ---------------------------------------------------------
 * Before Java 7: String pool was in PermGen (fixed size, not GC'd).
 * Java 7+:      String pool moved to the main heap — pooled strings can be
 *               GC'd if no references remain. Still, the pool hash table
 *               has a fixed capacity (configurable via -XX:StringTableSize).
 *
 * ---------------------------------------------------------
 * Command-line flags
 * ---------------------------------------------------------
 * -XX:+PrintStringTableStatistics  — print pool stats at JVM exit
 * -XX:StringTableSize=<N>           — pool hash table size (default ~60013,
 *                                      larger values reduce bucket collisions)
 */

public class StringInternDemo {

    public static void main(String[] args) {
        demoLiteralsAreAlreadyInterned();
        demoHeapStringsNeedManualIntern();
        demoDuplicateElimination();
        demoIdiomaticUseCase();
        demoJvmMemoryHint();
    }

    // -------------------------------------------------------
    // 1. Compile-time literals are automatically interned
    // -------------------------------------------------------
    private static void demoLiteralsAreAlreadyInterned() {
        header("1. LITERALS ARE ALREADY INTERNED");

        String a = "claude";
        String b = "claude";
        String c = "c" + "laude";      // constant-folded by compiler → "claude"

        System.out.printf("a == b   : %b  (two literal refs → same pool object)%n", a == b);
        System.out.printf("a == c   : %b  (constant-folded at compile time)%n", a == c);

        // Verify: all three point to the same interned instance
        System.out.printf("a.intern() == a  : %b  (literal IS the interned string)%n%n",
                          a.intern() == a);
    }

    // -------------------------------------------------------
    // 2. Heap-allocated Strings need manual intern()
    // -------------------------------------------------------
    private static void demoHeapStringsNeedManualIntern() {
        header("2. HEAP STRINGS vs INTERNED STRINGS");

        String heap1 = new String("sonnet");
        String heap2 = new String("sonnet");
        String literal = "sonnet";

        System.out.printf("heap1 == heap2              : %b  (two different heap objects)%n",
                          heap1 == heap2);
        System.out.printf("heap1.equals(heap2)         : %b  (content equal)%n",
                          heap1.equals(heap2));
        System.out.printf("heap1 == literal            : %b  (heap object ≠ pool object)%n",
                          heap1 == literal);
        System.out.printf("heap1.intern() == literal    : %b  (intern pulls pool ref)%n",
                          heap1.intern() == literal);
        System.out.printf("heap1.intern() == heap2.intern() : %b  (both intern → same pool ref)%n%n",
                          heap1.intern() == heap2.intern());
    }

    // -------------------------------------------------------
    // 3. Duplicate elimination — memory savings
    // -------------------------------------------------------
    private static void demoDuplicateElimination() {
        header("3. DUPLICATE ELIMINATION (MEMORY SAVINGS)");

        // Simulate reading 100_000 lines where 95% use 5 repeating strings
        String[] pool = {"SUCCESS", "FAILURE", "PENDING", "RETRY", "TIMEOUT"};

        // Without intern: every "SUCCESS" is a unique heap object
        long memBefore = approximateMemory();
        String[] raw = new String[100_000];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = new String(pool[i % pool.length]); // force distinct objects
        }
        long memRaw = approximateMemory() - memBefore;

        // With intern: all duplicates collapse to 5 pooled references
        System.gc(); // hint GC to get cleaner reading
        memBefore = approximateMemory();
        String[] interned = new String[100_000];
        for (int i = 0; i < interned.length; i++) {
            interned[i] = new String(pool[i % pool.length]).intern();
        }
        long memInterned = approximateMemory() - memBefore;

        System.out.printf("100,000 strings (5 unique values):%n");
        System.out.printf("  Without intern : ~%d bytes (all distinct heap objects)%n", memRaw);
        System.out.printf("  With intern    : ~%d bytes (only 5 pool objects + 100k refs)%n", memInterned);
        System.out.printf("  Memory saved   : ~%d bytes%n", memRaw - memInterned);

        // Speed comparison: == vs .equals on interning success
        long start = System.nanoTime();
        int matches = 0;
        String needle = pool[0].intern();
        for (String s : interned) {
            if (s == needle) matches++;
        }
        long refTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (String s : interned) {
            if (s.equals(needle)) matches++;
        }
        long eqTime = System.nanoTime() - start;

        System.out.printf("  == comparison  : %,d ns%n", refTime);
        System.out.printf("  .equals()      : %,d ns%n", eqTime);
        System.out.printf("  (after interning, == is safe and faster)%n%n");
    }

    // -------------------------------------------------------
    // 4. Idiomatic use-case: parsing with a dedup map
    // -------------------------------------------------------
    private static void demoIdiomaticUseCase() {
        header("4. IDIOMATIC USE-CASE: CVS/JSON COLUMN DEDUP");

        // Simulate a CSV parser that reads column names repeatedly.
        // Instead of calling intern() (which has native synchronization overhead
        // and pollutes the JVM-wide pool), we use a local HashMap<String, String>
        // as a private dedup table — same idea, lower JVM-level cost.
        Map<String, String> symbolTable = new HashMap<>();

        String[] csvColumns = {
            "user_id", "amount", "currency", "user_id", "amount",
            "user_id", "timestamp", "user_id", "amount", "status"
        };

        String[] deduped = new String[csvColumns.length];
        for (int i = 0; i < csvColumns.length; i++) {
            deduped[i] = symbolTable.computeIfAbsent(csvColumns[i], k -> k);
        }

        // Now we can use == safely within this parser
        int userIdCount = 0;
        for (String col : deduped) {
            if (col == symbolTable.get("user_id")) userIdCount++;
        }

        System.out.printf("CSV columns parsed: %d%n", csvColumns.length);
        System.out.printf("Deduped entry count: %d (vs 10 raw strings)%n", symbolTable.size());
        System.out.printf("'user_id' occurrences found via == : %d%n", userIdCount);
        System.out.printf("Note: HashMap-based dedup is safer than JVM-wide intern()%n");
        System.out.printf("for high-volume, dynamic string processing.%n%n");
    }

    // -------------------------------------------------------
    // 5. JVM memory awareness hint
    // -------------------------------------------------------
    private static void demoJvmMemoryHint() {
        header("5. JVM STRING POOL TUNING");

        System.out.printf("String pool is a fixed-size hash table in the JVM.%n");
        System.out.printf("Default size: ~60013 buckets.%n");
        System.out.printf("Start JVM with: -XX:StringTableSize=1000003 for larger tables.%n");
        System.out.printf("Print stats with: -XX:+PrintStringTableStatistics%n");
        System.out.printf("Run with -XX:+PrintStringTableStatistics to see pool usage after this demo.%n%n");
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private static void header(String title) {
        String line = "=".repeat(80);
        System.out.println(line);
        System.out.printf("  %s%n", title);
        System.out.println(line);
    }

    /**
     * Rough estimation of used heap. Not a precise measurement but enough
     * to show the order-of-magnitude difference.
     */
    private static long approximateMemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }
}
