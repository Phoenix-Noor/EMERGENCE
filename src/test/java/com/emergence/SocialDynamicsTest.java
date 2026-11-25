package com.emergence;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.core.Simulation;
import com.emergence.core.SimulationConfig;
import com.emergence.decision.DecisionEngine;
import com.emergence.game.GameMove;
import com.emergence.game.StrategyType;
import com.emergence.social.InformationDiffusion;
import com.emergence.social.InformationMessage;
import com.emergence.social.SocialLearningEngine;
import com.emergence.social.SocialNetwork;
import com.emergence.world.Position;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SocialDynamicsTest {

    @Test
    void trustAndReputationEvolveFromCooperationAndDefection() {
        SocialNetwork net = new SocialNetwork();
        AgentTraits traits = new AgentTraits(0.2, 0.8, 0.8, 0.4, 0.5, 0.5);
        Agent a = new Agent(1L, new Position(0, 0), 60, 100, 100, 100, 5, 1, traits);
        Agent b = new Agent(2L, new Position(0, 1), 60, 100, 100, 100, 5, 1, traits);
        Agent c = new Agent(3L, new Position(1, 0), 60, 100, 100, 100, 5, 1, traits);

        net.recordGameInteraction(a, b, GameMove.COOPERATE, 1L);
        net.recordGameInteraction(a, b, GameMove.COOPERATE, 2L);
        net.recordGameInteraction(a, c, GameMove.DEFECT, 2L);

        double trustAB = net.getTrust(1L, 2L, 0.5);
        double trustAC = net.getTrust(1L, 3L, 0.5);
        assertTrue(trustAB > 0.5, "Cooperation should increase trust");
        assertTrue(trustAC < 0.5, "Defection should decrease trust");
        assertTrue(net.getReputation(2L) > net.getReputation(3L));

        // Triangle clustering coefficient = 1.0 when all 3 are mutually connected
        net.recordGameInteraction(b, c, GameMove.COOPERATE, 3L);
        assertEquals(1.0, net.getClusteringCoefficient(List.of(a, b, c)), 1e-6);
        assertTrue(net.getNetworkDensity(3) > 0.0);
    }

    @Test
    void informationSpreadsAcrossTrustedNetworkAndExpires() {
        SimulationConfig config = new SimulationConfig();
        config.setWorldWidth(10);
        config.setWorldHeight(10);
        config.setInitialPopulation(0);
        config.setWaterFraction(0.0);
        config.setHazardFraction(0.0);
        config.setForestFraction(0.0);

        Simulation sim = new Simulation(config, new DecisionEngine());
        AgentTraits traits = new AgentTraits(0.1, 0.9, 0.9, 0.3, 0.5, 0.8);
        Agent a = sim.getContext().spawnAgent(new Position(1, 1), 70, 1, traits);
        Agent b = sim.getContext().spawnAgent(new Position(5, 5), 70, 1, traits);
        Agent c = sim.getContext().spawnAgent(new Position(8, 8), 70, 1, traits);
        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(c);

        SocialNetwork net = sim.getSocialNetwork();
        net.recordGameInteraction(a, b, GameMove.COOPERATE, 1L);
        net.recordGameInteraction(b, c, GameMove.COOPERATE, 1L);

        InformationDiffusion diffusion = sim.getInformationDiffusion();
        Position secretFood = new Position(4, 4);
        InformationMessage msg = diffusion.publishMessage(
                a,
                InformationMessage.InformationType.RESOURCE_LOCATION,
                secretFood,
                "Rich food",
                0.9,
                1L,
                Map.of(1L, a, 2L, b, 3L, c)
        );

        assertFalse(msg.isExpired(10L));
        assertTrue(msg.isExpired(100L));

        // Tick 1 delivers A -> B and forwards B -> C; Tick 2 delivers B -> C
        sim.stepTick();
        sim.stepTick();

        assertTrue(b.getMemory().getKnownResourceLocations().contains(secretFood));
        assertTrue(c.getMemory().getKnownResourceLocations().contains(secretFood));
        assertTrue(diffusion.getInformationReach() >= 2);
    }

    @Test
    void socialLearningAndContagionOperateAcrossTrustedEdges() {
        SimulationConfig config = new SimulationConfig();
        config.setWorldWidth(10);
        config.setWorldHeight(10);
        config.setInitialPopulation(0);
        config.setWaterFraction(0.0);
        config.setHazardFraction(0.0);
        config.setForestFraction(0.0);
        config.setGameTheoryEnabled(false);

        Simulation sim = new Simulation(config, new DecisionEngine());
        AgentTraits traits = new AgentTraits(0.2, 0.8, 0.8, 0.4, 0.5, 0.8);
        Agent learner = sim.getContext().spawnAgent(new Position(2, 2), 30, 1, traits);
        Agent mentor = sim.getContext().spawnAgent(new Position(7, 7), 95, 1, traits);
        assertNotNull(learner);
        assertNotNull(mentor);

        learner.setStrategyType(StrategyType.RANDOM);
        mentor.setStrategyType(StrategyType.TIT_FOR_TAT);
        mentor.setSignalActive(true);

        SocialNetwork net = sim.getSocialNetwork();
        for (int i = 0; i < 4; i++) {
            net.recordGameInteraction(learner, mentor, GameMove.COOPERATE, i);
            net.recordGameInteraction(mentor, learner, GameMove.COOPERATE, i);
        }

        SocialLearningEngine sle = sim.getSocialLearningEngine();
        sle.setMode(SocialLearningEngine.SocialLearningMode.TRUST_WEIGHTED);

        for (int i = 0; i < 15; i++) {
            sim.stepTick();
        }

        assertTrue(sle.getStrategyAdoptions() >= 1 || learner.getStrategyType() == StrategyType.TIT_FOR_TAT);
        assertTrue(sle.getContagionTransmissions() >= 1);
        assertTrue(learner.isSignalActive());
    }

    @Test
    void phase4SocialDynamicsAreReproducibleWithFixedSeed() {
        SimulationConfig c1 = new SimulationConfig();
        c1.setWorldWidth(40);
        c1.setWorldHeight(40);
        c1.setInitialPopulation(40);
        c1.setRandomSeed(8888L);

        SimulationConfig c2 = new SimulationConfig();
        c2.setWorldWidth(40);
        c2.setWorldHeight(40);
        c2.setInitialPopulation(40);
        c2.setRandomSeed(8888L);

        Simulation sim1 = new Simulation(c1, new DecisionEngine());
        Simulation sim2 = new Simulation(c2, new DecisionEngine());

        for (int i = 0; i < 150; i++) {
            sim1.stepTick();
            sim2.stepTick();
        }

        assertEquals(sim1.getSocialNetwork().getTotalEdgeCount(), sim2.getSocialNetwork().getTotalEdgeCount());
        assertEquals(sim1.getSocialNetwork().getAverageTrust(), sim2.getSocialNetwork().getAverageTrust(), 1e-9);
        assertEquals(sim1.getInformationDiffusion().getInformationReach(), sim2.getInformationDiffusion().getInformationReach());
    }
}
