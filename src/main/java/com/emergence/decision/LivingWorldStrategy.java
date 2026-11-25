package com.emergence.decision;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentState;
import com.emergence.agent.Perception;
import com.emergence.core.DeterministicRandom;
import com.emergence.core.SimulationContext;
import com.emergence.world.Cell;
import com.emergence.world.Position;
import com.emergence.world.Terrain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Phase 1 autonomous foraging, survival, and reproduction strategy.
 */
public final class LivingWorldStrategy implements AgentStrategy {

    private static final int[][] CARDINAL_AND_DIAGONAL = {
            {0, -1}, {1, 0}, {0, 1}, {-1, 0},
            {1, -1}, {1, 1}, {-1, 1}, {-1, -1}
    };

    @Override
    public String getStrategyName() {
        return "LivingWorldForager";
    }

    @Override
    public Action chooseAction(Agent agent, Perception perception, SimulationContext context) {
        agent.setState(AgentState.ACTIVE);

        // 1. Reproduce if eligible and healthy
        ReproduceAction reproduceAction = new ReproduceAction(agent);
        if (reproduceAction.isValid(context)) {
            return reproduceAction;
        }

        // 2. Consume food on current cell if hungry and food is present
        Cell currentCell = perception.getCurrentCell();
        if (currentCell.getResourceAmount() > 0.5 && agent.getEnergy() < agent.getMaxEnergy() * 0.92) {
            return new ConsumeResourceAction(agent);
        }

        // 3. Move towards best visible food cell
        Cell bestFoodCell = findBestFoodCell(agent, perception);
        if (bestFoodCell != null) {
            Position nextStep = stepTowards(agent.getPosition(), bestFoodCell.getPosition(), context);
            if (nextStep != null) {
                return new MoveAction(agent, nextStep);
            }
        }

        // 4. Explore a safe passable adjacent cell
        Position exploreStep = pickExplorationStep(agent, context);
        if (exploreStep != null) {
            return new MoveAction(agent, exploreStep);
        }

        // 5. Fallback to rest
        return new RestAction(agent);
    }

    private Cell findBestFoodCell(Agent agent, Perception perception) {
        Position origin = agent.getPosition();
        return perception.getVisibleCells().stream()
                .filter(c -> c.getResourceAmount() > 0.5)
                .filter(c -> c.getTerrain() != Terrain.HAZARD)
                .max(Comparator.comparingDouble(c -> {
                    double dist = Math.max(1.0, origin.manhattanDistanceTo(c.getPosition()));
                    return c.getResourceAmount() / dist;
                }))
                .orElse(null);
    }

    private Position stepTowards(Position from, Position target, SimulationContext context) {
        int dx = Integer.compare(target.x(), from.x());
        int dy = Integer.compare(target.y(), from.y());
        Position direct = from.translate(dx, dy);
        if (isSafePassable(direct, context)) {
            return direct;
        }
        Position horiz = from.translate(dx, 0);
        if (dx != 0 && isSafePassable(horiz, context)) {
            return horiz;
        }
        Position vert = from.translate(0, dy);
        if (dy != 0 && isSafePassable(vert, context)) {
            return vert;
        }
        return null;
    }

    private Position pickExplorationStep(Agent agent, SimulationContext context) {
        DeterministicRandom random = context.getRandom();
        List<Position> safeMoves = new ArrayList<>();
        for (int[] dir : CARDINAL_AND_DIAGONAL) {
            Position candidate = agent.getPosition().translate(dir[0], dir[1]);
            if (isSafePassable(candidate, context)) {
                safeMoves.add(candidate);
            }
        }
        if (safeMoves.isEmpty()) {
            return null;
        }
        return safeMoves.get(random.nextInt(safeMoves.size()));
    }

    private boolean isSafePassable(Position pos, SimulationContext context) {
        if (!context.getWorld().isPassable(pos)) {
            return false;
        }
        Position norm = context.getWorld().normalizePosition(pos);
        return context.getWorld().getCell(norm).getTerrain() != Terrain.HAZARD;
    }
}
