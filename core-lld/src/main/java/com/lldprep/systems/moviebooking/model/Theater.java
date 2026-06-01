package com.lldprep.systems.moviebooking.model;

import com.lldprep.systems.moviebooking.model.enums.City;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Theater {
    private final String id;
    private final String name;
    private final City city;
    private final List<Screen> screens;

    public Theater(String id, String name, City city) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.screens = new ArrayList<>();
    }

    public void addScreen(Screen screen) {
        screens.add(screen);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public City getCity() {
        return city;
    }

    public List<Screen> getScreens() {
        return Collections.unmodifiableList(screens);
    }

    @Override
    public String toString() {
        return name + " (" + city + ")";
    }
}
