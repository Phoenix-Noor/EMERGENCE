package com.emergence;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.core.Simulation;
import com.emergence.core.SimulationConfig;
import com.emergence.decision.DecisionEngine;
import com.emergence.swarm.AgentGroup;
import com.emergence.swarm.FlockingEngine;
import com.emergence.swarm.GroupManager;
import com.emergence.world.Position;
import com.emergence.world.Terrain;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SwarmIntelligenceTest {

    @Test
    void flockingRulesProduceExpectedVectorsIndependently() {
        FlockingEngine engine = new FlockingEngine();
        AgentTraits traits = new AgentTraits(0.2, 0.8, 0.9, 0.5, 0.5, 0.8);

        Agent self = new Agent(1L, new Position(10, 10), 60, 100, 100, 100, 6, 1, traits);
        Agent closeNeighbour = new Agent(2L, new Position(11, 10), 60, 100, 100, 100, 6, 1, traits);
        closeNeighbour.setLastMove(0, 1);
        Agent farNeighbour = new Agent(3L, new Position(15, 10), 60, 100, 100, 100, 6, 1, traits);
        farNeighbour.setLastMove(0, 1);

        // Separation pushes self away from closeNeighbour (towards negative X)
        var sep = engine.computeSeparation(self, List.of(closeNeighbour), 2);
        assertTrue(sep.x() < 0.0);

        // Alignment matches neighbours moving in +Y
        var ali = engine.computeAlignment(self, List.of(closeNeighbour, farNeighbour));
        assertTrue(ali.y() > 0.0);

        // Cohesion pulls self towards centroid of neighbours (+X)
        var coh = engine.computeCohesion(self, List.of(closeNeighbour, farNeighbour));
        assertTrue(coh.x() > 0.0);
    }

    @Test
    void groupManagerFormsMaintainsAndCleansUpGroups() {
        SimulationConfig config = new SimulationConfig();
        config.setWorldWidth(30);
        config.setWorldHeight(30);
        config.setInitialPopulation(0);
        config.setWaterFraction(0.0);
        config.setHazardFraction(0.0);
        config.setForestFraction(0.0);
        config.setGroupFormationRadius(5);

        Simulation sim = new Simulation(config, new DecisionEngine());
        AgentTraits traits = new AgentTraits(0.2, 0.8, 0.9, 0.4, 0.5, 0.8);

        Agent a1 = sim.getContext().spawnAgent(new Position(10, 10), 60, 1, traits);
        Agent a2 = sim.getContext().spawnAgent(new Position(11, 10), 60, 1, traits);
        Agent a3 = sim.getContext().spawnAgent(new Position(12, 10), 60, 1, traits);
        assertNotNull(a1);
        assertNotNull(a2);
        assertNotNull(a3);

        sim.stepTick();

        GroupManager gm = sim.getGroupManager();
        assertTrue(gm.getGroupCount() >= 1);
        assertTrue(gm.getAverageGroupSize() >= 2.0);
        assertTrue(gm.getAverageCohesion() > 0.0 && gm.getAverageCohesion() <= 1.0);

        // Kill two agents and verify group dissolves cleanly
        a1.adjustHealth(-1000.0);
        a2.adjustHealth(-1000.0);
        sim.stepTick();

        Set<Long> seenMembers = new HashSet<>();
        for (AgentGroup group : gm.getGroups()) {
            assertTrue(group.size() >= 2);
            for (Long memberId : group.getMemberIds()) {
                assertTrue(seenMembers.add(memberId), "Agent cannot belong to multiple groups");
            }
        }
    }

    @Test
    void navigationEngineSharesDiscoveredResourceLocations() {
        SimulationConfig config = new SimulationConfig();
        config.setWorldWidth(15);
        config.setWorldHeight(15);
        config.setInitialPopulation(0);
        config.setWaterFraction(0.0);
        config.setHazardFraction(0.0);
        config.setForestFraction(0.0);
        config.setInitialFoodCellDensity(0.0);
        config.setGroupFormationRadius(5);

        Simulation sim = new Simulation(config, new DecisionEngine());
        Position foodPos = new Position(7, 7);
        sim.getContext().getWorld().getCell(foodPos).setTerrain(Terrain.GRASSLAND);
        sim.getContext().getWorld().getCell(foodPos).setResourceAmount(20.0);

        AgentTraits coopTraits = new AgentTraits(0.1, 0.9, 0.9, 0.3, 0.5, 0.9);
        Agent discoverer = sim.getContext().spawnAgent(foodPos, 80, 1, coopTraits);
        Agent peer = sim.getContext().spawnAgent(new Position(9, 7), 80, 1, coopTraits);
        assertNotNull(discoverer);
        assertNotNull(peer);

        sim.stepTick();

        assertTrue(sim.getNavigationEngine().getResourcesSharedCount() >= 1);
        assertFalse(peer.getMemory().getKnownResourceLocations().isEmpty());
    }

    @Test
    void swarmSimulationRespectsBoundsAndIsReproducible() {
        SimulationConfig c1 = new SimulationConfig();
        c1.setWorldWidth(50);
        c1.setWorldHeight(50);
        c1.setInitialPopulation(50);
        c1.setRandomSeed(31415L);

        SimulationConfig c2 = new SimulationConfig();
        c2.setWorldWidth(50);
        c2.setWorldHeight(50);
        c2.setInitialPopulation(50);
        c2.setRandomSeed(31415L);

        Simulation sim1 = new Simulation(c1, new DecisionEngine());
        Simulation sim2 = new Simulation(c2, new DecisionEngine());

        for (int i = 0; i < 150; i++) {
            sim1.stepTick();
            sim2.stepTick();
        }

        assertEquals(sim1.getGroupManager().getGroupCount(), sim2.getGroupManager().getGroupCount());
        assertEquals(sim1.getGroupManager().getAverageGroupSize(), sim2.getGroupManager().getAverageGroupSize(), 1e-9);
        assertEquals(sim1.getGroupManager().getAverageCohesion(), sim2.getGroupManager().getAverageCohesion(), 1e-9);
        assertEquals(sim1.getNavigationEngine().getResourcesSharedCount(), sim2.getNavigationEngine().getResourcesSharedCount());

        for (Agent a : sim1.getContext().getAgents()) {
            assertTrue(sim1.getContext().getWorld().isInBounds(a.getPosition()));
        }
    }
}
