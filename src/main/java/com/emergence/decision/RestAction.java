package com.emergence.decision;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentState;
import com.emergence.core.SimulationContext;

import java.util.Objects;

/**
 * Allows an agent to rest in place and recover a small amount of health.
 */
public final class RestAction implements Action {

    private final Agent agent;

    public RestAction(Agent agent) {
        this.agent = Objects.requireNonNull(agent, "agent must not be null");
    }

    @Override
    public String getName() {
        return "REST";
    }

    @Override
    public boolean isValid(SimulationContext context) {
        return agent.isAlive();
    }

    @Override
    public void execute(SimulationContext context) {
        agent.setState(AgentState.RESTING);
        agent.adjustHealth(0.5);
    }
}
