package com.emergence.game;

import com.emergence.agent.Agent;
import com.emergence.core.DeterministicRandom;

import java.util.List;

/**
 * Common interface for game-theoretic strategic decision-making.
 */
public interface GameStrategy {

    StrategyType getType();

    GameMove chooseMove(
            Agent self,
            Agent opponent,
            List<InteractionOutcome> historyWithOpponent,
            PayoffMatrix payoffMatrix,
            DeterministicRandom random
    );
}
