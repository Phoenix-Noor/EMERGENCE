package com.emergence.core;

/**
 * Discrete simulation clock tracking the current tick and execution state.
 */
public final class SimulationClock {

    private long currentTick;

    public SimulationClock() {
        this.currentTick = 0L;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public void advanceTick() {
        currentTick++;
    }

    public void reset() {
        currentTick = 0L;
    }
}
