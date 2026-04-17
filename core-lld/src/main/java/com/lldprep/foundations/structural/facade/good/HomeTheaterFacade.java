package com.lldprep.foundations.structural.facade.good;

/**
 * Facade — single entry point that hides the startup/shutdown complexity of the subsystem.
 *
 * <p>The client calls {@code watchMovie()} or {@code endMovie()} and is completely
 * shielded from the internal orchestration order. If a new component is added
 * (e.g., SurroundSoundSystem), only this class changes — no client changes needed.
 */
public class HomeTheaterFacade {

    private final Amplifier amplifier;
    private final DVDPlayer dvdPlayer;
    private final Projector projector;
    private final Lights lights;

    public HomeTheaterFacade(Amplifier amplifier, DVDPlayer dvdPlayer,
                             Projector projector, Lights lights) {
        this.amplifier = amplifier;
        this.dvdPlayer = dvdPlayer;
        this.projector = projector;
        this.lights    = lights;
    }

    public void watchMovie(String movie) {
        System.out.println("  [Facade] Getting ready to watch: " + movie);
        lights.dim(30);
        projector.on();
        projector.setInput("DVD");
        amplifier.on();
        amplifier.setVolume(8);
        dvdPlayer.on();
        dvdPlayer.play(movie);
    }

    public void endMovie() {
        System.out.println("  [Facade] Shutting down home theater...");
        dvdPlayer.stop();
        dvdPlayer.off();
        amplifier.off();
        projector.off();
        lights.dim(100);
    }
}
