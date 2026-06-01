package com.lldprep.systems.moviebooking.repository;

import com.lldprep.systems.moviebooking.model.Screen;
import com.lldprep.systems.moviebooking.model.Show;
import com.lldprep.systems.moviebooking.model.Theater;
import com.lldprep.systems.moviebooking.model.enums.City;

import java.time.LocalDate;
import java.util.*;

public class ShowRepository {
    private final Map<String, Show> showMap;
    private final List<Show> allShows;
    private final Map<String, Screen> screenMap;
    private final Map<String, Theater> theaterMap;

    public ShowRepository() {
        this.showMap = new LinkedHashMap<>();
        this.allShows = new ArrayList<>();
        this.screenMap = new HashMap<>();
        this.theaterMap = new HashMap<>();
    }

    public void registerTheater(Theater theater) {
        theaterMap.put(theater.getId(), theater);
        for (Screen screen : theater.getScreens()) {
            screenMap.put(screen.getId(), screen);
        }
    }

    public void addShow(Show show) {
        showMap.put(show.getId(), show);
        allShows.add(show);
    }

    public Show getById(String showId) {
        return showMap.get(showId);
    }

    public Theater getTheaterForShow(String showId) {
        Show show = showMap.get(showId);
        if (show == null) {
            return null;
        }
        Screen screen = screenMap.get(show.getScreen().getId());
        return theaterMap.values().stream()
            .filter(t -> t.getScreens().contains(screen))
            .findFirst()
            .orElse(null);
    }

    public List<Show> searchShows(City city, String movieName, LocalDate date) {
        List<Show> results = new ArrayList<>();
        String lowerMovie = movieName != null ? movieName.toLowerCase() : null;

        for (Show show : allShows) {
            if (!show.getShowTime().toLocalDate().equals(date)) {
                continue;
            }

            Screen screen = screenMap.get(show.getScreen().getId());
            if (screen == null) {
                continue;
            }

            Theater theater = findTheaterFor(screen);
            if (theater == null || theater.getCity() != city) {
                continue;
            }

            if (lowerMovie != null && !show.getMovieName().toLowerCase().contains(lowerMovie)) {
                continue;
            }

            results.add(show);
        }

        results.sort(Comparator.comparing(Show::getShowTime));
        return results;
    }

    private Theater findTheaterFor(Screen screen) {
        for (Theater t : theaterMap.values()) {
            if (t.getScreens().contains(screen)) {
                return t;
            }
        }
        return null;
    }
}
