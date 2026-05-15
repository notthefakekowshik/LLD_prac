# Java I/O — From First Principles to Production Systems

> "Every backend system is ultimately an I/O machine. Your REST API reads bytes from a socket,
> your database writes bytes to disk, your message queue moves bytes between processes.
> **Understand I/O and you understand the backbone of every system you'll ever build.**"

---

## Table of Contents

| # | Phase | Topic | Key Question |
|---|-------|-------|-------------|
| 1 | [The Problem](#1-the-problem--why-io-is-hard) | Why I/O Is Hard | What actually happens when you read a file? |
| 2 | [Byte Streams](#2-byte-streams--inputstream--outputstream) | InputStream / OutputStream | How does Java model raw bytes? |
| 3 | [Character Streams](#3-character-streams--reader--writer) | Reader / Writer | Why can't you just use InputStream for text? |
| 4 | [Buffering](#4-buffering--the-single-biggest-performance-win) | BufferedInputStream / BufferedReader | Why is unbuffered I/O 100x slower? |
| 5 | [Decorator Pattern](#5-the-decorator-pattern--javas-io-architecture) | Java I/O's Architecture | Why are there so many wrapper classes? |
| 6 | [Compression & Data Streams](#6-compression--data-streams) | GZIPStream, DataStream, ObjectStream | How do you layer functionality? |
| 7 | [try-with-resources](#7-resource-management--try-with-resources) | AutoCloseable, Resource Leaks | Why do file handles leak and how to prevent it? |
| 8 | [File & Path API](#8-file--path-api-nio2) | java.nio.file (NIO.2) | Modern file operations — Files, Path, walking trees |
| 9 | [FileChannel & NIO](#9-filechannel--nio) | Channels + Buffers | Why channels instead of streams? |
| 10 | [Memory-Mapped Files](#10-memory-mapped-files) | MappedByteBuffer | How does Kafka read 1M msgs/sec from disk? |
| 11 | [Blocking vs Non-Blocking I/O](#11-blocking-vs-non-blocking-io) | BIO / NIO / AIO | Why does Netty use non-blocking I/O? |
| 12 | [Selectors & Event Loops](#12-selectors--event-loops) | Selector, SelectionKey | How does 1 thread handle 10K connections? |
| 13 | [Zero-Copy & sendfile](#13-zero-copy--sendfile) | transferTo / transferFrom | How does Kafka avoid copying bytes through userspace? |
| 14 | [Real-World Systems](#14-real-world-backend-systems) | Kafka, Netty, Databases, Spring | How do production systems choose I/O strategies? |
| 15 | [Decision Framework](#15-decision-framework--when-to-use-what) | Cheat Sheet | Which I/O approach for which problem? |

---

## 1. The Problem — Why I/O Is Hard

### 1.1 What Actually Happens When You Read a File?

```
Your Java Code          JVM             OS Kernel           Disk Hardware
─────────────         ─────           ──────────          ──────────────
read(buffer)  ──→  syscall read()  ──→  Page Cache lookup
                                       ├─ HIT  → copy to userspace buffer ──→ return
                                       └─ MISS → schedule disk read (DMA)
                                                   ↓
                                              Disk controller
                                              reads sectors
                                                   ↓
                                              DMA copies to Page Cache
                                                   ↓
                                              Kernel copies to userspace ──→ return
```

**Key pain points:**
1. **Syscall overhead** — Every `read()` call crosses the user↔kernel boundary. This is expensive (~1-10μs per syscall).
2. **Disk latency** — SSD: ~100μs, HDD: ~5-10ms. If you read 1 byte at a time, you pay this penalty per byte.
3. **Encoding** — Raw bytes on disk might be UTF-8, UTF-16, ASCII, Latin-1. Reading bytes ≠ reading text.
4. **Resource management** — Every open file uses an OS file descriptor. Leak them and your process crashes.
5. **Blocking** — The thread that calls `read()` is STUCK until data arrives. 10,000 connections = 10,000 stuck threads.

### 1.2 The Evolution of Java I/O

```
Java 1.0 (1996)          Java 1.1 (1997)          Java 1.4 (2002)          Java 7 (2011)
──────────────          ───────────────          ──────────────          ─────────────
InputStream             Reader / Writer          NIO (Channels,          NIO.2 (Path,
OutputStream            (character streams       Buffers, Selectors,     Files, WatchService,
(byte streams only)      + encodings)            non-blocking I/O)       AsynchronousChannel)
```

Each generation was born from **real production pain** — not academic design.

---

## 2. Byte Streams — InputStream / OutputStream

### 2.1 The Core Abstraction

Everything in a computer is bytes. Java's most fundamental I/O abstraction:

```
InputStream                              OutputStream
───────────                              ────────────
int read()        → read ONE byte        void write(int b)      → write ONE byte
int read(byte[])  → read INTO buffer     void write(byte[])     → write FROM buffer
void close()      → release resources    void flush()           → force write to destination
                                         void close()
```

**Critical insight:** `read()` returns `int`, not `byte`. Why?
- It returns -1 to signal end-of-stream (EOF)
- A `byte` only holds 0–255, so -1 wouldn't fit
- The actual byte data is in the lower 8 bits of the int

### 2.2 Reading a File — The Naive Way

```java
// ⚠️ TERRIBLE PERFORMANCE — one syscall per byte!
FileInputStream fis = new FileInputStream("data.bin");
int b;
while ((b = fis.read()) != -1) {  // read ONE byte at a time
    process((byte) b);
}
fis.close(); // What if process() throws? RESOURCE LEAK!
```

**Problem:** If `data.bin` is 1MB, this makes ~1,000,000 syscalls.
Each syscall: ~1-10μs → Total: **1-10 seconds** just for syscall overhead.

### 2.3 Reading with a Buffer — Manual

```java
// ✅ MUCH better — one syscall per 8KB chunk
FileInputStream fis = new FileInputStream("data.bin");
byte[] buffer = new byte[8192]; // 8KB buffer
int bytesRead;
while ((bytesRead = fis.read(buffer)) != -1) {  // fills buffer, returns count
    for (int i = 0; i < bytesRead; i++) {
        process(buffer[i]);
    }
}
fis.close();
```

**1MB file / 8KB buffer = ~122 syscalls** instead of 1,000,000.

### 2.4 Concrete Implementations

| Class | Source/Destination | Use Case |
|-------|-------------------|----------|
| `FileInputStream` / `FileOutputStream` | File on disk | Read/write files |
| `ByteArrayInputStream` / `ByteArrayOutputStream` | In-memory byte[] | Testing, buffering in memory |
| `SocketInputStream` (internal) | Network socket | HTTP, database connections |
| `PipedInputStream` / `PipedOutputStream` | Inter-thread communication | Producer-consumer between threads |

---

## 3. Character Streams — Reader / Writer

### 3.1 The Problem: Bytes ≠ Characters

```
File on disk (UTF-8):   E4 B8 96 E7 95 8C    ← 6 bytes
What you want:          世界                    ← 2 characters

File on disk (UTF-16):  4E 16 75 4C            ← 4 bytes
What you want:          世界                    ← same 2 characters
```

InputStream gives you bytes. But text files have **encodings**. If you try to read UTF-8 bytes as characters, you'll corrupt data. Reader handles this.

### 3.2 The Abstraction

```
Reader                                   Writer
──────                                   ──────
int read()         → read ONE char       void write(int c)       → write ONE char
int read(char[])   → read INTO buffer    void write(char[])      → write FROM buffer
                                         void write(String)      → write string directly
```

### 3.3 The Bridge: InputStreamReader / OutputStreamWriter

```
Bytes on disk ──→ FileInputStream ──→ InputStreamReader(charset) ──→ Reader (chars)

        "Bridge" — converts byte stream to character stream using a charset decoder
```

```java
// Explicit encoding — always specify charset in production!
Reader reader = new InputStreamReader(new FileInputStream("data.txt"), StandardCharsets.UTF_8);

// Shorthand (Java 11+):
Reader reader = new FileReader("data.txt", StandardCharsets.UTF_8);
```

### 3.4 Why Not Just Cast Bytes to Chars?

```java
// ❌ WRONG — works for ASCII, corrupts everything else
char c = (char) inputStream.read();

// A UTF-8 character can be 1-4 bytes:
// 'A'  = 0x41                    (1 byte)
// '€'  = 0xE2 0x82 0xAC         (3 bytes)
// '𝄞'  = 0xF0 0x9D 0x84 0x9E   (4 bytes)
// Casting the first byte of '€' to char gives garbage
```

### 3.5 Concrete Implementations

| Class | Source/Destination | Notes |
|-------|-------------------|-------|
| `FileReader` | File | Convenience wrapper over InputStreamReader + FileInputStream |
| `StringReader` / `StringWriter` | In-memory String | Great for testing |
| `CharArrayReader` | In-memory char[] | |
| `InputStreamReader` | Any InputStream | The **bridge** — byte→char with charset |

---

## 4. Buffering — The Single Biggest Performance Win

### 4.1 The Problem Visualized

```
WITHOUT buffering (FileInputStream.read()):
┌──────────┐     syscall     ┌──────────┐     disk I/O    ┌──────┐
│ Your Code │ ──────────────→ │  Kernel  │ ──────────────→ │ Disk │
│  read 1B  │ ←────────────── │          │ ←────────────── │      │
│  read 1B  │ ──────────────→ │          │    (may be      │      │
│  read 1B  │ ←────────────── │          │    cached in    │      │
│  ...×1M   │                 │          │    page cache)  │      │
└──────────┘                 └──────────┘                 └──────┘
= 1,000,000 syscalls for 1MB file

WITH buffering (BufferedInputStream wrapping FileInputStream):
┌──────────┐    memcpy     ┌─────────────────┐   syscall   ┌──────────┐   disk I/O   ┌──────┐
│ Your Code │ ───────────→ │ BufferedInStream │ ──────────→ │  Kernel  │ ──────────→ │ Disk │
│  read 1B  │ ←─────────── │  8KB buffer[]   │ ←────────── │          │ ←────────── │      │
│  read 1B  │ ←─(from buf) │  (fills 8KB     │             │          │             │      │
│  read 1B  │ ←─(from buf) │   at a time)    │             │          │             │      │
│  ...×1M   │              │                 │             │          │             │      │
└──────────┘              └─────────────────┘             └──────────┘             └──────┘
= ~122 syscalls for 1MB file (8192 bytes per syscall)
```

### 4.2 BufferedInputStream — How It Works Internally

```java
// Simplified internal mechanics
public class BufferedInputStream extends InputStream {
    private byte[] buf;          // internal buffer (default 8192 bytes)
    private int pos;             // current read position in buffer
    private int count;           // number of valid bytes in buffer
    private InputStream in;      // the underlying stream

    public int read() {
        if (pos >= count) {              // buffer exhausted?
            fill();                       // refill from underlying stream (1 syscall)
            if (pos >= count) return -1;  // EOF
        }
        return buf[pos++] & 0xFF;        // return next byte from buffer (no syscall!)
    }

    private void fill() {
        count = in.read(buf, 0, buf.length);  // bulk read into buffer
        pos = 0;
    }
}
```

### 4.3 BufferedReader — Line Reading

```java
// BufferedReader adds readLine() — THE most used method for text files
BufferedReader br = new BufferedReader(new FileReader("log.txt", StandardCharsets.UTF_8));
String line;
while ((line = br.readLine()) != null) {
    processLine(line);
}
```

### 4.4 Performance Comparison

```
Reading a 100MB file:
──────────────────────────────────────────────────
Approach                       Time        Syscalls
──────────────────────────────────────────────────
FileInputStream.read()         ~12 sec     ~100M
BufferedInputStream.read()     ~0.3 sec    ~12K      ← 40x faster
FileInputStream.read(byte[])   ~0.3 sec    ~12K      ← same (manual buffer)
BufferedReader.readLine()      ~0.5 sec    ~12K      ← slightly more overhead (String creation)
Files.readAllBytes()           ~0.2 sec    ~few      ← bulk read (but loads entire file in memory)
──────────────────────────────────────────────────
```

### 4.5 Buffer Size — How to Choose?

| Size | When | Why |
|------|------|-----|
| 8KB (default) | General purpose | Matches typical OS page size, good syscall amortization |
| 64KB–256KB | Large file processing | Better throughput on SSDs, fewer syscalls |
| 1MB+ | Sequential bulk reads | Diminishing returns; watch memory usage |
| < 4KB | Almost never | Too many syscalls, wastes the optimization |

**Rule of thumb:** 8KB default is fine for 95% of cases. Only tune if profiling shows I/O as bottleneck.

---

## 5. The Decorator Pattern — Java's I/O Architecture

### 5.1 Why So Many Wrapper Classes?

Java I/O is the **textbook example of the Decorator pattern** (you've already studied this!).

```
Instead of creating:
  BufferedCompressedEncryptedFileInputStream    ← combinatorial explosion

Java lets you COMPOSE:
  new BufferedInputStream(
      new GZIPInputStream(
          new CipherInputStream(
              new FileInputStream("data.gz.enc")
          )
      )
  )
```

### 5.2 The Layering Architecture

```
┌─────────────────────────────────┐
│     BufferedInputStream          │  Layer 3: Performance (buffering)
│  ┌──────────────────────────┐   │
│  │   GZIPInputStream         │  │  Layer 2: Functionality (decompression)
│  │  ┌────────────────────┐  │   │
│  │  │  FileInputStream    │  │  │  Layer 1: Source (actual bytes from disk)
│  │  └────────────────────┘  │   │
│  └──────────────────────────┘   │
└─────────────────────────────────┘

read() call flows INWARD:
  BufferedInputStream.read()
    → checks its buffer, if empty calls:
    GZIPInputStream.read()
      → decompresses bytes, to get compressed bytes calls:
      FileInputStream.read()
        → syscall → kernel → disk
```

### 5.3 Common Decorator Stacks

```java
// 1. Read a text file efficiently
new BufferedReader(new FileReader("data.txt", UTF_8))

// 2. Read a gzipped text file
new BufferedReader(
    new InputStreamReader(
        new GZIPInputStream(
            new FileInputStream("data.txt.gz")), UTF_8))

// 3. Write objects to a compressed file
new ObjectOutputStream(
    new GZIPOutputStream(
        new BufferedOutputStream(
            new FileOutputStream("data.obj.gz"))))

// 4. Read a network stream with buffering
new BufferedInputStream(socket.getInputStream())
```

### 5.4 Decorator Pattern Mapping (from your design patterns study)

| Decorator Concept | Java I/O Equivalent |
|-------------------|---------------------|
| Component (interface) | `InputStream` |
| ConcreteComponent | `FileInputStream`, `ByteArrayInputStream` |
| Decorator (abstract) | `FilterInputStream` |
| ConcreteDecorator | `BufferedInputStream`, `GZIPInputStream`, `DataInputStream` |

---

## 6. Compression & Data Streams

### 6.1 GZIPInputStream / GZIPOutputStream

```java
// Writing compressed data
try (var gzOut = new GZIPOutputStream(new FileOutputStream("data.gz"))) {
    gzOut.write("Hello compressed world!".getBytes(UTF_8));
}

// Reading compressed data
try (var gzIn = new GZIPInputStream(new FileInputStream("data.gz"))) {
    byte[] data = gzIn.readAllBytes();  // Java 9+
    System.out.println(new String(data, UTF_8));
}
```

**Where you see this in production:**
- HTTP responses (`Content-Encoding: gzip`)
- Log file rotation (logback compresses rotated logs)
- Kafka message compression

### 6.2 DataInputStream / DataOutputStream

Read/write Java primitives in binary format (big-endian):

```java
// Write structured binary data
try (var dos = new DataOutputStream(new FileOutputStream("record.bin"))) {
    dos.writeUTF("Alice");       // length-prefixed UTF string
    dos.writeInt(42);            // 4 bytes, big-endian
    dos.writeDouble(3.14159);    // 8 bytes, IEEE 754
    dos.writeBoolean(true);      // 1 byte
}

// Read it back — must read in SAME ORDER
try (var dis = new DataInputStream(new FileInputStream("record.bin"))) {
    String name = dis.readUTF();
    int age = dis.readInt();
    double score = dis.readDouble();
    boolean active = dis.readBoolean();
}
```

### 6.3 ObjectInputStream / ObjectOutputStream (Serialization)

```java
// Write object graph to file
try (var oos = new ObjectOutputStream(new FileOutputStream("user.ser"))) {
    oos.writeObject(new User("Alice", 30));
}

// Read it back
try (var ois = new ObjectInputStream(new FileInputStream("user.ser"))) {
    User user = (User) ois.readObject();
}
```

**⚠️ Production warning:** Java serialization is slow, fragile, and a security risk.
Real systems use: Protocol Buffers, JSON (Jackson), Avro, MessagePack instead.

---

## 7. Resource Management — try-with-resources

### 7.1 The Problem

```java
// ❌ Resource leak if exception occurs between open and close
FileInputStream fis = new FileInputStream("data.txt");
processData(fis);      // What if this throws?
fis.close();           // NEVER REACHED → file descriptor leaked!

// ❌ Even try-finally has issues
FileInputStream fis = null;
try {
    fis = new FileInputStream("data.txt");
    processData(fis);
} finally {
    if (fis != null) fis.close();  // close() can ALSO throw, masking the original exception
}
```

### 7.2 The Solution — try-with-resources (Java 7+)

```java
// ✅ Guaranteed close, even on exception. Suppressed exceptions handled properly.
try (FileInputStream fis = new FileInputStream("data.txt")) {
    processData(fis);
}
// fis.close() called automatically here — even if processData() throws

// ✅ Multiple resources — closed in REVERSE order
try (FileInputStream fis = new FileInputStream("data.txt");
     BufferedInputStream bis = new BufferedInputStream(fis)) {
    processData(bis);
}
// bis.close() called first, then fis.close()
```

### 7.3 AutoCloseable Interface

```java
// Any class implementing AutoCloseable works with try-with-resources
public interface AutoCloseable {
    void close() throws Exception;
}

// Closeable extends AutoCloseable (for I/O — throws IOException specifically)
public interface Closeable extends AutoCloseable {
    void close() throws IOException;
}
```

---

## 8. File & Path API (NIO.2)

### 8.1 The Problem with `java.io.File`

The old `File` class (Java 1.0) has major issues:
- `delete()` returns `boolean` instead of throwing exceptions — silent failures
- No support for symbolic links, file attributes, file permissions
- Platform-inconsistent behavior
- No atomic operations
- No file watching

### 8.2 Path — The Modern Replacement

```java
// Old way
File file = new File("/var/log/app.log");

// New way (Java 7+)
Path path = Path.of("/var/log/app.log");            // Java 11+
Path path = Paths.get("/var/log/app.log");           // Java 7+
Path path = Path.of("/var", "log", "app.log");       // multi-part
```

### 8.3 Files — The Swiss Army Knife

```java
// Read entire file (small files only!)
String content = Files.readString(Path.of("config.json"));
List<String> lines = Files.readAllLines(Path.of("data.csv"));
byte[] bytes = Files.readAllBytes(Path.of("image.png"));

// Write
Files.writeString(Path.of("out.txt"), "Hello\n");
Files.write(Path.of("data.bin"), byteArray);

// Stream lines (lazy — handles huge files)
try (Stream<String> lines = Files.lines(Path.of("huge.log"))) {
    long errorCount = lines.filter(l -> l.contains("ERROR")).count();
}

// Copy, move, delete — with proper exceptions
Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
Files.delete(path);  // throws if file doesn't exist
Files.deleteIfExists(path);

// Walk directory tree
try (Stream<Path> walk = Files.walk(Path.of("/var/log"), 3)) {
    walk.filter(p -> p.toString().endsWith(".log"))
        .forEach(System.out::println);
}

// File attributes
long size = Files.size(path);
FileTime modified = Files.getLastModifiedTime(path);
boolean readable = Files.isReadable(path);
```

### 8.4 When to Use What

| Need | Use |
|------|-----|
| Read small file (< few MB) | `Files.readString()` / `Files.readAllBytes()` |
| Read large file line-by-line | `Files.lines()` (lazy Stream) or `BufferedReader` |
| Process huge binary file | `FileChannel` or `InputStream` with buffer |
| Copy/move/delete files | `Files.copy()` / `Files.move()` / `Files.delete()` |
| Walk directory tree | `Files.walk()` or `Files.find()` |

---

## 9. FileChannel & NIO

### 9.1 Streams vs Channels — The Mental Model

```
OLD (java.io) — Streams:
─────────────────────────
  One-directional (InputStream OR OutputStream)
  Byte-at-a-time or byte[]-at-a-time
  Blocking only
  No positioning (read forward only, unless you use RandomAccessFile)

NEW (java.nio) — Channels + Buffers:
────────────────────────────────────
  Bidirectional (read AND write from same channel)
  Always through a Buffer (ByteBuffer)
  Can be non-blocking (SocketChannel)
  Positionable (can seek to any offset)
  Supports scatter/gather I/O
  Supports memory mapping
  Supports file locking
```

### 9.2 ByteBuffer — The Core NIO Data Structure

```
ByteBuffer:
┌───────────────────────────────────────────────┐
│ position        limit           capacity      │
│    ↓              ↓                ↓          │
│ [ H | e | l | l | o | _ | _ | _ | _ | _ ]    │
│    0   1   2   3   4   5   6   7   8   9     │
└───────────────────────────────────────────────┘

position: where the next read/write will happen
limit:    boundary of readable/writable data
capacity: total buffer size (fixed at creation)

Key operations:
  put(byte)  → writes at position, advances position
  get()      → reads at position, advances position
  flip()     → sets limit=position, position=0  (switch from writing TO reading)
  clear()    → sets position=0, limit=capacity   (ready for new writes)
  compact()  → shifts unread data to beginning    (partial reads)
```

### 9.3 FileChannel — Reading and Writing

```java
// Reading with FileChannel
try (FileChannel channel = FileChannel.open(Path.of("data.bin"), StandardOpenOption.READ)) {
    ByteBuffer buffer = ByteBuffer.allocate(8192);
    while (channel.read(buffer) != -1) {
        buffer.flip();           // switch to read mode
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            process(b);
        }
        buffer.clear();          // switch back to write mode
    }
}

// Writing with FileChannel
try (FileChannel channel = FileChannel.open(Path.of("out.bin"),
        StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
    ByteBuffer buffer = ByteBuffer.wrap("Hello NIO!".getBytes(UTF_8));
    channel.write(buffer);
}

// Random access — read from specific position (no seeking needed!)
ByteBuffer buf = ByteBuffer.allocate(100);
channel.read(buf, 1024);  // read 100 bytes starting at offset 1024
```

### 9.4 Direct vs Heap Buffers

```java
// Heap buffer — allocated in JVM heap (default)
ByteBuffer heapBuf = ByteBuffer.allocate(8192);
// → Java byte[] on heap
// → For I/O, JVM must copy to a temporary direct buffer before syscall

// Direct buffer — allocated outside JVM heap (OS native memory)
ByteBuffer directBuf = ByteBuffer.allocateDirect(8192);
// → Off-heap memory
// → Avoids one copy on I/O (no heap→native copy needed)
// → Slower to allocate, not GC'd efficiently
// → Use for long-lived buffers that do lots of I/O
```

| | Heap Buffer | Direct Buffer |
|---|---|---|
| **Allocation** | Fast (just a byte[]) | Slow (OS malloc + JNI) |
| **GC** | Normal GC | Cleaned by Cleaner/finalizer |
| **I/O performance** | Extra copy (heap→native→kernel) | One less copy (native→kernel) |
| **Best for** | Short-lived, small buffers | Long-lived, high-throughput I/O |
| **Used by** | General application code | Netty, database drivers, Kafka |

---

## 10. Memory-Mapped Files

### 10.1 The Concept

Instead of read() syscalls, **map the file directly into your process's virtual memory**.
The OS page cache IS your buffer. No copies needed.

```
Traditional read():
  Disk → Page Cache → Kernel Buffer → User Buffer → Your Code
         (4 copies including DMA)

Memory-mapped:
  Disk → Page Cache ← Your Code reads directly from here
         (file IS your buffer — just pointer dereference)
```

### 10.2 How It Works

```java
try (FileChannel channel = FileChannel.open(Path.of("huge.dat"), StandardOpenOption.READ)) {
    // Map entire file into memory
    MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

    // Now you can access ANY byte instantly — no read() calls, no buffering logic
    byte firstByte = mapped.get(0);
    byte millionthByte = mapped.get(1_000_000);  // no seek needed!

    // Read structured data
    int header = mapped.getInt();        // reads 4 bytes at current position
    long timestamp = mapped.getLong();   // reads 8 bytes

    // The OS handles paging transparently:
    // - First access to a page → page fault → OS loads from disk → subsequent access is instant
    // - OS can evict pages under memory pressure and reload on demand
}
```

### 10.3 Map Modes

| Mode | Description | Use Case |
|------|-------------|----------|
| `READ_ONLY` | Read only | Analytics, log scanning |
| `READ_WRITE` | Changes written back to file | Database storage engines |
| `PRIVATE` | Copy-on-write (changes not written to file) | Safe experimentation |

### 10.4 When to Use

| ✅ Use memory-mapped files for | ❌ Don't use for |
|---|---|
| Large files (> 100MB) read randomly | Small files (overhead not worth it) |
| Files read many times | Files read once sequentially |
| Database storage engines | Network I/O (can't map sockets) |
| Shared memory between processes | Files that change size frequently |

### 10.5 Production Examples

- **Kafka** — Uses memory-mapped files for the log segment index. Random access to find message offsets.
- **RocksDB / LevelDB** — SSTable files are memory-mapped for read access.
- **Lucene (Elasticsearch)** — Memory-maps index segments for search.
- **SQLite** — Can use mmap for database file access.

---

## 11. Blocking vs Non-Blocking I/O

### 11.1 The C10K Problem

```
Blocking I/O (BIO) — Thread-per-connection:
────────────────────────────────────────────
Thread-1: [read...........blocked..........] [process] [write]
Thread-2: [read....blocked...] [process] [write.blocked.]
Thread-3: [read.blocked.] [process] [write]
...
Thread-10000: [read...blocked...]    ← 10K threads × 1MB stack = 10GB RAM just for stacks!

Non-Blocking I/O (NIO) — Event-driven:
───────────────────────────────────────
Single Thread: [check 10K channels] → [read ready ones] → [process] → [write ready ones] → [loop]
```

### 11.2 Blocking I/O Model

```java
// Traditional blocking server — 1 thread per connection
ServerSocket server = new ServerSocket(8080);
while (true) {
    Socket client = server.accept();           // BLOCKS until connection arrives
    new Thread(() -> {
        InputStream in = client.getInputStream();
        byte[] buf = new byte[1024];
        int n = in.read(buf);                  // BLOCKS until data arrives
        // process and respond...
    }).start();
}
// Problem: 10,000 connections = 10,000 threads
```

### 11.3 Non-Blocking I/O Model

```java
// NIO — channels can be set to non-blocking
ServerSocketChannel server = ServerSocketChannel.open();
server.bind(new InetSocketAddress(8080));
server.configureBlocking(false);               // ← KEY: non-blocking mode

SocketChannel client = server.accept();        // returns null immediately if no connection (not block)
if (client != null) {
    client.configureBlocking(false);
    ByteBuffer buf = ByteBuffer.allocate(1024);
    int n = client.read(buf);                  // returns 0 immediately if no data (not block)
}
```

### 11.4 Comparison

```
                  Blocking I/O              Non-Blocking I/O           Async I/O (AIO)
                  ────────────              ────────────────           ───────────────
Thread model      1 thread per connection   1 thread for many         OS handles completely
When read()       Blocks until data         Returns immediately       Callback when done
  returns         arrives                   (0 if no data, -1 EOF)
Complexity        Simple                    Complex (state machines)  Medium (callbacks)
Scalability       Hundreds of connections   Tens of thousands         Tens of thousands
Used by           Traditional servlets      Netty, Nginx, Node.js    Windows IOCP
Java API          java.io + Socket          java.nio (Channels)      AsynchronousChannel
```

---

## 12. Selectors & Event Loops

### 12.1 The Problem with Polling

Without selectors, non-blocking I/O requires busy-polling 10,000 channels:

```java
// ❌ Terrible — spin-loop checking every channel
while (true) {
    for (SocketChannel ch : allChannels) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        int n = ch.read(buf);  // check each one — wastes CPU
        if (n > 0) process(buf);
    }
}
```

### 12.2 Selector — Let the OS Tell You What's Ready

```java
Selector selector = Selector.open();

// Register channels with the selector
serverChannel.register(selector, SelectionKey.OP_ACCEPT);
clientChannel.register(selector, SelectionKey.OP_READ);

// Event loop — the heart of every high-performance server
while (true) {
    selector.select();  // BLOCKS until at least one channel is ready (OS-level epoll/kqueue)

    Set<SelectionKey> readyKeys = selector.selectedKeys();
    for (SelectionKey key : readyKeys) {
        if (key.isAcceptable()) {
            // New connection — accept it
            SocketChannel client = serverChannel.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
        }
        if (key.isReadable()) {
            // Data ready to read — process it
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buf = ByteBuffer.allocate(1024);
            client.read(buf);
            // process...
        }
    }
    readyKeys.clear();
}
```

### 12.3 Under the Hood — OS Primitives

| OS | Mechanism | How Selector uses it |
|----|-----------|---------------------|
| Linux | `epoll` | O(1) readiness notification, scales to millions of fds |
| macOS | `kqueue` | Similar to epoll, BSD-based |
| Windows | IOCP (I/O Completion Ports) | Proactor model (completion-based) |

### 12.4 This Is How Netty Works

```
Netty's EventLoop:
┌──────────────────────────────────────────────┐
│  while (true) {                              │
│      1. selector.select()    // wait for I/O │
│      2. processSelectedKeys()// handle I/O   │
│      3. runAllTasks()        // run queued    │
│  }                                           │
│                                              │
│  One EventLoop thread handles THOUSANDS of   │
│  connections via a single Selector.           │
└──────────────────────────────────────────────┘
```

---

## 13. Zero-Copy & sendfile

### 13.1 Traditional File→Socket Transfer

```
1. read(file, userBuffer)    →  Disk → PageCache → KernelBuffer → UserBuffer  (2 copies)
2. write(socket, userBuffer) →  UserBuffer → KernelSocketBuffer → NIC         (2 copies)
                                                                    Total: 4 copies, 2 syscalls

           ┌──────────┐     copy      ┌──────────┐     copy      ┌──────────┐
 Disk ───→ │PageCache │ ──────────→ │UserBuffer│ ──────────→ │ Socket   │ ───→ Network
           └──────────┘              └──────────┘              │ Buffer   │
                                                               └──────────┘
```

### 13.2 Zero-Copy with transferTo / sendfile

```java
// Java's zero-copy API
try (FileChannel fileChannel = FileChannel.open(Path.of("big-file.dat"));
     SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("host", 9092))) {
    fileChannel.transferTo(0, fileChannel.size(), socketChannel);
}
```

```
transferTo → sendfile() syscall:
           ┌──────────┐                              ┌──────────┐
 Disk ───→ │PageCache │ ────── DMA directly ────────→│   NIC    │ ───→ Network
           └──────────┘  (data never enters userspace)└──────────┘
                                               Total: 0 copies through userspace, 1 syscall
```

### 13.3 This Is How Kafka Achieves Insane Throughput

```
Kafka Broker serving a consumer:
1. Consumer requests messages from offset X
2. Broker finds the log segment file
3. Calls FileChannel.transferTo() → OS sendfile()
4. Data goes directly from page cache to network card
5. No serialization, no copying through JVM heap
→ Result: 1+ GB/sec throughput per broker
```

---

## 14. Real-World Backend Systems

### 14.1 Kafka

| Layer | I/O Strategy | Why |
|-------|-------------|-----|
| Log storage | Sequential append to FileChannel | Disk sequential write ≈ memory speed |
| Log index | Memory-mapped files (MappedByteBuffer) | Random access to offset index |
| Consumer reads | `FileChannel.transferTo()` (zero-copy) | Avoid copying through JVM heap |
| Compression | GZIPOutputStream / LZ4 / Snappy per batch | Reduce network + disk I/O |

### 14.2 Netty

| Layer | I/O Strategy | Why |
|-------|-------------|-----|
| Transport | NIO SocketChannel + Selector (event loop) | Handle 100K+ connections with few threads |
| Buffers | Direct ByteBuffers (off-heap) | Avoid GC, avoid heap↔native copy |
| Memory | PooledByteBufAllocator | Reuse buffers, avoid allocation overhead |
| File serving | `FileRegion` (wraps transferTo) | Zero-copy file serving |

### 14.3 Databases (PostgreSQL JDBC, HikariCP)

| Layer | I/O Strategy | Why |
|-------|-------------|-----|
| Wire protocol | BufferedInputStream / BufferedOutputStream over Socket | Simple, reliable, buffered |
| Large results | Streaming ResultSet (fetch size) | Don't load millions of rows into memory |
| Connection pool | Blocking I/O + thread pool (HikariCP) | Simpler than NIO for request-response |

### 14.4 Spring Boot / Tomcat

| Layer | I/O Strategy | Why |
|-------|-------------|-----|
| HTTP server | NIO connector (Tomcat NIO) | Handle many connections, thread-per-request dispatch |
| Request body | InputStream (buffered by Tomcat) | Streaming large uploads |
| File uploads | `MultipartFile` → temp file on disk | Don't hold huge files in memory |
| Responses | OutputStream (buffered) | Stream responses without buffering entire body |

---

## 15. Decision Framework — When to Use What

### 15.1 Quick Decision Tree

```
What are you doing?
│
├─ Reading/writing a SMALL file (< few MB)?
│   └─→ Files.readString() / Files.readAllBytes() / Files.writeString()
│
├─ Reading a LARGE text file line-by-line?
│   └─→ Files.lines() (Stream<String>) or BufferedReader
│
├─ Processing a LARGE binary file sequentially?
│   └─→ BufferedInputStream or FileChannel + ByteBuffer
│
├─ Random access on a HUGE file?
│   └─→ FileChannel with positional reads, or MappedByteBuffer
│
├─ High-throughput file → network transfer?
│   └─→ FileChannel.transferTo() (zero-copy)
│
├─ Handling MANY simultaneous network connections?
│   ├─ < 1000 connections? → Thread-per-connection (blocking I/O) is fine
│   └─ > 1000 connections? → NIO + Selector (or use Netty)
│
├─ Building a web server?
│   └─→ Use Netty or Spring WebFlux (they handle NIO for you)
│
└─ Building a database / message queue?
    └─→ FileChannel + MappedByteBuffer + zero-copy + direct buffers
```

### 15.2 The Complete Comparison

| Approach | Throughput | Latency | Memory | Complexity | When |
|----------|-----------|---------|--------|-----------|------|
| `InputStream` (unbuffered) | Terrible | High | Low | Simple | Never in production |
| `BufferedInputStream` | Good | Good | Low | Simple | Default choice for byte streams |
| `BufferedReader` | Good | Good | Low | Simple | Default choice for text files |
| `Files.readAllBytes()` | Great | Low | **HIGH** (entire file) | Trivial | Small files only |
| `Files.lines()` | Great | Good | Low (lazy) | Simple | Large text files with Stream API |
| `FileChannel` + ByteBuffer | Great | Good | Medium | Medium | When you need positioning or NIO features |
| `MappedByteBuffer` | Excellent | Lowest | Virtual (OS manages) | Medium | Random access on huge files |
| `transferTo()` (zero-copy) | Maximum | Lowest | Near-zero | Low | File → socket transfers |
| NIO + Selector | Best for scale | Depends | Low per connection | **High** | 10K+ concurrent connections |

---

## Learning Roadmap — Suggested Order

### Phase 1: Foundations (Do first — essential for any backend work)
- [ ] **Byte Streams** — `FileInputStream`, `FileOutputStream`, understand syscall overhead
- [ ] **Character Streams** — `Reader`, `Writer`, `InputStreamReader`, charset encoding
- [ ] **Buffering** — `BufferedInputStream`, `BufferedReader`, benchmark the difference
- [ ] **Decorator Pattern in I/O** — Stack streams, understand the layering
- [ ] **try-with-resources** — `AutoCloseable`, resource leak prevention
- [ ] **Files & Path API** — `Files.readString()`, `Files.lines()`, `Files.walk()`

### Phase 2: NIO & Channels (For systems programming / high-performance work)
- [ ] **ByteBuffer** — `allocate`, `allocateDirect`, `flip()`, `clear()`, `compact()`
- [ ] **FileChannel** — positional reads, scatter/gather, file locking
- [ ] **Memory-mapped files** — `MappedByteBuffer`, when Kafka uses it

### Phase 3: Network I/O & Scalability (For building servers / understanding frameworks)
- [ ] **Blocking vs Non-blocking** — `ServerSocket` vs `ServerSocketChannel`
- [ ] **Selectors & Event Loops** — How Netty's event loop works
- [ ] **Zero-copy** — `transferTo()`, how Kafka serves consumers

### Phase 4: Production Mastery (Connect everything to real systems)
- [ ] **Study Kafka's I/O architecture** — Why it's the fastest message broker
- [ ] **Study Netty's buffer management** — Why it uses direct buffers + pooling
- [ ] **Study Spring's I/O choices** — When Tomcat uses NIO vs blocking

---

## Suggested Experiment Files

| File | What to Build | Phase |
|------|--------------|-------|
| `ByteStreamExperiment.java` | Benchmark unbuffered vs buffered, measure syscalls | 1 |
| `CharsetExperiment.java` | Read UTF-8, UTF-16, see mojibake when encoding is wrong | 1 |
| `DecoratorStackExperiment.java` | Stack GZIPStream + BufferedStream + FileStream | 1 |
| `FilesAPIExperiment.java` | Use `Files.lines()`, `Files.walk()`, `Files.copy()` | 1 |
| `ByteBufferExperiment.java` | Practice flip/clear/compact, direct vs heap | 2 |
| `FileChannelExperiment.java` | Random access reads, positional writes | 2 |
| `MemoryMappedExperiment.java` | Map a large file, compare with FileChannel read | 2 |
| `BlockingServerExperiment.java` | Simple echo server with thread-per-connection | 3 |
| `NIOSelectorExperiment.java` | Same server with NIO Selector (single thread) | 3 |
| `ZeroCopyExperiment.java` | transferTo vs manual copy, benchmark | 3 |
