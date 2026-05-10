package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Power Unit Provider - assigns the correct factory based on team's supplier contract.
 *
 * Centralizes the decision of which PU manufacturer a team uses.
 * In F1, this is decided before the season — each team signs a multi-year PU supply deal.
 */
public class PowerUnitProvider {

    public enum Manufacturer {
        MERCEDES, FERRARI, HONDA_RBPT
    }

    public static PowerUnitFactory getFactory(Manufacturer manufacturer) {
        return switch (manufacturer) {
            case MERCEDES   -> new MercedesFactory();
            case FERRARI    -> new FerrariFactory();
            case HONDA_RBPT -> new HondaRBPTFactory();
        };
    }

    /**
     * Lookup which PU manufacturer a team uses.
     * Real F1 2024-2025 supplier contracts.
     */
    public static PowerUnitFactory getFactoryForTeam(String teamName) {
        return switch (teamName.toLowerCase()) {
            case "mercedes", "mclaren", "williams", "aston martin" ->
                    getFactory(Manufacturer.MERCEDES);
            case "ferrari", "haas", "sauber", "kick sauber" ->
                    getFactory(Manufacturer.FERRARI);
            case "red bull", "rb", "alphatauri" ->
                    getFactory(Manufacturer.HONDA_RBPT);
            default -> throw new IllegalArgumentException("Unknown team: " + teamName);
        };
    }
}

/**
 * Concrete Factory - Honda RBPT (Red Bull Powertrains).
 * Supplies power units to: Red Bull Racing, RB (AlphaTauri).
 *
 * Easy to add — just implement PowerUnitFactory. Zero changes to existing code (OCP).
 */
class HondaRBPTFactory implements PowerUnitFactory {
    @Override
    public Engine createEngine() {
        return new HondaRBPTEngine();
    }
    @Override
    public ERS createERS() {
        return new HondaRBPTERS();
    }
    @Override
    public Gearbox createGearbox() {
        return new HondaRBPTGearbox();
    }
}

class HondaRBPTEngine implements Engine {
    @Override
    public void start() {
        System.out.println("  [Honda/RBPT ICE] RA621H 1.6L V6 turbo-hybrid starting... Max power mode!");
    }
    @Override
    public int getHorsepower() { return 1010; }
    @Override
    public String getSpecification() {
        return "Honda/RBPT RA621H | 1010hp | Best overall PU on grid | Bulletproof reliability";
    }
}

class HondaRBPTERS implements ERS {
    @Override
    public void deploy() {
        System.out.println("  [Honda/RBPT ERS] Deploying 160hp — optimized deployment curve for exit speed");
    }
    @Override
    public void harvest() {
        System.out.println("  [Honda/RBPT ERS] Harvesting energy — efficient regen on braking zones");
    }
    @Override
    public int getBoostHP() { return 160; }
    @Override
    public String getSpecification() {
        return "Honda/RBPT ERS | 160hp boost | Best energy management on grid";
    }
}

class HondaRBPTGearbox implements Gearbox {
    private int currentGear = 0;
    @Override
    public void shiftUp() {
        if (currentGear < 8) {
            currentGear++;
            System.out.println("  [Honda/RBPT Gearbox] Rapid shift UP → Gear " + currentGear);
        }
    }
    @Override
    public void shiftDown() {
        if (currentGear > 1) {
            currentGear--;
            System.out.println("  [Honda/RBPT Gearbox] Rapid shift DOWN → Gear " + currentGear);
        }
    }
    @Override
    public int getCurrentGear() { return currentGear; }
    @Override
    public String getSpecification() {
        return "Honda/RBPT 8-speed | Ultra-lightweight | Integrated with RB chassis";
    }
}
