package com.emergence.game;

import com.emergence.agent.Agent;
import com.emergence.core.SimulationContext;
import com.emergence.core.SimulationEvent;
import com.emergence.core.SimulationModule;
import com.emergence.social.SocialNetwork;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Coordinates repeated strategic interactions between nearby agents and collects game-theoretic metrics.
 */
public final class GameTheoryEngine implements SimulationModule {

    private static final double PAYOFF_BASELINE_NEUTRAL = 1.5;
    private static final double ENERGY_SCALE = 0.75;

    private final PayoffMatrix payoffMatrix;
    private final RepeatedGame repeatedGame;
    private SocialNetwork socialNetwork;

    private long totalInteractions;
    private long cooperationMoves;
    private long defectionMoves;
    private long reciprocalInteractions;
    private double totalPayoffSum;

    public GameTheoryEngine() {
        this(new PayoffMatrix());
    }

    public GameTheoryEngine(PayoffMatrix payoffMatrix) {
        this.payoffMatrix = Objects.requireNonNull(payoffMatrix, "payoffMatrix must not be null");
        this.repeatedGame = new RepeatedGame(payoffMatrix);
    }

    public void setSocialNetwork(SocialNetwork socialNetwork) {
        this.socialNetwork = socialNetwork;
    }

    @Override
    public void initialize(SimulationContext context) {
        resetMetrics();
    }

    @Override
    public void update(SimulationContext context) {
        if (!context.getConfig().isGameTheoryEnabled()) {
            return;
        }

        long tick = context.getClock().getCurrentTick();
        int radius = context.getConfig().getInteractionRadius();

        List<Agent> living = new ArrayList<>();
        for (Agent a : context.getAgents()) {
            if (a.isAlive()) {
                living.add(a);
            }
        }
        living.sort(Comparator.comparingLong(Agent::getId));

        Set<Long> interactedThisTick = new HashSet<>();
        int tickInteractions = 0;

        for (Agent agentA : living) {
            if (!agentA.isAlive() || interactedThisTick.contains(agentA.getId())) {
                continue;
            }
            Agent partner = findNearestEligiblePartner(agentA, living, interactedThisTick, radius);
            if (partner == null) {
                continue;
            }

            interactedThisTick.add(agentA.getId());
            interactedThisTick.add(partner.getId());

            List<InteractionOutcome> histA = agentA.getMemory().getInteractionsWith(partner.getId());
            List<InteractionOutcome> histB = partner.getMemory().getInteractionsWith(agentA.getId());

            RepeatedGame.RoundResult result = repeatedGame.playRound(
                    tick, agentA, partner, histA, histB, context.getRandom()
            );

            InteractionOutcome outA = result.outcomeA();
            InteractionOutcome outB = result.outcomeB();

            agentA.getMemory().recordInteraction(outA);
            partner.getMemory().recordInteraction(outB);

            if (socialNetwork != null) {
                socialNetwork.recordGameInteraction(agentA, partner, outB.selfMove(), tick);
                socialNetwork.recordGameInteraction(partner, agentA, outA.selfMove(), tick);
            }

            // Apply payoff to agent energy (payoffs above baseline reward cooperation; below baseline cost energy)
            agentA.adjustEnergy((outA.selfPayoff() - PAYOFF_BASELINE_NEUTRAL) * ENERGY_SCALE);
            partner.adjustEnergy((outB.selfPayoff() - PAYOFF_BASELINE_NEUTRAL) * ENERGY_SCALE);

            recordMetrics(outA, outB);
            tickInteractions++;
        }

        if (tickInteractions > 0 && tick % 10 == 0) {
            context.getEventBus().publish(new SimulationEvent(
                    tick,
                    "GAME",
                    String.format("Tick %d: %d strategic interactions (coop=%.1f%%)",
                            tick, tickInteractions, getCooperationFrequency() * 100.0)
            ));
        }
    }

    private Agent findNearestEligiblePartner(Agent self, List<Agent> candidates, Set<Long> busy, int radius) {
        Agent best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Agent other : candidates) {
            if (other.getId() == self.getId() || !other.isAlive() || busy.contains(other.getId())) {
                continue;
            }
            int dist = self.getPosition().manhattanDistanceTo(other.getPosition());
            if (dist <= radius && dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }

    private void recordMetrics(InteractionOutcome outA, InteractionOutcome outB) {
        totalInteractions++;
        if (outA.selfMove() == GameMove.COOPERATE) cooperationMoves++; else defectionMoves++;
        if (outB.selfMove() == GameMove.COOPERATE) cooperationMoves++; else defectionMoves++;

        if (outA.selfMove() == outB.selfMove()) {
            reciprocalInteractions++;
        }
        totalPayoffSum += outA.selfPayoff() + outB.selfPayoff();
    }

    public void resetMetrics() {
        totalInteractions = 0L;
        cooperationMoves = 0L;
        defectionMoves = 0L;
        reciprocalInteractions = 0L;
        totalPayoffSum = 0.0;
    }

    public PayoffMatrix getPayoffMatrix() { return payoffMatrix; }
    public long getTotalInteractions() { return totalInteractions; }
    public long getCooperationMoves() { return cooperationMoves; }
    public long getDefectionMoves() { return defectionMoves; }

    public double getCooperationFrequency() {
        long totalMoves = cooperationMoves + defectionMoves;
        return totalMoves == 0 ? 0.0 : (double) cooperationMoves / totalMoves;
    }

    public double getDefectionFrequency() {
        long totalMoves = cooperationMoves + defectionMoves;
        return totalMoves == 0 ? 0.0 : (double) defectionMoves / totalMoves;
    }

    public double getAveragePayoff() {
        long totalMoves = cooperationMoves + defectionMoves;
        return totalMoves == 0 ? 0.0 : totalPayoffSum / totalMoves;
    }

    public double getReciprocityRate() {
        return totalInteractions == 0 ? 0.0 : (double) reciprocalInteractions / totalInteractions;
    }

    public Map<StrategyType, Long> getStrategyDistribution(List<Agent> agents) {
        Map<StrategyType, Long> dist = new EnumMap<>(StrategyType.class);
        for (StrategyType type : StrategyType.values()) {
            dist.put(type, 0L);
        }
        for (Agent agent : agents) {
            if (agent.isAlive()) {
                dist.merge(agent.getStrategyType(), 1L, Long::sum);
            }
        }
        return dist;
    }
}
