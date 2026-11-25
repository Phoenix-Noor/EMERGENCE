package com.emergence.decision;

import com.emergence.agent.Agent;
import com.emergence.agent.Perception;
import com.emergence.core.SimulationContext;

/**
 * Strategy contract for selecting an agent action given local perception.
 */
public interface AgentStrategy {

    String getStrategyName();

    Action chooseAction(Agent agent, Perception perception, SimulationContext context);
}
