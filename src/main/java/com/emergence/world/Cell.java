package com.emergence.world;

import java.util.Objects;

/**
 * Single grid cell in the simulation world containing terrain and renewable food resources.
 * Invariant: 0.0 <= resourceAmount <= maxResource.
 */
public final class Cell {

    private final Position position;
    private Terrain terrain;
    private double resourceAmount;
    private double maxResource;
    private double regenerationRate;

    public Cell(Position position, Terrain terrain, double initialResource, double maxResource, double regenerationRate) {
        this.position = Objects.requireNonNull(position, "position must not be null");
        this.terrain = Objects.requireNonNull(terrain, "terrain must not be null");
        if (maxResource < 0) {
            throw new IllegalArgumentException("maxResource cannot be negative");
        }
        if (regenerationRate < 0) {
            throw new IllegalArgumentException("regenerationRate cannot be negative");
        }
        this.maxResource = maxResource;
        this.regenerationRate = regenerationRate;
        this.resourceAmount = clampResource(initialResource);
    }

    public Position getPosition() {
        return position;
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = Objects.requireNonNull(terrain, "terrain must not be null");
        if (!terrain.isPassableByDefault()) {
            this.resourceAmount = 0.0;
        }
    }

    public double getResourceAmount() {
        return resourceAmount;
    }

    public double getMaxResource() {
        return maxResource;
    }

    public double getRegenerationRate() {
        return regenerationRate;
    }

    /**
     * Consumes up to requestedAmount from this cell and returns the actual amount consumed.
     */
    public double consume(double requestedAmount) {
        if (requestedAmount <= 0 || resourceAmount <= 0) {
            return 0.0;
        }
        double consumed = Math.min(resourceAmount, requestedAmount);
        resourceAmount = clampResource(resourceAmount - consumed);
        return consumed;
    }

    /**
     * Regenerates resources according to R(t+1) = min(Rmax, R(t) + G(t)).
     */
    public void regenerate() {
        if (regenerationRate <= 0 || !terrain.isPassableByDefault() || terrain == Terrain.HAZARD) {
            return;
        }
        resourceAmount = clampResource(resourceAmount + regenerationRate);
    }

    public void setResourceAmount(double amount) {
        this.resourceAmount = clampResource(amount);
    }

    private double clampResource(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        return Math.min(value, maxResource);
    }
}
