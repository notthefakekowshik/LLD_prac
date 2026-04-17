package com.lldprep.foundations.structural.facade.good;

/**
 * Facade Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * A subsystem has many components that must be orchestrated in a specific order.
 * Without a facade, every client must know this order — coupling clients tightly
 * to subsystem internals. A facade provides a simple, unified interface.
 *
 * <p><b>How it works:</b><br>
 * - Subsystem classes ({@code Amplifier}, {@code DVDPlayer}, etc.) are unchanged.<br>
 * - {@code HomeTheaterFacade} owns instances of all subsystem components.<br>
 * - Client calls one method ({@code watchMovie()}) — facade handles all the steps.<br>
 * - Subsystem classes are still accessible directly if needed (facade doesn't lock you in).
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>You want to provide a simple interface to a complex subsystem.</li>
 *   <li>There are many clients that repeat the same orchestration steps.</li>
 *   <li>You want to layer your system — clients talk to the facade, not the internals.</li>
 * </ul>
 *
 * <p><b>Facade vs Adapter:</b><br>
 * - {@code Adapter} makes one interface look like another (interface translation).<br>
 * - {@code Facade} simplifies a complex subsystem (complexity hiding) — no interface mismatch.
 *
 * <p><b>Real-world examples:</b><br>
 * {@code OrderBookEngine.placeOrder()} (this repo), Spring's {@code JdbcTemplate},
 * any service layer hiding repository + cache + messaging calls from controllers.
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>watchMovie / endMovie — full lifecycle via facade</li>
 *   <li>Direct subsystem access when needed — facade doesn't block it</li>
 * </ol>
 */
public class FacadeDemo {

    public static void main(String[] args) {
        demo1_WatchAndEnd();
        demo2_DirectAccessStillWorks();
    }

    // -------------------------------------------------------------------------

    private static void demo1_WatchAndEnd() {
        section("Demo 1: Full movie lifecycle via facade");

        HomeTheaterFacade theater = buildTheater();

        theater.watchMovie("Inception");
        System.out.println("  ...[watching movie]...");
        theater.endMovie();
    }

    private static void demo2_DirectAccessStillWorks() {
        section("Demo 2: Direct subsystem access — facade is opt-in, not a lock-in");

        // Sometimes you only need one component — the facade doesn't hide access to subsystems
        Amplifier amp = new Amplifier();
        amp.on();
        amp.setVolume(5);
        amp.off();
    }

    // -------------------------------------------------------------------------

    private static HomeTheaterFacade buildTheater() {
        return new HomeTheaterFacade(
                new Amplifier(),
                new DVDPlayer(),
                new Projector(),
                new Lights()
        );
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
