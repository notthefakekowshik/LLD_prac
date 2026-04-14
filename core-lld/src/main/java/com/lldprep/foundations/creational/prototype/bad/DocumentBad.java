package com.lldprep.foundations.creational.prototype.bad;

/**
 * BAD: Manual object copying with copy constructors.
 * 
 * PROBLEMS:
 * 1. Copy constructor needs to know all subclass fields
 * 2. Must update when class changes - violates OCP
 * 3. Shallow vs deep copy bugs (references shared unintentionally)
 * 4. Doesn't scale with inheritance hierarchies
 */
public class DocumentBad {
    private String title;
    private String content;
    private Author author;  // Reference type - shallow copy issues!
    
    public DocumentBad(String title, String content, Author author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }
    
    // Copy constructor - MANUAL and ERROR-PRONE
    public DocumentBad(DocumentBad other) {
        this.title = other.title;        // String is immutable - OK
        this.content = other.content;    // String is immutable - OK
        this.author = other.author;      // DANGER: Shallow copy!
        // Both documents share same Author object!
    }
    
    static class Author {
        String name;
        String email;
        
        Author(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
