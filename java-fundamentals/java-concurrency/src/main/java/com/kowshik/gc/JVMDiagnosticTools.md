# JVM Diagnostic Tools — CLI Quick Reference

Every Java developer should know these. Interviewers expect you to reach for the right tool.

---

## The Big 7

| Tool | What It Does | When |
|------|-------------|------|
| `jps` | List JVM processes (like `ps` for Java) | Find the PID |
| `jcmd` | Swiss army knife — heap dump, thread dump, JVM flags, native memory | Most operations |
| `jstat` | Real-time GC and classloading stats | Watch GC live |
| `jmap` | Heap histogram, heap dump | Memory analysis |
| `jstack` | Thread dump (stack traces of all threads) | Deadlock, stuck threads |
| `jinfo` | JVM system properties and flags | Verify config |
| `jhat` | Heap analysis tool (deprecated, use Eclipse MAT instead) | Legacy support |

---

## jps — Find Java Processes

```bash
jps -l          # List PIDs with main class name
jps -v          # Show JVM args
jps -lmv        # Everything: class + args + JVM flags
```

Output:
```
86721 com.kowshik.gc.GCMonitoringDemo -Xmx256m -Xlog:gc*
86785 jdk.jcmd/sun.tools.jps.Jps -lmv
```

---

## jcmd — Everything Tool

### Heap dump
```bash
jcmd <pid> GC.heap_dump /tmp/dump.hprof
```

### Thread dump
```bash
jcmd <pid> Thread.print           # All threads
jcmd <pid> Thread.print -e        # Extended (locks + monitors)
```

### JVM flags (including ergonomic defaults)
```bash
jcmd <pid> VM.flags               # All flags
jcmd <pid> VM.flags -all          # Include default values
```

### System properties
```bash
jcmd <pid> VM.system_properties
jcmd <pid> VM.command_line        # Command line that started the JVM
```

### Native memory tracking (requires -XX:NativeMemoryTracking=summary at startup)
```bash
jcmd <pid> VM.native_memory summary
jcmd <pid> VM.native_memory detail   # Detailed (slow)
```

### Force GC (testing only!)
```bash
jcmd <pid> GC.run
```

### Flight Recording (JFR) control
```bash
jcmd <pid> JFR.start name=profiling duration=60s filename=/tmp/recording.jfr
jcmd <pid> JFR.dump name=profiling filename=/tmp/dump.jfr
jcmd <pid> JFR.stop name=profiling
```

### List available commands
```bash
jcmd <pid> help
```

---

## jstat — Real-Time GC Statistics

### GC stats, updated every second
```bash
jstat -gc <pid> 1s         # Columns: S0C S1C S0U S1U EC EU OC OU MC MU CCSC CCSU YGC YGCT FGC FGCT ...
jstat -gcutil <pid> 1s     # Same but as percentages
```

### Key columns

| Column | Meaning | Red Flag |
|--------|---------|----------|
| `S0C/S1C` | Survivor capacity (KB) | — |
| `S0U/S1U` | Survivor used (KB) | — |
| `EC` | Eden capacity (KB) | — |
| `EU` | Eden used (KB) | Stays near EC → young gen too small |
| `OC` | Old gen capacity | — |
| `OU` | Old gen used | Growing monotonically → memory leak |
| `YGC` | Young GC count | — |
| `YGCT` | Young GC time (seconds) | High → tune young gen |
| `FGC` | Full GC count | Should be 0 or near-0 in steady state |
| `FGCT` | Full GC time (seconds) | Any Full GCs in production → investigate |

### Other jstat modes

```bash
jstat -class <pid> 1s      # Classes loaded/unloaded
jstat -compiler <pid> 1s   # JIT compilation stats
jstat -printcompilation <pid> 1s  # Methods being compiled
```

### Quick health check one-liner

```bash
jstat -gc <pid> 1s 5 | awk 'NR>2 {printf "YGC=%d FGC=%d OU=%dKB\n", $13, $15, $10}'
```

---

## jmap — Memory Map

### Live histogram (forces Full GC)
```bash
jmap -histo:live <pid> | head -30
```

### Histogram without Full GC
```bash
jmap -histo <pid> | head -30
```

### Heap dump (prefer jcmd for this)
```bash
jmap -dump:live,format=b,file=dump.hprof <pid>    # Forces Full GC first
jmap -dump:format=b,file=dump.hprof <pid>         # No Full GC
```

### Finalizer queue inspection
```bash
jmap -finalizerinfo <pid>
```

---

## jstack — Thread Dump

### Basic thread dump
```bash
jstack <pid>                    # All threads
jstack -l <pid>                 # With lock info (ownable synchronizers)
```

### Filter for thread states
```bash
jstack <pid> | grep "java.lang.Thread.State"
# Count by state:
jstack <pid> | grep "java.lang.Thread.State" | sort | uniq -c
```

### Deadlock detection
```bash
jstack <pid> | grep -A 50 "deadlock"
```

JStack is also accessible via `kill -3 <pid>` on Unix — thread dump goes to stdout of the JVM process.

### What to look for

| Thread State | Meaning | Action |
|-------------|---------|--------|
| `RUNNABLE` | Working or waiting for CPU | Normal |
| `BLOCKED` | Waiting for a monitor lock | Check which thread holds the lock |
| `WAITING` | `Object.wait()` or `LockSupport.park()` | Check what it's waiting on |
| `TIMED_WAITING` | `Thread.sleep()` or timed wait | Check timeout values |
| `DEADLOCK` | Circular lock dependency | Fix lock ordering |

---

## jinfo — JVM Configuration

```bash
jinfo <pid>                              # All flags and properties
jinfo -flag MaxHeapSize <pid>            # Specific flag
jinfo -flag +PrintGCDetails <pid>        # Enable flag dynamically
jinfo -flag -PrintGCDetails <pid>        # Disable flag dynamically
```

**Note**: Not all flags are manageable at runtime. `jinfo -flag` shows which are.

---

## Production Workflow: What Tool When

### Problem: "App is slow, maybe GC?"

```bash
jps -l                              # Find PID
jstat -gc <pid> 1s 10               # Watch GC for 10 samples
jstack <pid> > threads.txt          # Any BLOCKED threads?
jcmd <pid> VM.flags                 # What GC algo? What heap size?
```

### Problem: "App crashed with OOM"

```bash
# Check if heap dump was generated (if -XX:+HeapDumpOnOutOfMemoryError was set)
ls -lh java_pid*.hprof
# Open in Eclipse MAT → Leak Suspects → Dominator Tree → Path to GC Roots
```

### Problem: "Deadlock suspected"

```bash
jstack <pid> | grep -i deadlock -A 50
# Also: jstack will print "Found 1 deadlock." at the end
```

### Problem: "Memory growing slowly over hours"

```bash
# Take histogram every 5 minutes, compare
jmap -histo:live <pid> > histo_$(date +%H%M).txt
# Repeat every 5 min — diff to find which class is growing
diff histo_1400.txt histo_1405.txt | grep "^>" | head -10
```

---

## Interview Q&A

**Q: The app keeps getting slower over time. What do you do?**

> First I'd take GC stats with `jstat -gc <pid> 1s` to check if Full GCs are happening and if Old Gen is growing. Then a thread dump with `jstack <pid>` to look for BLOCKED threads or deadlocks. If Old Gen is growing monotonically, I'd take two heap histograms 5 minutes apart with `jmap -histo:live` and diff them to find the leaking class.

**Q: How do you know it's a memory leak vs. just needing more heap?**

> If `-Xmx` is already large and I see from `jstat` that Full GCs can't reclaim significant space (OU stays high after FGC), it's a leak. A properly sized app should see OU drop after Full GC. If OU drops after FGC, it's not a leak — just a busy app that needs more heap or more frequent GC.

**Q: What's the first tool you reach for?**

> `jcmd <pid> help` — it tells me what kind of process this is and lists all available diagnostic commands. Then `jstat -gc <pid> 1s` for the quick GC health check. That covers 80% of production issues.
