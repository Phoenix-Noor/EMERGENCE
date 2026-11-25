package com.emergence;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.core.DeterministicRandom;
import com.emergence.core.Simulation;
import com.emergence.core.SimulationConfig;
import com.emergence.decision.DecisionEngine;
import com.emergence.decision.DecisionRecord;
import com.emergence.game.*;
import com.emergence.world.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecisionAndGameTheoryTest {

    @Test
    void prisonersDilemmaPayoffMatrixMatchesSpecification() {
        PayoffMatrix matrix = new PayoffMatrix();
        assertTrue(matrix.isValidPrisonersDilemma());
        assertEquals(3.0, matrix.payoffFor(GameMove.COOPERATE, GameMove.COOPERATE));
        assertEquals(0.0, matrix.payoffFor(GameMove.COOPERATE, GameMove.DEFECT));
        assertEquals(5.0, matrix.payoffFor(GameMove.DEFECT, GameMove.COOPERATE));
        assertEquals(1.0, matrix.payoffFor(GameMove.DEFECT, GameMove.DEFECT));
    }

    @Test
    void allSixStrategiesBehaveAccordingToSpecification() {
        PayoffMatrix matrix = new PayoffMatrix();
        DeterministicRandom random = new DeterministicRandom(42L);
        AgentTraits neutralTraits = new AgentTraits(0.5, 0.5, 0.5, 0.5, 0.5, 0.6);
        Agent self = new Agent(1L, new Position(0, 0), 60, 100, 100, 100, 5, 1, neutralTraits);
        Agent opp = new Agent(2L, new Position(0, 1), 60, 100, 100, 100, 5, 1, neutralTraits);

        assertEquals(GameMove.COOPERATE, GameStrategies.forType(StrategyType.ALWAYS_COOPERATE)
                .chooseMove(self, opp, List.of(), matrix, random));
        assertEquals(GameMove.DEFECT, GameStrategies.forType(StrategyType.ALWAYS_DEFECT)
                .chooseMove(self, opp, List.of(), matrix, random));

        // Tit-for-Tat: cooperates first, then mirrors opponent's last move
        GameStrategy tft = GameStrategies.forType(StrategyType.TIT_FOR_TAT);
        assertEquals(GameMove.COOPERATE, tft.chooseMove(self, opp, List.of(), matrix, random));
        List<InteractionOutcome> oppDefected = List.of(
                new InteractionOutcome(1L, 2L, GameMove.COOPERATE, GameMove.DEFECT, 0.0, 5.0)
        );
        assertEquals(GameMove.DEFECT, tft.chooseMove(self, opp, oppDefected, matrix, random));

        // Conditional Cooperation: defects if opponent's coop rate < 0.5
        GameStrategy cond = GameStrategies.forType(StrategyType.CONDITIONAL_COOPERATION);
        assertEquals(GameMove.COOPERATE, cond.chooseMove(self, opp, List.of(), matrix, random));
        assertEquals(GameMove.DEFECT, cond.chooseMove(self, opp, oppDefected, matrix, random));

        // Resource Maximizer: maximizes expected immediate payoff
        GameStrategy maxRes = GameStrategies.forType(StrategyType.RESOURCE_MAXIMIZER);
        assertEquals(GameMove.DEFECT, maxRes.chooseMove(self, opp, List.of(), matrix, random));

        // Random: returns a valid GameMove deterministically
        GameMove rndMove = GameStrategies.forType(StrategyType.RANDOM)
                .chooseMove(self, opp, List.of(), matrix, random);
        assertNotNull(rndMove);
    }

    @Test
    void decisionEngineProducesExplainableDecisionsAndRespectsConstraints() {
        SimulationConfig config = new SimulationConfig();
        config.setWorldWidth(20);
        config.setWorldHeight(20);
        config.setInitialPopulation(10);
        config.setRandomSeed(77L);

        Simulation sim = new Simulation(config, new DecisionEngine());
        sim.stepTick();

        for (Agent agent : sim.getContext().getAgents()) {
            DecisionRecord record = agent.getLastDecision();
            assertNotNull(record, "Every living agent should have an explainable DecisionRecord");
            assertFalse(record.candidateUtilities().isEmpty());
            assertFalse(record.reason().isBlank());
            assertNotEquals("REPRODUCE", record.selectedAction(),
                    "New agents at age 1 must not reproduce due to minAge constraint");
        }
    }

    @Test
    void repeatedInteractionsAffectEnergyAndComputeMetrics() {
        SimulationConfig config = new SimulationConfig();
        config.setWorldWidth(5);
        config.setWorldHeight(5);
        config.setInitialPopulation(0);
        config.setWaterFraction(0.0);
        config.setHazardFraction(0.0);
        config.setForestFraction(0.0);
        config.setInitialFoodCellDensity(0.0);
        config.setResourceRegenerationRate(0.0);
        config.setBaseMetabolismCost(0.0);
        config.setMovementEnergyCost(0.0);
        config.setInteractionRadius(2);

        Simulation sim = new Simulation(config, new DecisionEngine());
        AgentTraits traits = new AgentTraits(0.2, 0.8, 0.8, 0.2, 0.5, 0.8);
        Agent coop1 = sim.getContext().spawnAgent(new Position(2, 2), 50.0, 1, traits);
        Agent coop2 = sim.getContext().spawnAgent(new Position(2, 2), 50.0, 1, traits);
        assertNotNull(coop1);
        assertNotNull(coop2);
        coop1.setStrategyType(StrategyType.ALWAYS_COOPERATE);
        coop2.setStrategyType(StrategyType.ALWAYS_COOPERATE);

        sim.stepTick();

        GameTheoryEngine engine = sim.getGameTheoryEngine();
        assertEquals(1L, engine.getTotalInteractions());
        assertEquals(1.0, engine.getCooperationFrequency(), 1e-6);
        assertEquals(0.0, engine.getDefectionFrequency(), 1e-6);
        assertEquals(3.0, engine.getAveragePayoff(), 1e-6);
        assertEquals(1.0, engine.getReciprocityRate(), 1e-6);

        // Mutual cooperation payoff (3.0 > 1.5 baseline) increases both agents' energy
        assertTrue(coop1.getEnergy() > 50.0);
        assertTrue(coop2.getEnergy() > 50.0);
    }

    @Test
    void phase2SimulationIsReproducibleWithFixedSeed() {
        SimulationConfig c1 = new SimulationConfig();
        c1.setWorldWidth(40);
        c1.setWorldHeight(40);
        c1.setInitialPopulation(40);
        c1.setRandomSeed(2026L);

        SimulationConfig c2 = new SimulationConfig();
        c2.setWorldWidth(40);
        c2.setWorldHeight(40);
        c2.setInitialPopulation(40);
        c2.setRandomSeed(2026L);

        Simulation sim1 = new Simulation(c1, new DecisionEngine());
        Simulation sim2 = new Simulation(c2, new DecisionEngine());

        for (int i = 0; i < 200; i++) {
            sim1.stepTick();
            sim2.stepTick();
        }

        assertEquals(sim1.getContext().getLivingPopulationCount(), sim2.getContext().getLivingPopulationCount());
        assertEquals(sim1.getGameTheoryEngine().getTotalInteractions(), sim2.getGameTheoryEngine().getTotalInteractions());
        assertEquals(sim1.getGameTheoryEngine().getCooperationFrequency(), sim2.getGameTheoryEngine().getCooperationFrequency(), 1e-9);
        assertEquals(sim1.getGameTheoryEngine().getAveragePayoff(), sim2.getGameTheoryEngine().getAveragePayoff(), 1e-9);
    }
}
