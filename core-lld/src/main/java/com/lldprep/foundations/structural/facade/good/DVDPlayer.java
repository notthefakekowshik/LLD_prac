package com.lldprep.foundations.structural.facade.good;

public class DVDPlayer {
    public void on()  { System.out.println("  DVDPlayer: on"); }
    public void off() { System.out.println("  DVDPlayer: off"); }
    public void play(String movie) { System.out.println("  DVDPlayer: playing '" + movie + "'"); }
    public void stop() { System.out.println("  DVDPlayer: stopped"); }
}
