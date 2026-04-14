package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Abstract Factory - creates families of related products.
 * 
 * GUARANTEES:
 * - All products from same family (no mixing Windows button with Mac checkbox)
 * - Platform consistency across application
 */
public interface GUIFactory {
    Button createButton();
    Checkbox createCheckbox();
    TextField createTextField();
}
