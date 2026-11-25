package com.emergence.social;

import com.emergence.agent.Agent;
import com.emergence.core.DeterministicRandom;
import com.emergence.core.SimulationContext;
import com.emergence.core.SimulationModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Supports social learning (strategy imitation) and abstract signal contagion across social links.
 */
public final class SocialLearningEngine implements SimulationModule {

    public enum SocialLearningMode {
        NONE,
        RANDOM_IMITATION,
        SUCCESS_BASED,
        TRUST_WEIGHTED
    }

    private final SocialNetwork socialNetwork;
    private SocialLearningMode mode = SocialLearningMode.TRUST_WEIGHTED;
    private long strategyAdoptions = 0L;
    private long contagionTransmissions = 0L;

    public SocialLearningEngine(SocialNetwork socialNetwork) {
        this.socialNetwork = Objects.requireNonNull(socialNetwork, "socialNetwork must not be null");
    }

    @Override
    public void initialize(SimulationContext context) {
        strategyAdoptions = 0L;
        contagionTransmissions = 0L;
    }

    @Override
    public void update(SimulationContext context) {
        if (mode == SocialLearningMode.NONE) {
            return;
        }

        Map<Long, Agent> aliveById = new HashMap<>();
        for (Agent a : context.getAgents()) {
            if (a.isAlive()) {
                aliveById.put(a.getId(), a);
            }
        }

        DeterministicRandom random = context.getRandom();
        for (Agent learner : aliveById.values()) {
            // Social learning: consider imitating a connected peer when energy is below 65%
            if (learner.getEnergy() < learner.getMaxEnergy() * 0.65) {
                Agent mentor = selectMentor(learner, aliveById, random);
                if (mentor != null && mentor.getStrategyType() != learner.getStrategyType()) {
                    if (random.nextDouble() < 0.20) {
                        learner.setStrategyType(mentor.getStrategyType());
                        strategyAdoptions++;
                    }
                }
            }

            // Abstract signal contagion across high-trust edges
            if (learner.isSignalActive()) {
                for (Relationship rel : socialNetwork.getOutgoingRelationships(learner.getId())) {
                    if (rel.getTrust() >= 0.60) {
                        Agent peer = aliveById.get(rel.getTargetId());
                        if (peer != null && !peer.isSignalActive() && random.nextDouble() < rel.getTrust() * 0.35) {
                            peer.setSignalActive(true);
                            contagionTransmissions++;
                        }
                    }
                }
            }
        }
    }

    private Agent selectMentor(Agent learner, Map<Long, Agent> aliveById, DeterministicRandom random) {
        var edges = socialNetwork.getOutgoingRelationships(learner.getId());
        if (edges.isEmpty()) {
            return null;
        }
        Agent best = null;
        double bestScore = -1.0;
        for (Relationship rel : edges) {
            Agent candidate = aliveById.get(rel.getTargetId());
            if (candidate == null) {
                continue;
            }
            double score = switch (mode) {
                case NONE -> -1.0;
                case RANDOM_IMITATION -> random.nextDouble();
                case SUCCESS_BASED -> candidate.getEnergy() > learner.getEnergy() ? candidate.getEnergy() : -1.0;
                case TRUST_WEIGHTED -> (candidate.getEnergy() > learner.getEnergy() && rel.getTrust() >= 0.50)
                        ? rel.getTrust() * candidate.getEnergy()
                        : -1.0;
            };
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    public SocialLearningMode getMode() { return mode; }
    public void setMode(SocialLearningMode mode) { this.mode = Objects.requireNonNull(mode); }
    public long getStrategyAdoptions() { return strategyAdoptions; }
    public long getContagionTransmissions() { return contagionTransmissions; }
}
