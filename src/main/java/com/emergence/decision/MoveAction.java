package com.emergence.decision;

import com.emergence.agent.Agent;
import com.emergence.core.SimulationContext;
import com.emergence.world.Cell;
import com.emergence.world.Position;
import com.emergence.world.World;

import java.util.Objects;

/**
 * Moves an agent to an adjacent passable cell, deducting terrain-weighted movement energy.
 */
public final class MoveAction implements Action {

    private final Agent agent;
    private final Position targetPosition;

    public MoveAction(Agent agent, Position targetPosition) {
        this.agent = Objects.requireNonNull(agent, "agent must not be null");
        this.targetPosition = Objects.requireNonNull(targetPosition, "targetPosition must not be null");
    }

    public Position getTargetPosition() {
        return targetPosition;
    }

    @Override
    public String getName() {
        return "MOVE";
    }

    @Override
    public boolean isValid(SimulationContext context) {
        if (!agent.isAlive()) {
            return false;
        }
        World world = context.getWorld();
        if (!world.isPassable(targetPosition)) {
            return false;
        }
        Position normalized = world.normalizePosition(targetPosition);
        Cell cell = world.getCell(normalized);
        double cost = context.getConfig().getMovementEnergyCost() * cell.getTerrain().getMovementCostMultiplier();
        return agent.getEnergy() > cost;
    }

    @Override
    public void execute(SimulationContext context) {
        World world = context.getWorld();
        Position normalized = world.normalizePosition(targetPosition);
        Cell targetCell = world.getCell(normalized);
        double cost = context.getConfig().getMovementEnergyCost() * targetCell.getTerrain().getMovementCostMultiplier();
        int dx = Integer.compare(normalized.x(), agent.getPosition().x());
        int dy = Integer.compare(normalized.y(), agent.getPosition().y());
        agent.setPosition(normalized);
        agent.setLastMove(dx, dy);
        agent.adjustEnergy(-cost);
    }
}
