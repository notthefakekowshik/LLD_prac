package com.lldprep.foundations.creational.prototype.good;

/**
 * Concrete Prototype - Document with shallow copy.
 * 
 * USE: When object contains only primitives/immutables.
 */
public class Document implements Prototype {
    private String title;
    private String content;
    private String author;
    
    public Document(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }
    
    // Shallow copy - sufficient for primitive/immutable fields
    @Override
    public Document clone() {
        try {
            return (Document) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Clone not supported", e);
        }
    }
    
    // Copy constructor alternative (more control)
    public Document(Document other) {
        this.title = other.title;
        this.content = other.content;
        this.author = other.author;
    }
    
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
    
    @Override
    public String toString() {
        return "Document{" + "title='" + title + '\'' + ", content='" + content + '\'' + 
               ", author='" + author + '\'' + ", hash=" + hashCode() + '}';
    }
}
