package com.lldprep.foundations.creational.builder.good;

/**
 * Builder with Validation - ensures valid object state.
 * 
 * Useful when certain combinations are invalid.
 */
public class Computer {
    private final String cpu;
    private final String ram;
    private final String storage;
    private final String gpu;
    private final String powerSupply;
    private final boolean liquidCooling;
    
    private Computer(Builder builder) {
        this.cpu = builder.cpu;
        this.ram = builder.ram;
        this.storage = builder.storage;
        this.gpu = builder.gpu;
        this.powerSupply = builder.powerSupply;
        this.liquidCooling = builder.liquidCooling;
    }
    
    @Override
    public String toString() {
        return "Computer [CPU=" + cpu + ", RAM=" + ram + ", Storage=" + storage 
            + ", GPU=" + gpu + ", PSU=" + powerSupply + ", Liquid Cooling=" + liquidCooling + "]";
    }
    
    public static class Builder {
        // Required
        private String cpu;
        private String ram;
        
        // Optional
        private String storage = "512GB SSD";
        private String gpu = "Integrated";
        private String powerSupply = "450W";
        private boolean liquidCooling = false;
        
        public Builder(String cpu, String ram) {
            this.cpu = cpu;
            this.ram = ram;
        }
        
        public Builder storage(String storage) {
            this.storage = storage;
            return this;
        }
        
        public Builder gpu(String gpu) {
            this.gpu = gpu;
            return this;
        }
        
        public Builder powerSupply(String psu) {
            this.powerSupply = psu;
            return this;
        }
        
        public Builder liquidCooling(boolean enabled) {
            this.liquidCooling = enabled;
            return this;
        }
        
        /**
         * Validation before building.
         * Ensures the computer configuration is valid.
         */
        public Computer build() {
            validate();
            return new Computer(this);
        }
        
        private void validate() {
            if (cpu == null || cpu.isEmpty()) {
                throw new IllegalStateException("CPU is required");
            }
            if (ram == null || ram.isEmpty()) {
                throw new IllegalStateException("RAM is required");
            }
            // Business rule: High-end GPU needs better PSU
            if (!gpu.equals("Integrated") && powerSupply.compareTo("750W") < 0) {
                throw new IllegalStateException(
                    "GPU " + gpu + " requires at least 750W PSU, but got " + powerSupply);
            }
            // Business rule: Liquid cooling recommended for high-end setups
            if (liquidCooling && !powerSupply.equals("1000W")) {
                System.out.println("[WARNING] Liquid cooling with " + powerSupply + " might be insufficient");
            }
        }
    }
}
