package com.lldprep.foundations.structural.facade.good;

public class Projector {
    public void on()  { System.out.println("  Projector: on"); }
    public void off() { System.out.println("  Projector: off"); }
    public void setInput(String source) { System.out.println("  Projector: input = " + source); }
}
