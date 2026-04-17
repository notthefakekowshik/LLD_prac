package com.lldprep.foundations.structural.proxy.good;

/**
 * Proxy Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * You need to control access to an object — for lazy initialisation, caching, logging,
 * access control, or remote delegation — without the client knowing or the real object
 * being modified. The proxy sits in front of the real object and intercepts calls.
 *
 * <p><b>How it works:</b><br>
 * - {@code Image} — Subject interface; both proxy and real object implement it.<br>
 * - {@code RealImage} — Heavy object; client should not instantiate this directly.<br>
 * - {@code ImageProxy} — Proxy; same interface, lazy-init + cache logic, no disk I/O
 *   until actually needed. Client holds an {@code Image} reference — proxy is transparent.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li><b>Virtual Proxy</b> — lazy init of expensive objects (this demo).</li>
 *   <li><b>Caching Proxy</b> — cache results of expensive operations.</li>
 *   <li><b>Protection Proxy</b> — access control (check permissions before delegating).</li>
 *   <li><b>Remote Proxy</b> — represent a remote object locally (RMI, gRPC stubs).</li>
 *   <li><b>Logging Proxy</b> — add audit logging without touching the real class.</li>
 * </ul>
 *
 * <p><b>Proxy vs Decorator:</b><br>
 * - {@code Decorator} adds behaviour (wraps to enrich); client usually builds the stack.<br>
 * - {@code Proxy} controls access (wraps to guard/defer); client doesn't know it's a proxy.
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>Virtual proxy — image not loaded until display() is called</li>
 *   <li>Caching proxy — second display() reuses already-loaded image</li>
 *   <li>Never-displayed image — proves zero disk I/O occurred</li>
 * </ol>
 */
public class ProxyDemo {

    public static void main(String[] args) {
        demo1_LazyInit();
        demo2_CacheHit();
        demo3_NeverDisplayed();
    }

    // -------------------------------------------------------------------------

    private static void demo1_LazyInit() {
        section("Demo 1: Lazy init — image only loaded when display() is called");

        Image img = new ImageProxy("hero-banner.jpg");

        System.out.println("  [Client] Proxy created — no disk I/O yet");
        System.out.println("  [Client] Now calling display()...");
        img.display();
    }

    private static void demo2_CacheHit() {
        section("Demo 2: Cache hit — second display() reuses already-loaded image");

        Image img = new ImageProxy("product-photo.jpg");

        img.display();   // first call — loads from disk
        img.display();   // second call — served from cache, no disk I/O
        img.display();   // third call — still from cache
    }

    private static void demo3_NeverDisplayed() {
        section("Demo 3: Proxy created but never displayed — zero disk I/O");

        Image img1 = new ImageProxy("thumbnail-1.jpg");
        Image img2 = new ImageProxy("thumbnail-2.jpg");
        Image img3 = new ImageProxy("thumbnail-3.jpg");

        System.out.println("  [Client] Only displaying img1...");
        img1.display();

        System.out.println("  [Client] img2 and img3 were never displayed — never loaded from disk");
        // img2 and img3: RealImage was never constructed, no disk I/O at all
    }

    // -------------------------------------------------------------------------

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
