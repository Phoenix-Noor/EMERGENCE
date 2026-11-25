package com.emergence.core;

/**
 * Explicit, validated configuration for a simulation run.
 * Defaults follow PRD Section 8.1 (100x100 world, 100 initial agents).
 */
public final class SimulationConfig {

    private int worldWidth = 100;
    private int worldHeight = 100;
    private int initialPopulation = 100;
    private int maxPopulation = 400;
    private long randomSeed = 42L;
    private boolean wrapEdges = false;

    // Resource & terrain parameters
    private double initialFoodCellDensity = 0.18;
    private double maxCellResource = 25.0;
    private double resourceRegenerationRate = 0.35;
    private double waterFraction = 0.08;
    private double forestFraction = 0.18;
    private double hazardFraction = 0.03;

    // Agent & ecosystem parameters
    private double initialAgentEnergy = 60.0;
    private double maxAgentEnergy = 100.0;
    private double initialAgentHealth = 100.0;
    private double maxAgentHealth = 100.0;
    private double baseMetabolismCost = 0.6;
    private double movementEnergyCost = 0.5;
    private double foodConsumptionAmount = 12.0;
    private double hazardDamage = 10.0;
    private int defaultPerceptionRadius = 5;

    // Reproduction parameters
    private int reproductionMinAge = 15;
    private double reproductionMinEnergy = 75.0;
    private double reproductionMinHealth = 70.0;
    private double reproductionEnergyCost = 35.0;
    private int reproductionCooldownTicks = 12;

    // Simulation speed
    private int ticksPerSecond = 10;

    // Game theory parameters
    private boolean gameTheoryEnabled = true;
    private int interactionRadius = 2;

    // Swarm intelligence parameters
    private boolean swarmEnabled = true;
    private boolean swarmResourceSharingEnabled = true;
    private int separationRadius = 2;
    private int groupFormationRadius = 6;
    private double separationWeight = 1.4;
    private double alignmentWeight = 1.0;
    private double cohesionWeight = 1.1;

    public SimulationConfig() {
    }

    public void validate() {
        if (worldWidth <= 0 || worldHeight <= 0) {
            throw new IllegalArgumentException("World dimensions must be positive");
        }
        if (initialPopulation < 0) {
            throw new IllegalArgumentException("Initial population cannot be negative");
        }
        if (maxPopulation < initialPopulation) {
            throw new IllegalArgumentException("Max population must be >= initial population");
        }
        if (maxCellResource <= 0) {
            throw new IllegalArgumentException("maxCellResource must be positive");
        }
        if (resourceRegenerationRate < 0) {
            throw new IllegalArgumentException("resourceRegenerationRate cannot be negative");
        }
        if (maxAgentEnergy <= 0 || maxAgentHealth <= 0) {
            throw new IllegalArgumentException("Max energy and health must be positive");
        }
        if (ticksPerSecond <= 0) {
            throw new IllegalArgumentException("ticksPerSecond must be positive");
        }
    }

    public int getWorldWidth() { return worldWidth; }
    public void setWorldWidth(int worldWidth) { this.worldWidth = worldWidth; }

    public int getWorldHeight() { return worldHeight; }
    public void setWorldHeight(int worldHeight) { this.worldHeight = worldHeight; }

    public int getInitialPopulation() { return initialPopulation; }
    public void setInitialPopulation(int initialPopulation) { this.initialPopulation = initialPopulation; }

    public int getMaxPopulation() { return maxPopulation; }
    public void setMaxPopulation(int maxPopulation) { this.maxPopulation = maxPopulation; }

    public long getRandomSeed() { return randomSeed; }
    public void setRandomSeed(long randomSeed) { this.randomSeed = randomSeed; }

    public boolean isWrapEdges() { return wrapEdges; }
    public void setWrapEdges(boolean wrapEdges) { this.wrapEdges = wrapEdges; }

    public double getInitialFoodCellDensity() { return initialFoodCellDensity; }
    public void setInitialFoodCellDensity(double initialFoodCellDensity) { this.initialFoodCellDensity = initialFoodCellDensity; }

    public double getMaxCellResource() { return maxCellResource; }
    public void setMaxCellResource(double maxCellResource) { this.maxCellResource = maxCellResource; }

    public double getResourceRegenerationRate() { return resourceRegenerationRate; }
    public void setResourceRegenerationRate(double resourceRegenerationRate) { this.resourceRegenerationRate = resourceRegenerationRate; }

    public double getWaterFraction() { return waterFraction; }
    public void setWaterFraction(double waterFraction) { this.waterFraction = waterFraction; }

    public double getForestFraction() { return forestFraction; }
    public void setForestFraction(double forestFraction) { this.forestFraction = forestFraction; }

    public double getHazardFraction() { return hazardFraction; }
    public void setHazardFraction(double hazardFraction) { this.hazardFraction = hazardFraction; }

    public double getInitialAgentEnergy() { return initialAgentEnergy; }
    public void setInitialAgentEnergy(double initialAgentEnergy) { this.initialAgentEnergy = initialAgentEnergy; }

    public double getMaxAgentEnergy() { return maxAgentEnergy; }
    public void setMaxAgentEnergy(double maxAgentEnergy) { this.maxAgentEnergy = maxAgentEnergy; }

    public double getInitialAgentHealth() { return initialAgentHealth; }
    public void setInitialAgentHealth(double initialAgentHealth) { this.initialAgentHealth = initialAgentHealth; }

    public double getMaxAgentHealth() { return maxAgentHealth; }
    public void setMaxAgentHealth(double maxAgentHealth) { this.maxAgentHealth = maxAgentHealth; }

    public double getBaseMetabolismCost() { return baseMetabolismCost; }
    public void setBaseMetabolismCost(double baseMetabolismCost) { this.baseMetabolismCost = baseMetabolismCost; }

    public double getMovementEnergyCost() { return movementEnergyCost; }
    public void setMovementEnergyCost(double movementEnergyCost) { this.movementEnergyCost = movementEnergyCost; }

    public double getFoodConsumptionAmount() { return foodConsumptionAmount; }
    public void setFoodConsumptionAmount(double foodConsumptionAmount) { this.foodConsumptionAmount = foodConsumptionAmount; }

    public double getHazardDamage() { return hazardDamage; }
    public void setHazardDamage(double hazardDamage) { this.hazardDamage = hazardDamage; }

    public int getDefaultPerceptionRadius() { return defaultPerceptionRadius; }
    public void setDefaultPerceptionRadius(int defaultPerceptionRadius) { this.defaultPerceptionRadius = defaultPerceptionRadius; }

    public int getReproductionMinAge() { return reproductionMinAge; }
    public void setReproductionMinAge(int reproductionMinAge) { this.reproductionMinAge = reproductionMinAge; }

    public double getReproductionMinEnergy() { return reproductionMinEnergy; }
    public void setReproductionMinEnergy(double reproductionMinEnergy) { this.reproductionMinEnergy = reproductionMinEnergy; }

    public double getReproductionMinHealth() { return reproductionMinHealth; }
    public void setReproductionMinHealth(double reproductionMinHealth) { this.reproductionMinHealth = reproductionMinHealth; }

    public double getReproductionEnergyCost() { return reproductionEnergyCost; }
    public void setReproductionEnergyCost(double reproductionEnergyCost) { this.reproductionEnergyCost = reproductionEnergyCost; }

    public int getReproductionCooldownTicks() { return reproductionCooldownTicks; }
    public void setReproductionCooldownTicks(int reproductionCooldownTicks) { this.reproductionCooldownTicks = reproductionCooldownTicks; }

    public int getTicksPerSecond() { return ticksPerSecond; }
    public void setTicksPerSecond(int ticksPerSecond) { this.ticksPerSecond = ticksPerSecond; }

    public boolean isGameTheoryEnabled() { return gameTheoryEnabled; }
    public void setGameTheoryEnabled(boolean gameTheoryEnabled) { this.gameTheoryEnabled = gameTheoryEnabled; }

    public int getInteractionRadius() { return interactionRadius; }
    public void setInteractionRadius(int interactionRadius) { this.interactionRadius = interactionRadius; }

    public boolean isSwarmEnabled() { return swarmEnabled; }
    public void setSwarmEnabled(boolean swarmEnabled) { this.swarmEnabled = swarmEnabled; }

    public boolean isSwarmResourceSharingEnabled() { return swarmResourceSharingEnabled; }
    public void setSwarmResourceSharingEnabled(boolean swarmResourceSharingEnabled) { this.swarmResourceSharingEnabled = swarmResourceSharingEnabled; }

    public int getSeparationRadius() { return separationRadius; }
    public void setSeparationRadius(int separationRadius) { this.separationRadius = separationRadius; }

    public int getGroupFormationRadius() { return groupFormationRadius; }
    public void setGroupFormationRadius(int groupFormationRadius) { this.groupFormationRadius = groupFormationRadius; }

    public double getSeparationWeight() { return separationWeight; }
    public void setSeparationWeight(double separationWeight) { this.separationWeight = separationWeight; }

    public double getAlignmentWeight() { return alignmentWeight; }
    public void setAlignmentWeight(double alignmentWeight) { this.alignmentWeight = alignmentWeight; }

    public double getCohesionWeight() { return cohesionWeight; }
    public void setCohesionWeight(double cohesionWeight) { this.cohesionWeight = cohesionWeight; }
}
