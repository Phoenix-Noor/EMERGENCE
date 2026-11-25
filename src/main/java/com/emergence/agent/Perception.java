package com.emergence.agent;

import com.emergence.world.Cell;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable local observation snapshot for an agent during a single tick.
 */
public final class Perception {

    private final Cell currentCell;
    private final List<Cell> visibleCells;
    private final List<Agent> nearbyAgents;

    public Perception(Cell currentCell, List<Cell> visibleCells, List<Agent> nearbyAgents) {
        this.currentCell = Objects.requireNonNull(currentCell, "currentCell must not be null");
        this.visibleCells = List.copyOf(visibleCells);
        this.nearbyAgents = List.copyOf(nearbyAgents);
    }

    public Cell getCurrentCell() {
        return currentCell;
    }

    public List<Cell> getVisibleCells() {
        return Collections.unmodifiableList(visibleCells);
    }

    public List<Agent> getNearbyAgents() {
        return Collections.unmodifiableList(nearbyAgents);
    }
}
