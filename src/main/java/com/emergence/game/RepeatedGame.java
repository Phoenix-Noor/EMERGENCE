package com.emergence.game;

import com.emergence.agent.Agent;
import com.emergence.core.DeterministicRandom;

import java.util.List;
import java.util.Objects;

/**
 * Executes a single round of a two-player repeated strategic game.
 */
public final class RepeatedGame {

    private final PayoffMatrix payoffMatrix;

    public RepeatedGame(PayoffMatrix payoffMatrix) {
        this.payoffMatrix = Objects.requireNonNull(payoffMatrix, "payoffMatrix must not be null");
    }

    public record RoundResult(InteractionOutcome outcomeA, InteractionOutcome outcomeB) {
    }

    public RoundResult playRound(
            long tick,
            Agent agentA,
            Agent agentB,
            List<InteractionOutcome> historyAWithB,
            List<InteractionOutcome> historyBWithA,
            DeterministicRandom random
    ) {
        GameStrategy strategyA = GameStrategies.forType(agentA.getStrategyType());
        GameStrategy strategyB = GameStrategies.forType(agentB.getStrategyType());

        // Simultaneous move selection against historical state
        GameMove moveA = strategyA.chooseMove(agentA, agentB, historyAWithB, payoffMatrix, random);
        GameMove moveB = strategyB.chooseMove(agentB, agentA, historyBWithA, payoffMatrix, random);

        double payoffA = payoffMatrix.payoffFor(moveA, moveB);
        double payoffB = payoffMatrix.payoffFor(moveB, moveA);

        InteractionOutcome outA = new InteractionOutcome(tick, agentB.getId(), moveA, moveB, payoffA, payoffB);
        InteractionOutcome outB = new InteractionOutcome(tick, agentA.getId(), moveB, moveA, payoffB, payoffA);

        return new RoundResult(outA, outB);
    }

    public PayoffMatrix getPayoffMatrix() {
        return payoffMatrix;
    }
}
