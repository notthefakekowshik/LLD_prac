package com.lldprep.foundations.structural.facade.good;

public class Amplifier {
    public void on()  { System.out.println("  Amplifier: on"); }
    public void off() { System.out.println("  Amplifier: off"); }
    public void setVolume(int level) { System.out.println("  Amplifier: volume = " + level); }
}
