package com.emergence.decision;

import com.emergence.core.SimulationContext;

/**
 * Represents an executable agent action with explicit validation and execution.
 */
public interface Action {

    String getName();

    boolean isValid(SimulationContext context);

    void execute(SimulationContext context);
}
