package com.lldprep.foundations.structural.proxy.good;

/**
 * Subject interface — both the real object and the proxy implement this.
 * The client depends only on this interface and cannot tell which it is talking to.
 */
public interface Image {
    void display();
    String getFilename();
}
