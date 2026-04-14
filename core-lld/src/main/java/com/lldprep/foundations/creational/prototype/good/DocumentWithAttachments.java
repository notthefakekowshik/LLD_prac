package com.lldprep.foundations.creational.prototype.good;

import java.util.ArrayList;
import java.util.List;

/**
 * Deep Copy Prototype - handles mutable reference fields.
 * 
 * USE: When object contains mutable collections or references.
 * Must clone all mutable fields to avoid shared state.
 */
public class DocumentWithAttachments implements Prototype {
    private String title;
    private String content;
    private String author;
    private List<String> attachments;  // Mutable collection!
    private Metadata metadata;        // Mutable reference!
    
    public DocumentWithAttachments(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.attachments = new ArrayList<>();
        this.metadata = new Metadata();
    }
    
    /**
     * Deep copy - clones all mutable references.
     * Resulting object is completely independent.
     */
    @Override
    public DocumentWithAttachments clone() {
        try {
            DocumentWithAttachments cloned = (DocumentWithAttachments) super.clone();
            // DEEP COPY: Clone mutable fields
            cloned.attachments = new ArrayList<>(this.attachments);
            cloned.metadata = this.metadata.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Clone not supported", e);
        }
    }
    
    public void addAttachment(String attachment) {
        attachments.add(attachment);
    }
    
    public void setMetadata(String key, String value) {
        metadata.setProperty(key, value);
    }
    
    public List<String> getAttachments() { return attachments; }
    public Metadata getMetadata() { return metadata; }
    
    @Override
    public String toString() {
        return "DocumentWithAttachments{title='" + title + "', attachments=" + attachments 
            + ", metadata=" + metadata + ", hash=" + hashCode() + "}";
    }
    
    /**
     * Mutable reference class - must be cloneable for deep copy.
     */
    static class Metadata implements Cloneable {
        private java.util.Map<String, String> properties = new java.util.HashMap<>();
        
        public void setProperty(String key, String value) {
            properties.put(key, value);
        }
        
        public String getProperty(String key) {
            return properties.get(key);
        }
        
        @Override
        public Metadata clone() {
            try {
                Metadata cloned = (Metadata) super.clone();
                cloned.properties = new java.util.HashMap<>(this.properties);
                return cloned;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
        
        @Override
        public String toString() {
            return "Metadata" + properties + "@" + hashCode();
        }
    }
}
