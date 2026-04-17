package com.lldprep.foundations.structural.proxy.good;

/**
 * Real Subject — does the actual heavy work (disk I/O).
 * The proxy controls access to this and creates it only when needed.
 */
public class RealImage implements Image {

    private final String filename;

    public RealImage(String filename) {
        this.filename = filename;
        loadFromDisk();
    }

    private void loadFromDisk() {
        System.out.printf("  [Disk] Loading heavy image: %s%n", filename);
    }

    @Override
    public void display() {
        System.out.printf("  [Screen] Displaying: %s%n", filename);
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
