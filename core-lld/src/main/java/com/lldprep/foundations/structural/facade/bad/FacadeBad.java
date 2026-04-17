package com.lldprep.foundations.structural.facade.bad;

/**
 * BAD: Client orchestrates the entire subsystem directly.
 *
 * Problems:
 * 1. Client must know the correct startup/shutdown order of every subsystem component.
 * 2. Any change to the subsystem (e.g., new component, different startup order) requires
 *    editing every client that uses it.
 * 3. The client is tightly coupled to implementation details — not the intention.
 * 4. Startup/teardown logic is duplicated across every client.
 */
public class FacadeBad {

    public static void main(String[] args) {
        // Client must know and orchestrate every subsystem step in the right order
        Amplifier amp = new Amplifier();
        DVDPlayer dvd = new DVDPlayer();
        Projector projector = new Projector();
        Lights lights = new Lights();

        lights.dim(30);
        projector.on();
        projector.setInput("DVD");
        amp.on();
        amp.setVolume(8);
        dvd.on();
        dvd.play("Inception");

        System.out.println("  [Client] Watching movie...");

        // Shutdown — client must also know the teardown order
        dvd.stop();
        dvd.off();
        amp.off();
        projector.off();
        lights.dim(100);
    }
}

class Amplifier {
    public void on()  { System.out.println("  Amplifier: on"); }
    public void off() { System.out.println("  Amplifier: off"); }
    public void setVolume(int level) { System.out.println("  Amplifier: volume = " + level); }
}

class DVDPlayer {
    public void on()  { System.out.println("  DVDPlayer: on"); }
    public void off() { System.out.println("  DVDPlayer: off"); }
    public void play(String movie) { System.out.println("  DVDPlayer: playing '" + movie + "'"); }
    public void stop() { System.out.println("  DVDPlayer: stopped"); }
}

class Projector {
    public void on()  { System.out.println("  Projector: on"); }
    public void off() { System.out.println("  Projector: off"); }
    public void setInput(String source) { System.out.println("  Projector: input = " + source); }
}

class Lights {
    public void dim(int level) { System.out.println("  Lights: dimmed to " + level + "%"); }
}
