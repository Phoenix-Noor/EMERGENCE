package com.emergence.ecosystem;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.core.DeterministicRandom;
import com.emergence.core.SimulationConfig;
import com.emergence.game.GameStrategies;
import com.emergence.game.StrategyType;

/**
 * Handles genetic trait inheritance, bounded mutation, and optional strategy inheritance during reproduction.
 */
public final class ReproductionEngine {

    private static final double STRATEGY_MUTATION_PROBABILITY = 0.08;

    public AgentTraits inheritTraits(Agent parent, SimulationConfig config, DeterministicRandom random) {
        if (!config.isEvolutionEnabled()) {
            return parent.getTraits();
        }
        return parent.getTraits().mutate(random, config.getMutationRate(), config.getMutationStrength());
    }

    public StrategyType inheritStrategy(
            Agent parent,
            AgentTraits childTraits,
            SimulationConfig config,
            DeterministicRandom random
    ) {
        if (!config.isStrategyInheritanceEnabled()) {
            return GameStrategies.assignFromTraits(childTraits, random);
        }
        if (config.isEvolutionEnabled() && random.nextDouble() < STRATEGY_MUTATION_PROBABILITY) {
            StrategyType[] all = StrategyType.values();
            return all[random.nextInt(all.length)];
        }
        return parent.getStrategyType();
    }
}
