package com.emergence.game;

import com.emergence.agent.Agent;
import com.emergence.core.DeterministicRandom;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry and implementations of the six core game-theoretic strategies.
 */
public final class GameStrategies {

    private static final Map<StrategyType, GameStrategy> REGISTRY = new EnumMap<>(StrategyType.class);

    static {
        register(new AlwaysCooperate());
        register(new AlwaysDefect());
        register(new TitForTat());
        register(new RandomStrategy());
        register(new ConditionalCooperation());
        register(new ResourceMaximizer());
    }

    private GameStrategies() {
    }

    private static void register(GameStrategy strategy) {
        REGISTRY.put(strategy.getType(), strategy);
    }

    public static GameStrategy forType(StrategyType type) {
        return REGISTRY.getOrDefault(type, REGISTRY.get(StrategyType.TIT_FOR_TAT));
    }

    public static StrategyType assignFromTraits(com.emergence.agent.AgentTraits traits, DeterministicRandom random) {
        double coop = traits.cooperation();
        double aggr = traits.aggression();
        if (coop >= 0.78 && aggr < 0.35) {
            return StrategyType.ALWAYS_COOPERATE;
        }
        if (aggr >= 0.78 && coop < 0.35) {
            return StrategyType.ALWAYS_DEFECT;
        }
        if (coop >= 0.55 && traits.trustPropensity() >= 0.5) {
            return StrategyType.TIT_FOR_TAT;
        }
        if (coop >= 0.45) {
            return StrategyType.CONDITIONAL_COOPERATION;
        }
        if (aggr >= 0.55) {
            return StrategyType.RESOURCE_MAXIMIZER;
        }
        return random.nextDouble() < 0.5 ? StrategyType.RANDOM : StrategyType.TIT_FOR_TAT;
    }

    private static final class AlwaysCooperate implements GameStrategy {
        @Override
        public StrategyType getType() { return StrategyType.ALWAYS_COOPERATE; }

        @Override
        public GameMove chooseMove(Agent self, Agent opponent, List<InteractionOutcome> history, PayoffMatrix matrix, DeterministicRandom random) {
            return GameMove.COOPERATE;
        }
    }

    private static final class AlwaysDefect implements GameStrategy {
        @Override
        public StrategyType getType() { return StrategyType.ALWAYS_DEFECT; }

        @Override
        public GameMove chooseMove(Agent self, Agent opponent, List<InteractionOutcome> history, PayoffMatrix matrix, DeterministicRandom random) {
            return GameMove.DEFECT;
        }
    }

    private static final class TitForTat implements GameStrategy {
        @Override
        public StrategyType getType() { return StrategyType.TIT_FOR_TAT; }

        @Override
        public GameMove chooseMove(Agent self, Agent opponent, List<InteractionOutcome> history, PayoffMatrix matrix, DeterministicRandom random) {
            if (history.isEmpty()) {
                return GameMove.COOPERATE;
            }
            return history.get(history.size() - 1).partnerMove();
        }
    }

    private static final class RandomStrategy implements GameStrategy {
        @Override
        public StrategyType getType() { return StrategyType.RANDOM; }

        @Override
        public GameMove chooseMove(Agent self, Agent opponent, List<InteractionOutcome> history, PayoffMatrix matrix, DeterministicRandom random) {
            return random.nextBoolean() ? GameMove.COOPERATE : GameMove.DEFECT;
        }
    }

    private static final class ConditionalCooperation implements GameStrategy {
        @Override
        public StrategyType getType() { return StrategyType.CONDITIONAL_COOPERATION; }

        @Override
        public GameMove chooseMove(Agent self, Agent opponent, List<InteractionOutcome> history, PayoffMatrix matrix, DeterministicRandom random) {
            if (history.isEmpty()) {
                return self.getTraits().trustPropensity() >= 0.45 ? GameMove.COOPERATE : GameMove.DEFECT;
            }
            long coopCount = history.stream().filter(o -> o.partnerMove() == GameMove.COOPERATE).count();
            double coopRate = (double) coopCount / history.size();
            return coopRate >= 0.5 ? GameMove.COOPERATE : GameMove.DEFECT;
        }
    }

    private static final class ResourceMaximizer implements GameStrategy {
        @Override
        public StrategyType getType() { return StrategyType.RESOURCE_MAXIMIZER; }

        @Override
        public GameMove chooseMove(Agent self, Agent opponent, List<InteractionOutcome> history, PayoffMatrix matrix, DeterministicRandom random) {
            double pCoop;
            if (history.isEmpty()) {
                pCoop = 0.5;
            } else {
                long coopCount = history.stream().filter(o -> o.partnerMove() == GameMove.COOPERATE).count();
                pCoop = (double) coopCount / history.size();
            }
            double expectedCoop = pCoop * matrix.getRewardBothCooperate()
                    + (1.0 - pCoop) * matrix.getSuckerCooperatorVsDefector();
            double expectedDefect = pCoop * matrix.getTemptationDefectorVsCooperator()
                    + (1.0 - pCoop) * matrix.getPunishmentBothDefect();
            return expectedCoop > expectedDefect ? GameMove.COOPERATE : GameMove.DEFECT;
        }
    }
}
