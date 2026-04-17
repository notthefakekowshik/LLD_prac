package com.lldprep.foundations.structural.proxy.good;

/**
 * Proxy — implements the same {@link Image} interface as the real object.
 *
 * <p>Responsibilities handled here (without touching {@link RealImage}):
 * <ul>
 *   <li><b>Lazy initialisation</b> — {@link RealImage} is created only on first {@code display()} call.</li>
 *   <li><b>Caching</b> — once loaded, the same {@link RealImage} instance is reused on subsequent calls.</li>
 *   <li><b>Logging</b> — load and cache-hit events are recorded transparently.</li>
 * </ul>
 *
 * The client never knows it is talking to a proxy — it holds an {@link Image} reference.
 */
public class ImageProxy implements Image {

    private final String filename;
    private RealImage realImage;   // null until first display() call

    public ImageProxy(String filename) {
        this.filename = filename;
        System.out.printf("  [Proxy] Registered proxy for: %s (not loaded yet)%n", filename);
    }

    @Override
    public void display() {
        if (realImage == null) {
            // Lazy init — only now do we pay the cost of loading
            realImage = new RealImage(filename);
        } else {
            System.out.printf("  [Proxy] Cache hit — reusing loaded image: %s%n", filename);
        }
        realImage.display();
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
