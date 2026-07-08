package com.lldprep.foundations.creational.builder.myprac;

class F1Car {
    private final String engineName;
    private final String tyres;
    private final String name;

    private F1Car(F1CarBuilder f1CarBuilder) {
        this.engineName = f1CarBuilder.engine;
        this.tyres = f1CarBuilder.tyres;
        this.name = f1CarBuilder.name;
    }

    public String getEngineName() {
        return engineName;
    }

    public String getTyres() {
        return tyres;
    }

    public String getName() {
        return name;
    }

    public static class F1CarBuilder {
        private String engine;
        private String tyres;
        private String name;

        public F1CarBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public F1CarBuilder withEngine(String engine) {
            this.engine = engine;
            return this;

        }

        public F1CarBuilder withTyres(String tyres) {
            this.tyres = tyres;
            return this;
        }

        public F1Car build() {
            return new F1Car(this);
        }
    }

}

public class BuilderMyPrac {

    public static void main(String[] args) {
        System.out.println("===== BUILDER: MY PRACTICE =====\n");
        F1Car redBull = new F1Car.F1CarBuilder()
            .withName("Red Bull")
            .withEngine("V8")
            .withTyres("Pirelli Soft")
            .build();

        System.out.println("Printing red bull details");
        System.out.println(redBull.getEngineName() + " " + redBull.getTyres() + " " + redBull.getName());

    }
}
