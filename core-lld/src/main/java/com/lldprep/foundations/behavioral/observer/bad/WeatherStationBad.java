package com.lldprep.foundations.behavioral.observer.bad;

/**
 * BAD: WeatherStation directly calls every dependent object.
 *
 * Problems:
 * 1. Tight coupling — WeatherStation must know about PhoneDisplay, TVDisplay, Logger.
 * 2. OCP violation — adding a new display requires editing WeatherStation.
 * 3. SRP violation — the station manages measurements AND manages who to notify.
 * 4. Cannot add/remove observers at runtime.
 */
public class WeatherStationBad {

    private double temperature;
    private PhoneDisplayBad phoneDisplay = new PhoneDisplayBad();
    private TVDisplayBad tvDisplay = new TVDisplayBad();
    private LoggerBad logger = new LoggerBad();

    public void setTemperature(double temperature) {
        this.temperature = temperature;
        // Must manually notify each dependent — rigid and non-extensible
        phoneDisplay.update(temperature);
        tvDisplay.update(temperature);
        logger.log(temperature);
        // Adding a new display → edit this method. OCP violation.
    }

    // --- Tightly coupled dependents defined inline for brevity ---

    static class PhoneDisplayBad {
        void update(double temp) { System.out.println("[PhoneDisplay] Temp: " + temp); }
    }

    static class TVDisplayBad {
        void update(double temp) { System.out.println("[TVDisplay] Temp: " + temp); }
    }

    static class LoggerBad {
        void log(double temp) { System.out.println("[Logger] Temp logged: " + temp); }
    }
}
