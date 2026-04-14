# Quick Pattern Reference Guide

## ⚡ Interview-Ready Pattern Cheat Sheet

### **When to Use Each Creational Pattern (2-Minute Review)**

| **Problem** | **Pattern** | **Real Example** | **Our Implementation** |
|---|---|---|---|
| Need exactly 1 instance | **Singleton** | Database connection pool | `BillPughSingleton` (recommended) |
| Create objects without hardcoding type | **Factory** | JDBC drivers, Spring beans | `NotificationFactory` |  
| Create families that work together | **Abstract Factory** | UI themes, database ecosystems | `WindowsGUIFactory` vs `MacGUIFactory` |
| Object has many optional parameters | **Builder** | HTTP requests, SQL queries | `User.Builder` with validation |
| Creating object is expensive | **Prototype** | Game pieces, VM templates | `Shape` cloning with registry |

### **Pattern Decision in 10 Seconds**

```
1 instance needed? → SINGLETON
Different creation logic? → FACTORY  
Families must match? → ABSTRACT FACTORY
Many parameters? → BUILDER
Expensive to create? → PROTOTYPE
```

### **Implementation Quick Start**

#### **Singleton (Production-Ready)**
```java
// Bill Pugh - RECOMMENDED approach
public class ConfigManager {
    private ConfigManager() {}
    
    private static class Holder {
        private static final ConfigManager INSTANCE = new ConfigManager();
    }
    
    public static ConfigManager getInstance() {
        return Holder.INSTANCE;
    }
}
```

#### **Factory (Extensible)**
```java
public interface NotificationFactory {
    static Notification create(String type) {
        return switch(type.toUpperCase()) {
            case "EMAIL" -> new EmailNotification();
            case "SMS" -> new SMSNotification();  
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}
```

#### **Builder (Fluent API)**
```java
public class User {
    private final String name;
    private final int age;
    
    private User(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
    }
    
    public static class Builder {
        private final String name;
        private int age = 0;
        
        public Builder(String name) { this.name = name; }
        public Builder age(int age) { this.age = age; return this; }
        public User build() { return new User(this); }
    }
}

// Usage: new User.Builder("John").age(30).build()
```

### **Interview Red Flags to Avoid**

❌ **Don't do this:**
- Singleton with public constructor
- Factory returning `Object` instead of interface
- Builder without fluent API
- Telescoping constructors (5+ parameters)

✅ **Do this:**
- Singleton with private constructor + getInstance()
- Factory returning common interface
- Builder with method chaining
- Builder for 4+ parameters

### **Complete Implementations Location**
📁 `core-lld/src/main/java/com/lldprep/foundations/creational/`

Each pattern includes:
- 📝 **JavaDoc** explaining WHY it exists
- ❌ **Bad example** (what NOT to do)
- ✅ **Good example** (proper implementation)  
- 🎯 **Demo** showing usage

### **Next Steps**
1. Review `FactoryDemo.java` for real usage
2. Run `GoodSingletonsDemo.java` to see all variations
3. Study `BuilderDemo.java` for advanced techniques
4. Move to **Structural Patterns** next
