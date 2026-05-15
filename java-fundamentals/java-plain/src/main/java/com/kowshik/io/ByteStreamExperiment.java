package com.kowshik.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Java I/O Essentials for SDE-2 — The 5 Things You Actually Use at Work
 * =======================================================================
 *
 * This file covers ONLY the I/O patterns you'll use daily as a backend engineer.
 * No NIO channels, no memory-mapped files, no selectors — those are infra/platform-level.
 *
 * THE 5 ESSENTIALS:
 * 1. Reading files     — Files.readString(), Files.lines(), BufferedReader
 * 2. Writing files     — Files.writeString(), BufferedWriter, PrintWriter
 * 3. Resource streams  — Reading from classpath (config files, templates)
 * 4. try-with-resources — ALWAYS. No exceptions.
 * 5. Common pitfalls   — OOM on large files, encoding bugs, resource leaks
 *
 * RUN:
 *   mvn exec:java -Dexec.mainClass="com.kowshik.io.ByteStreamExperiment" -pl java-fundamentals/java-plain
 */
public class ByteStreamExperiment {

    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  Java I/O Essentials — SDE-2 Daily Toolkit");
        System.out.println("═══════════════════════════════════════════════════\n");

        setupDemoFiles();

        essential1_ReadingFiles();
        essential2_WritingFiles();
        essential3_ResourceStreams();
        essential4_TryWithResources();
        essential5_CommonPitfalls();
        essential6_FilesUtility();

        cleanup();
    }

    // ═══════════════════════════════════════════════════════════
    //  ESSENTIAL 1: Reading Files
    //  "How do I read a file?" — the question you'll ask most.
    // ═══════════════════════════════════════════════════════════

    private static void essential1_ReadingFiles() throws IOException {
        System.out.println("─── ESSENTIAL 1: Reading Files ───\n");

        // ✅ OPTION A: Read entire small file as String (config, JSON, small CSV)
        // USE WHEN: File is small (< 10MB), you need the whole content
        // ⚠️ DANGER: Loads entire file into heap — OOM on large files
        String content = Files.readString(Path.of("demo/config.json"), StandardCharsets.UTF_8);
        System.out.println("A) Files.readString() — small files:");
        System.out.println("   Content: " + content.trim().substring(0, Math.min(60, content.trim().length())) + "...\n");

        // ✅ OPTION B: Read all lines into List (small text files)
        // USE WHEN: You need all lines in memory and file is small
        List<String> lines = Files.readAllLines(Path.of("demo/users.csv"), StandardCharsets.UTF_8);
        System.out.println("B) Files.readAllLines() — small text files:");
        System.out.println("   Lines: " + lines.size() + " → first: " + lines.get(0) + "\n");

        // ✅ OPTION C: Stream lines lazily (large log files, big CSVs)
        // USE WHEN: File could be large, you process line-by-line
        // This is the GO-TO for large files — never loads entire file into memory
        System.out.println("C) Files.lines() — large files (lazy streaming):");
        try (Stream<String> lineStream = Files.lines(Path.of("demo/app.log"), StandardCharsets.UTF_8)) {
            long errorCount = lineStream
                    .filter(line -> line.contains("ERROR"))
                    .count();
            System.out.println("   Errors found: " + errorCount + "\n");
        }
        // ↑ try-with-resources is MANDATORY here — Files.lines() opens a file handle

        // ✅ OPTION D: BufferedReader — when you need more control than Stream
        // USE WHEN: Custom parsing logic, need to break mid-file, read header separately
        System.out.println("D) BufferedReader — custom parsing:");
        try (BufferedReader br = new BufferedReader(
                new FileReader("demo/users.csv", StandardCharsets.UTF_8))) {
            String header = br.readLine();  // read header separately
            System.out.println("   Header: " + header);
            String firstData = br.readLine();
            System.out.println("   First row: " + firstData + "\n");
        }

        System.out.println("   RULE: Small file? → readString/readAllLines");
        System.out.println("         Large file? → Files.lines() or BufferedReader\n");
    }

    // ═══════════════════════════════════════════════════════════
    //  ESSENTIAL 2: Writing Files
    // ═══════════════════════════════════════════════════════════

    private static void essential2_WritingFiles() throws IOException {
        System.out.println("─── ESSENTIAL 2: Writing Files ───\n");

        // ✅ OPTION A: Write a String to file (small content, configs, JSON output)
        Files.writeString(Path.of("demo/output.txt"), "Hello from SDE-2!\nLine 2\n",
                StandardCharsets.UTF_8);
        System.out.println("A) Files.writeString() — one-shot write\n");

        // ✅ OPTION B: Write lines
        Files.write(Path.of("demo/lines_output.txt"),
                List.of("line 1", "line 2", "line 3"),
                StandardCharsets.UTF_8);
        System.out.println("B) Files.write(List<String>) — write lines\n");

        // ✅ OPTION C: BufferedWriter for large/incremental writes
        // USE WHEN: Writing many lines in a loop (reports, exports, log generation)
        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter("demo/report.csv", StandardCharsets.UTF_8))) {
            bw.write("name,age,city");
            bw.newLine();
            for (int i = 0; i < 5; i++) {
                bw.write("user" + i + "," + (25 + i) + ",city" + i);
                bw.newLine();
            }
        }
        System.out.println("C) BufferedWriter — incremental writes (reports, exports)\n");

        // ✅ OPTION D: PrintWriter — convenient formatted output
        // USE WHEN: You want printf-style writing (logs, debug output, reports)
        try (PrintWriter pw = new PrintWriter(
                new BufferedWriter(new FileWriter("demo/formatted.txt", StandardCharsets.UTF_8)))) {
            pw.println("=== Report ===");
            pw.printf("Users: %d, Active: %d%n", 100, 87);
            pw.printf("Rate: %.2f%%%n", 87.0);
        }
        System.out.println("D) PrintWriter — printf-style formatted output\n");
    }

    // ═══════════════════════════════════════════════════════════
    //  ESSENTIAL 3: Reading from Classpath (Resources)
    //  "How do I read application.properties / templates / SQL files?"
    // ═══════════════════════════════════════════════════════════

    private static void essential3_ResourceStreams() throws IOException {
        System.out.println("─── ESSENTIAL 3: Classpath Resources ───\n");

        // In Spring Boot you'd put files in src/main/resources/
        // At runtime they're on the classpath, NOT on the filesystem.
        // You CAN'T use Files.readString() for classpath resources!

        // ✅ THE WAY: getResourceAsStream()
        // Returns an InputStream — wrap in InputStreamReader for text
        System.out.println("Reading classpath resources (how Spring loads configs):\n");

        // Simulate with a real file since we're not in a Spring app
        // In production this would be:
        //   InputStream is = getClass().getResourceAsStream("/application.properties");
        //   InputStream is = MyClass.class.getClassLoader().getResourceAsStream("templates/email.html");

        System.out.println("   // In Spring/real apps:");
        System.out.println("   InputStream is = getClass().getResourceAsStream(\"/application.properties\");");
        System.out.println("   String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);");
        System.out.println();
        System.out.println("   // Or with BufferedReader for line-by-line:");
        System.out.println("   try (BufferedReader br = new BufferedReader(");
        System.out.println("           new InputStreamReader(is, StandardCharsets.UTF_8))) {");
        System.out.println("       br.lines().forEach(System.out::println);");
        System.out.println("   }");
        System.out.println();

        // ✅ Properties loading — you'll do this often
        // Simulating with a file (in production: getResourceAsStream)
        Properties props = new Properties();
        try (InputStream is = new FileInputStream("demo/config.properties")) {
            props.load(is);
        }
        System.out.println("   Properties loaded: db.url=" + props.getProperty("db.url"));
        System.out.println("   Properties loaded: db.pool=" + props.getProperty("db.pool.size") + "\n");

        System.out.println("   KEY INSIGHT: Classpath resources → getResourceAsStream()");
        System.out.println("                Filesystem files    → Files.readString() / BufferedReader\n");
    }

    // ═══════════════════════════════════════════════════════════
    //  ESSENTIAL 4: try-with-resources — NON-NEGOTIABLE
    // ═══════════════════════════════════════════════════════════

    private static void essential4_TryWithResources() throws IOException {
        System.out.println("─── ESSENTIAL 4: try-with-resources ───\n");

        // ❌ BAD — resource leak if exception occurs
        System.out.println("❌ BAD — never do this:");
        System.out.println("   FileReader fr = new FileReader(\"data.txt\");");
        System.out.println("   String data = readAll(fr);  // if this throws, fr is NEVER closed");
        System.out.println("   fr.close();\n");

        // ✅ GOOD — guaranteed close
        System.out.println("✅ GOOD — always do this:");
        try (BufferedReader br = new BufferedReader(
                new FileReader("demo/users.csv", StandardCharsets.UTF_8))) {
            System.out.println("   try (BufferedReader br = ...) {");
            System.out.println("       // use br");
            System.out.println("   } // br.close() called automatically, even on exception\n");
        }

        // ✅ Multiple resources — closed in REVERSE order
        System.out.println("✅ Multiple resources (closed in reverse order):");
        System.out.println("   try (InputStream fis = new FileInputStream(\"in.txt\");");
        System.out.println("        OutputStream fos = new FileOutputStream(\"out.txt\")) {");
        System.out.println("       // use both");
        System.out.println("   } // fos.close() first, then fis.close()\n");

        // ✅ Files.lines() MUST be in try-with-resources
        System.out.println("⚠️  Files.lines() MUST be in try-with-resources:");
        System.out.println("   try (Stream<String> lines = Files.lines(path)) {");
        System.out.println("       lines.filter(...).forEach(...);");
        System.out.println("   } // underlying file handle closed here\n");

        System.out.println("   RULE: If it implements AutoCloseable/Closeable → try-with-resources. Always.\n");
    }

    // ═══════════════════════════════════════════════════════════
    //  ESSENTIAL 5: Common Pitfalls (what trips people up at work)
    // ═══════════════════════════════════════════════════════════

    private static void essential5_CommonPitfalls() throws IOException {
        System.out.println("─── ESSENTIAL 5: Common Pitfalls ───\n");

        // PITFALL 1: Not specifying charset → platform-dependent encoding bugs
        System.out.println("PITFALL 1: Missing charset → encoding bugs on different OS");
        System.out.println("   ❌ new FileReader(\"data.txt\")            // uses OS default (Windows=CP1252, Linux=UTF-8)");
        System.out.println("   ✅ new FileReader(\"data.txt\", UTF_8)     // explicit, portable");
        System.out.println("   ❌ Files.readString(path)                 // UTF-8 default (ok but be explicit)");
        System.out.println("   ✅ Files.readString(path, UTF_8)          // explicit intent\n");

        // PITFALL 2: readAllBytes on huge file → OOM
        System.out.println("PITFALL 2: Loading huge files into memory → OOM");
        System.out.println("   ❌ byte[] data = Files.readAllBytes(hugeLogFile);   // 2GB file → OOM");
        System.out.println("   ✅ Files.lines(hugeLogFile).filter(...).forEach(...) // lazy, constant memory\n");

        // PITFALL 3: Forgetting to close Files.lines()
        System.out.println("PITFALL 3: Files.lines() without try-with-resources → file handle leak");
        System.out.println("   ❌ Files.lines(path).filter(...).forEach(...);  // handle never closed!");
        System.out.println("   ✅ try (Stream<String> s = Files.lines(path)) { s.filter(...).forEach(...); }\n");

        // PITFALL 4: Writing without flush/close → data loss
        System.out.println("PITFALL 4: BufferedWriter data loss");
        System.out.println("   BufferedWriter holds data in memory until buffer is full or close() is called.");
        System.out.println("   If you don't close it, the last chunk of data may be LOST.\n");

        // PITFALL 5: Relative paths in production
        System.out.println("PITFALL 5: Relative file paths break in production");
        System.out.println("   ❌ new File(\"config.json\")        // relative to CWD — unpredictable");
        System.out.println("   ✅ Path.of(\"/etc/myapp/config.json\")  // absolute, or from env var");
        System.out.println("   ✅ getResourceAsStream(\"/config.json\") // classpath — works everywhere\n");
    }

    // ═══════════════════════════════════════════════════════════
    //  ESSENTIAL 6: Files utility methods you'll actually use
    // ═══════════════════════════════════════════════════════════

    private static void essential6_FilesUtility() throws IOException {
        System.out.println("─── ESSENTIAL 6: Files API Cheat Sheet ───\n");

        Path dir = Path.of("demo");
        Path file = Path.of("demo/output.txt");

        // Existence checks
        System.out.println("Existence & properties:");
        System.out.printf("   Files.exists(dir):       %s%n", Files.exists(dir));
        System.out.printf("   Files.isDirectory(dir):  %s%n", Files.isDirectory(dir));
        System.out.printf("   Files.isRegularFile(f):  %s%n", Files.isRegularFile(file));
        System.out.printf("   Files.size(f):           %d bytes%n", Files.size(file));

        // Copy & Move
        Files.copy(file, Path.of("demo/output_copy.txt"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("   Files.copy() — done");

        // Delete
        Files.deleteIfExists(Path.of("demo/output_copy.txt"));
        System.out.println("   Files.deleteIfExists() — done");

        // Create dirs
        Files.createDirectories(Path.of("demo/nested/deep"));
        System.out.println("   Files.createDirectories() — creates parent dirs too");

        // Walk directory tree
        System.out.println("\n   Files.walk() — find all .csv files:");
        try (Stream<Path> walk = Files.walk(dir, 2)) {
            walk.filter(p -> p.toString().endsWith(".csv"))
                    .forEach(p -> System.out.println("      → " + p));
        }

        // List directory contents
        System.out.println("\n   Files.list() — flat directory listing:");
        try (Stream<Path> listing = Files.list(dir)) {
            listing.filter(Files::isRegularFile)
                    .limit(5)
                    .forEach(p -> System.out.println("      → " + p.getFileName()));
        }

        System.out.println();

        // Cleanup nested dirs
        Files.deleteIfExists(Path.of("demo/nested/deep"));
        Files.deleteIfExists(Path.of("demo/nested"));
    }

    // ═══════════════════════════════════════════════════════════
    //  SETUP & CLEANUP
    // ═══════════════════════════════════════════════════════════

    private static void setupDemoFiles() throws IOException {
        Files.createDirectories(Path.of("demo"));

        Files.writeString(Path.of("demo/config.json"),
                "{\n  \"app\": \"my-service\",\n  \"port\": 8080,\n  \"env\": \"production\"\n}\n",
                StandardCharsets.UTF_8);

        Files.writeString(Path.of("demo/users.csv"),
                "name,age,city\nalice,30,bangalore\nbob,25,mumbai\ncharlie,35,delhi\n",
                StandardCharsets.UTF_8);

        StringBuilder log = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            String level = (i % 5 == 0) ? "ERROR" : (i % 3 == 0) ? "WARN" : "INFO";
            log.append("2024-01-15 10:23:").append(String.format("%02d", i))
                    .append(" ").append(level).append("  [http-nio-8080-exec-").append(i % 4 + 1)
                    .append("] com.app.Service - Processing request #").append(i).append("\n");
        }
        Files.writeString(Path.of("demo/app.log"), log.toString(), StandardCharsets.UTF_8);

        Files.writeString(Path.of("demo/config.properties"),
                "db.url=jdbc:postgresql://localhost:5432/mydb\ndb.pool.size=10\napp.name=my-service\n",
                StandardCharsets.UTF_8);

        System.out.println("→ Demo files created in demo/\n");
    }

    private static void cleanup() throws IOException {
        System.out.println("\n─── Cleanup ───");
        String[] files = {"config.json", "users.csv", "app.log", "config.properties",
                "output.txt", "lines_output.txt", "report.csv", "formatted.txt"};
        for (String f : files) {
            Files.deleteIfExists(Path.of("demo/" + f));
        }
        Files.deleteIfExists(Path.of("demo"));
        System.out.println("→ Demo files cleaned up.");
        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.println("  SUMMARY — Your SDE-2 I/O Toolkit:");
        System.out.println("  • Small file read  → Files.readString() / readAllLines()");
        System.out.println("  • Large file read  → Files.lines() (lazy Stream)");
        System.out.println("  • Custom parsing   → BufferedReader");
        System.out.println("  • Writing          → Files.writeString() / BufferedWriter");
        System.out.println("  • Classpath files  → getResourceAsStream()");
        System.out.println("  • File operations  → Files.copy/move/delete/walk");
        System.out.println("  • ALWAYS           → try-with-resources, explicit charset");
        System.out.println("═══════════════════════════════════════════════════");
    }
}
