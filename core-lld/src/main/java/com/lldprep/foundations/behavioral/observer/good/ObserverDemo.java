package com.lldprep.foundations.behavioral.observer.good;

/**
 * Observer Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * When one object changes state, multiple other objects need to react — but you don't want
 * the source object to know who's listening. Without Observer, the source must call every
 * dependent directly (tight coupling). Adding a new dependent requires modifying the source.
 *
 * <p><b>How it works:</b><br>
 * - {@code Subject} (WeatherStation) holds an {@code EventManager} — it publishes events.<br>
 * - {@code Observer} classes (PhoneDisplay, TVDisplay, AlertSystem) subscribe to event types.<br>
 * - Subject notifies all subscribers when state changes. Subscribers can be added/removed at runtime.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>One event must trigger reactions in multiple independent components.</li>
 *   <li>You need loose coupling between producer and consumers.</li>
 *   <li>Observers can be added/removed at runtime (subscriptions).</li>
 * </ul>
 *
 * <p><b>Key gotchas:</b>
 * <ul>
 *   <li>Notification order is not guaranteed — don't write observers that depend on each other's order.</li>
 *   <li>Never let an observer modify the subject during notification — can cause infinite loops.</li>
 *   <li>Memory leaks: always unsubscribe observers when they are no longer needed (e.g., UI components).</li>
 * </ul>
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>Basic subscribe/notify/unsubscribe</li>
 *   <li>Multiple event types on one subject</li>
 *   <li>Runtime subscribe and unsubscribe</li>
 *   <li>Evolve: adding a new observer (AlertSystem) without touching WeatherStation</li>
 * </ol>
 */
public class ObserverDemo {

    public static void main(String[] args) {
        demo1_BasicObserver();
        demo2_MultipleEventTypes();
        demo3_UnsubscribeAtRuntime();
        demo4_EvolveAddNewObserver();
    }

    // -------------------------------------------------------------------------

    private static void demo1_BasicObserver() {
        section("Demo 1: Basic subscribe and notify");

        WeatherStation station = new WeatherStation();
        PhoneDisplay alice = new PhoneDisplay("Alice");
        TVDisplay tv = new TVDisplay();

        station.subscribe(WeatherStation.TEMPERATURE_CHANGED, alice);
        station.subscribe(WeatherStation.TEMPERATURE_CHANGED, tv);

        station.setTemperature(28.5);
        station.setTemperature(31.0);
    }

    private static void demo2_MultipleEventTypes() {
        section("Demo 2: Subscribe to different event types");

        WeatherStation station = new WeatherStation();
        PhoneDisplay bob = new PhoneDisplay("Bob");

        station.subscribe(WeatherStation.TEMPERATURE_CHANGED, bob);
        station.subscribe(WeatherStation.HUMIDITY_CHANGED, bob);

        station.setTemperature(35.0);  // Bob gets this
        station.setHumidity(80.0);      // Bob also gets this
    }

    private static void demo3_UnsubscribeAtRuntime() {
        section("Demo 3: Unsubscribe at runtime");

        WeatherStation station = new WeatherStation();
        PhoneDisplay alice = new PhoneDisplay("Alice");
        TVDisplay tv = new TVDisplay();

        station.subscribe(WeatherStation.TEMPERATURE_CHANGED, alice);
        station.subscribe(WeatherStation.TEMPERATURE_CHANGED, tv);

        station.setTemperature(22.0); // Both notified

        System.out.println("  [Alice unsubscribed]");
        station.unsubscribe(WeatherStation.TEMPERATURE_CHANGED, alice);

        station.setTemperature(18.0); // Only TV notified now
    }

    private static void demo4_EvolveAddNewObserver() {
        section("Demo 4: Evolve — add AlertSystem with zero changes to WeatherStation");

        WeatherStation station = new WeatherStation();
        station.subscribe(WeatherStation.TEMPERATURE_CHANGED, new PhoneDisplay("Carol"));
        station.subscribe(WeatherStation.TEMPERATURE_CHANGED, new AlertSystem(30.0)); // NEW — no edits needed

        station.setTemperature(25.0); // Normal — no alert
        station.setTemperature(33.5); // Exceeds threshold — alert fires
    }

    // -------------------------------------------------------------------------

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
