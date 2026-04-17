package com.lldprep.foundations.structural.proxy.bad;

/**
 * BAD: Client loads the image eagerly — even if it's never displayed.
 *
 * Problems:
 * 1. Every image is loaded from disk at construction time, regardless of whether
 *    it will ever be rendered (e.g., images below the fold on a page).
 * 2. No caching — if the same image is requested twice, it's loaded twice.
 * 3. Client code has no way to add cross-cutting concerns (logging, access control,
 *    lazy init) without modifying the Image class itself — OCP violation.
 */
public class ProxyBad {

    public static void main(String[] args) {
        // Images loaded immediately at construction — even if never displayed
        RealImage img1 = new RealImage("photo1.jpg");
        RealImage img2 = new RealImage("photo2.jpg");

        System.out.println("  [Client] Only displaying img1 — img2 was loaded for nothing");
        img1.display();
    }
}

class RealImage {
    private final String filename;

    RealImage(String filename) {
        this.filename = filename;
        loadFromDisk(); // always loads eagerly
    }

    private void loadFromDisk() {
        System.out.printf("  [Disk] Loading heavy image: %s%n", filename);
    }

    public void display() {
        System.out.printf("  [Screen] Displaying: %s%n", filename);
    }
}
