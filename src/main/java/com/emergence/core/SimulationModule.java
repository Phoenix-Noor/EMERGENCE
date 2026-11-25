package com.emergence.core;

/**
 * Contract for pluggable simulation subsystems.
 */
public interface SimulationModule {

    void initialize(SimulationContext context);

    void update(SimulationContext context);

    default void shutdown() {
        // Optional cleanup
    }
}
