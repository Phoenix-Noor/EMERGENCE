package com.emergence.decision;

import com.emergence.agent.Agent;
import com.emergence.core.SimulationContext;
import com.emergence.world.Cell;

import java.util.Objects;

/**
 * Consumes available food resource from the agent's current cell and restores energy.
 */
public final class ConsumeResourceAction implements Action {

    private final Agent agent;

    public ConsumeResourceAction(Agent agent) {
        this.agent = Objects.requireNonNull(agent, "agent must not be null");
    }

    @Override
    public String getName() {
        return "CONSUME";
    }

    @Override
    public boolean isValid(SimulationContext context) {
        if (!agent.isAlive()) {
            return false;
        }
        Cell cell = context.getWorld().getCell(agent.getPosition());
        return cell.getResourceAmount() > 0.0 && agent.getEnergy() < agent.getMaxEnergy();
    }

    @Override
    public void execute(SimulationContext context) {
        Cell cell = context.getWorld().getCell(agent.getPosition());
        double requested = Math.min(
                context.getConfig().getFoodConsumptionAmount(),
                agent.getMaxEnergy() - agent.getEnergy()
        );
        double consumed = cell.consume(requested);
        agent.adjustEnergy(consumed);
        agent.adjustHealth(consumed * 0.25);
        if (cell.getResourceAmount() <= 0.0) {
            agent.getMemory().forgetResource(cell.getPosition());
        } else {
            agent.getMemory().rememberResource(cell.getPosition());
        }
    }
}
