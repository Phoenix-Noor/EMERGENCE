package com.emergence.world;

/**
 * Configurable terrain types in the 2D simulation grid.
 */
public enum Terrain {
    GRASSLAND(true, 1.0, 0.0),
    FOREST(true, 1.4, 0.0),
    WATER(false, 2.0, 0.0),
    HAZARD(true, 1.2, 1.0);

    private final boolean passableByDefault;
    private final double movementCostMultiplier;
    private final double hazardSeverity;

    Terrain(boolean passableByDefault, double movementCostMultiplier, double hazardSeverity) {
        this.passableByDefault = passableByDefault;
        this.movementCostMultiplier = movementCostMultiplier;
        this.hazardSeverity = hazardSeverity;
    }

    public boolean isPassableByDefault() {
        return passableByDefault;
    }

    public double getMovementCostMultiplier() {
        return movementCostMultiplier;
    }

    public double getHazardSeverity() {
        return hazardSeverity;
    }
}
