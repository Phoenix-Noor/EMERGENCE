package com.emergence.agent;

import com.emergence.core.DeterministicRandom;

/**
 * Immutable behavioural traits for an agent, each bounded within [0.0, 1.0].
 */
public record AgentTraits(
        double aggression,
        double cooperation,
        double sociability,
        double exploration,
        double riskTolerance,
        double trustPropensity
) {
    public AgentTraits {
        aggression = clamp(aggression);
        cooperation = clamp(cooperation);
        sociability = clamp(sociability);
        exploration = clamp(exploration);
        riskTolerance = clamp(riskTolerance);
        trustPropensity = clamp(trustPropensity);
    }

    public static AgentTraits random(DeterministicRandom random) {
        return new AgentTraits(
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble()
        );
    }

    public AgentTraits mutate(DeterministicRandom random, double mutationRate, double mutationStrength) {
        return new AgentTraits(
                mutateTrait(aggression, random, mutationRate, mutationStrength),
                mutateTrait(cooperation, random, mutationRate, mutationStrength),
                mutateTrait(sociability, random, mutationRate, mutationStrength),
                mutateTrait(exploration, random, mutationRate, mutationStrength),
                mutateTrait(riskTolerance, random, mutationRate, mutationStrength),
                mutateTrait(trustPropensity, random, mutationRate, mutationStrength)
        );
    }

    private static double mutateTrait(double value, DeterministicRandom random, double rate, double strength) {
        if (random.nextDouble() < rate) {
            double delta = (random.nextDouble() * 2.0 - 1.0) * strength;
            return clamp(value + delta);
        }
        return value;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
