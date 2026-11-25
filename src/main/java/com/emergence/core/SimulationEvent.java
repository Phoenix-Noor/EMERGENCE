package com.emergence.core;

import java.util.Map;

/**
 * Immutable event emitted by simulation subsystems for logging, analytics, and UI observation.
 */
public record SimulationEvent(
        long tick,
        String category,
        String message,
        Map<String, Object> details
) {
    public SimulationEvent(long tick, String category, String message) {
        this(tick, category, message, Map.of());
    }
}
