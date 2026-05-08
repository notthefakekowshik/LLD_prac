# Java I/O: Decorator Pattern in Action

Java's I/O library is the **classic example** of the Decorator pattern. `FileReader` is just one concrete component—decorators add behavior without subclass explosion.

---

## Class Hierarchy

```
┌─────────────────────────────────────────────────────────┐
│              java.io.Reader (abstract)                  │
│              ↑ Component Interface                      │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌───────────────┐ ┌───────────────┐ ┌─────────────────┐
│  FileReader   │ │ StringReader  │ │ CharArrayReader │
│  (File)       │ │  (String)     │ │   (char[])      │
└───────────────┘ └───────────────┘ └─────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│           Decorators (wrap any Reader)                  │
├─────────────────────────────────────────────────────────┤
│  BufferedReader    → Adds buffering for performance     │
│  LineNumberReader  → Tracks line numbers               │
│  PushbackReader    → Allows "unreading" characters      │
│  InputStreamReader → Bridges byte streams to char      │
└─────────────────────────────────────────────────────────┘
```

---

## Basic Usage

### Step 1: Concrete Component
```java
// Plain FileReader — reads characters from file, unbuffered
Reader fileReader = new FileReader("data.txt");
int c = fileReader.read();  // Single char at a time (slow!)
```

### Step 2: Add Buffering (Decorator)
```java
// Wrap FileReader with BufferedReader for performance
Reader buffered = new BufferedReader(new FileReader("data.txt"));
String line = buffered.readLine();  // Reads entire line (fast!)
```

### Step 3: Stack Multiple Decorators
```java
// FileReader → BufferedReader → LineNumberReader
Reader reader = new LineNumberReader(
                    new BufferedReader(
                        new FileReader("data.txt")));

// Now you have: file reading + buffering + line tracking
String line;
while ((line = ((BufferedReader) reader).readLine()) != null) {
    int lineNum = ((LineNumberReader) reader).getLineNumber();
    System.out.println(lineNum + ": " + line);
}
```

---

## Why This Is Decorator Pattern

| Pattern Element | Java I/O Implementation |
|-----------------|------------------------|
| **Component** | `Reader` abstract class |
| **Concrete Component** | `FileReader`, `StringReader`, `CharArrayReader` |
| **Decorator Base** | `BufferedReader`, `FilterReader` (both extend `Reader`) |
| **Concrete Decorators** | `LineNumberReader extends BufferedReader`, `PushbackReader` |
| **Delegation** | All decorators hold a `Reader in` and delegate `read()` calls |

### Key Code from BufferedReader
```java
public class BufferedReader extends Reader {
    private Reader in;  // The wrapped Reader (can be ANY Reader!)
    private char[] cb;  // Buffer

    public BufferedReader(Reader in) {
        this(in, defaultCharBufferSize);
    }

    public String readLine() throws IOException {
        // Uses buffering logic, then delegates to in.read()
        // ...
        return line;
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        // Delegates to wrapped Reader when buffer empty
        return in.read(cbuf, off, len);
    }
}
```

---

## Without Decorator: Class Explosion Problem

If Java used inheritance instead of composition, you'd need:

```
❌ BufferedFileReader
❌ BufferedStringReader
❌ BufferedCharArrayReader
❌ LineNumberedFileReader
❌ LineNumberedStringReader
❌ ... (dozens more)
```

**With Decorator**: Just 3 components + 4 decorators = **any combination you need**

---

## Other Java Decorator Examples

| Stream Type | Decorators Available |
|-------------|---------------------|
| **InputStream** | `BufferedInputStream`, `DataInputStream`, `GZIPInputStream`, `ObjectInputStream` |
| **OutputStream** | `BufferedOutputStream`, `DataOutputStream`, `GZIPOutputStream`, `PrintStream` |
| **Writer** | `BufferedWriter`, `FilterWriter`, `PrintWriter` |

### Example: GZIP + Buffering + File
```java
// FileOutputStream → BufferedOutputStream → GZIPOutputStream
OutputStream out = new GZIPOutputStream(
                       new BufferedOutputStream(
                           new FileOutputStream("data.gz")));
```

---

## Quick Reference

```java
// Read file with buffering
Reader r1 = new BufferedReader(new FileReader("file.txt"));

// Read with line numbers + buffering
Reader r2 = new LineNumberReader(new BufferedReader(new FileReader("file.txt")));

// Read string with pushback capability
Reader r3 = new PushbackReader(new StringReader("hello"));
```

**Rule of thumb**: Always wrap `FileReader` with `BufferedReader` for performance—it's the standard idiom.
