package com.lldprep.foundations.creational.prototype.good;

/**
 * Prototype Interface - declares cloning capability.
 */
public interface Prototype extends Cloneable {
    Prototype clone();
}
