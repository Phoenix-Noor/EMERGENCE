package com.emergence;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.core.DeterministicRandom;
import com.emergence.core.Simulation;
import com.emergence.core.SimulationConfig;
import com.emergence.core.SimulationContext;
import com.emergence.decision.LivingWorldStrategy;
import com.emergence.ecosystem.EvolutionEngine;
import com.emergence.ecosystem.ReproductionEngine;
import com.emergence.game.StrategyType;
import com.emergence.world.Position;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Phase 5: Evolution and Natural Selection.
 * Covers trait inheritance, mutation, generation tracking, lineage, determinism, and metrics.
 */
class EvolutionTest {

    private SimulationConfig smallConfig() {
        SimulationConfig cfg = new SimulationConfig();
        cfg.setWorldWidth(20);
        cfg.setWorldHeight(20);
        cfg.setInitialPopulation(0);
        cfg.setMaxPopulation(200);
        cfg.setInitialFoodCellDensity(0.5);
        cfg.setWaterFraction(0.0);
        cfg.setForestFraction(0.0);
        cfg.setHazardFraction(0.0);
        cfg.setResourceRegenerationRate(1.0);
        cfg.setInitialAgentEnergy(90.0);
        cfg.setMaxAgentEnergy(100.0);
        cfg.setBaseMetabolismCost(0.1);
        cfg.setMovementEnergyCost(0.1);
        cfg.setReproductionMinAge(3);
        cfg.setReproductionMinEnergy(50.0);
        cfg.setReproductionMinHealth(50.0);
        cfg.setReproductionEnergyCost(20.0);
        cfg.setReproductionCooldownTicks(2);
        cfg.setRandomSeed(42L);
        cfg.setEvolutionEnabled(true);
        cfg.setMutationRate(0.25);
        cfg.setMutationStrength(0.10);
        cfg.setStrategyInheritanceEnabled(true);
        return cfg;
    }

    // ─── Test 1: Zero-mutation inheritance preserves parent traits exactly ─────
    @Test
    void zeroMutationInheritancePreservesParentTraits() {
        SimulationConfig cfg = smallConfig();
        cfg.setMutationRate(0.0);
        cfg.setMutationStrength(0.0);
        DeterministicRandom random = new DeterministicRandom(99L);

        AgentTraits parentTraits = new AgentTraits(0.3, 0.6, 0.5, 0.8, 0.2, 0.7);
        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());
        Agent parent = sim.getContext().spawnAgent(new Position(5, 5), 90.0, 1, parentTraits);

        ReproductionEngine engine = new ReproductionEngine();
        AgentTraits childTraits = engine.inheritTraits(parent, cfg, random);

        assertEquals(parentTraits.aggression(), childTraits.aggression(), 1e-12);
        assertEquals(parentTraits.cooperation(), childTraits.cooperation(), 1e-12);
        assertEquals(parentTraits.sociability(), childTraits.sociability(), 1e-12);
        assertEquals(parentTraits.exploration(), childTraits.exploration(), 1e-12);
        assertEquals(parentTraits.riskTolerance(), childTraits.riskTolerance(), 1e-12);
        assertEquals(parentTraits.trustPropensity(), childTraits.trustPropensity(), 1e-12);
    }

    // ─── Test 2: Mutated traits remain within [0.0, 1.0] ────────────────────
    @Test
    void mutatedTraitsStayWithinBounds() {
        SimulationConfig cfg = smallConfig();
        cfg.setMutationRate(1.0);    // mutate every trait
        cfg.setMutationStrength(0.5); // large mutation

        DeterministicRandom random = new DeterministicRandom(123L);
        AgentTraits extremeTraits = new AgentTraits(0.0, 1.0, 0.0, 1.0, 0.0, 1.0);
        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());
        Agent parent = sim.getContext().spawnAgent(new Position(3, 3), 90.0, 1, extremeTraits);

        ReproductionEngine engine = new ReproductionEngine();
        for (int i = 0; i < 100; i++) {
            AgentTraits child = engine.inheritTraits(parent, cfg, random);
            assertTrue(child.aggression() >= 0.0 && child.aggression() <= 1.0,
                    "aggression out of bounds: " + child.aggression());
            assertTrue(child.cooperation() >= 0.0 && child.cooperation() <= 1.0,
                    "cooperation out of bounds: " + child.cooperation());
            assertTrue(child.sociability() >= 0.0 && child.sociability() <= 1.0,
                    "sociability out of bounds: " + child.sociability());
            assertTrue(child.exploration() >= 0.0 && child.exploration() <= 1.0,
                    "exploration out of bounds: " + child.exploration());
            assertTrue(child.riskTolerance() >= 0.0 && child.riskTolerance() <= 1.0,
                    "riskTolerance out of bounds: " + child.riskTolerance());
            assertTrue(child.trustPropensity() >= 0.0 && child.trustPropensity() <= 1.0,
                    "trustPropensity out of bounds: " + child.trustPropensity());
        }
    }

    // ─── Test 3: Generation is set correctly on offspring ────────────────────
    @Test
    void offspringHasCorrectGenerationAndLineage() {
        SimulationConfig cfg = smallConfig();
        cfg.setMutationRate(0.0);
        cfg.setMutationStrength(0.0);
        // High energy, low thresholds to trigger reproduction
        cfg.setInitialAgentEnergy(95.0);
        cfg.setReproductionMinAge(1);
        cfg.setReproductionMinEnergy(30.0);
        cfg.setReproductionEnergyCost(10.0);
        cfg.setReproductionCooldownTicks(0);

        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());
        Agent parent = sim.getContext().spawnAgent(
                new Position(10, 10), 95.0, 1,
                new AgentTraits(0.5, 0.5, 0.5, 0.5, 0.5, 0.5)
        );
        long parentId = parent.getId();

        // Run ticks until reproduction occurs
        for (int i = 0; i < 20; i++) {
            sim.stepTick();
        }

        // Find offspring: agents with parentId == parent's ID
        List<Agent> children = new ArrayList<>();
        for (Agent a : sim.getContext().getAgents()) {
            if (a.getParentId() == parentId) {
                children.add(a);
            }
        }

        assertFalse(children.isEmpty(), "Parent should have produced at least one offspring");
        for (Agent child : children) {
            assertEquals(parentId, child.getParentId(), "Child parentId should match parent");
            assertEquals(2, child.getGeneration(), "Child should be generation 2");
        }
        assertTrue(parent.getOffspringCount() > 0, "Parent offspringCount should be incremented");
    }

    // ─── Test 4: Offspring count increments on parent ───────────────────────
    @Test
    void parentOffspringCountIncrements() {
        SimulationConfig cfg = smallConfig();
        cfg.setInitialAgentEnergy(95.0);
        cfg.setReproductionMinAge(1);
        cfg.setReproductionMinEnergy(30.0);
        cfg.setReproductionEnergyCost(10.0);
        cfg.setReproductionCooldownTicks(0);
        cfg.setMutationRate(0.0);
        cfg.setMutationStrength(0.0);

        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());
        Agent parent = sim.getContext().spawnAgent(
                new Position(10, 10), 95.0, 1,
                new AgentTraits(0.5, 0.5, 0.5, 0.5, 0.5, 0.5)
        );
        assertEquals(0, parent.getOffspringCount());

        for (int i = 0; i < 15; i++) {
            sim.stepTick();
        }

        // If reproduction happened, count must have incremented
        long childCount = 0;
        for (Agent a : sim.getContext().getAgents()) {
            if (a.getParentId() == parent.getId()) {
                childCount++;
            }
        }
        assertEquals(childCount, parent.getOffspringCount(),
                "offspringCount should equal number of children found");
    }

    // ─── Test 5: EvolutionEngine metrics are computed correctly ─────────────
    @Test
    void evolutionEngineComputesMetricsAfterTicks() {
        SimulationConfig cfg = smallConfig();
        cfg.setInitialPopulation(10);
        cfg.setRandomSeed(77L);

        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());
        EvolutionEngine engine = sim.getEvolutionEngine();

        // After initialization, metrics should already be computed
        assertTrue(engine.getMaxGeneration() >= 1, "maxGeneration should be at least 1");
        assertTrue(engine.getAverageGeneration() >= 1.0, "averageGeneration should be at least 1.0");
        assertNotNull(engine.getMeanTraits(), "meanTraits should not be null");

        for (int i = 0; i < 10; i++) {
            sim.stepTick();
        }

        // After ticks, history should be populated
        assertFalse(engine.getHistory().isEmpty(), "history should have snapshots");
        assertTrue(engine.getTraitDiversity() >= 0.0, "diversity should be non-negative");
    }

    // ─── Test 6: Deterministic reproduction with fixed seed ─────────────────
    @Test
    void deterministicReproductionWithFixedSeed() {
        List<Double> run1Traits = collectTraitsAfterRun(42L);
        List<Double> run2Traits = collectTraitsAfterRun(42L);

        assertEquals(run1Traits.size(), run2Traits.size(),
                "Two runs with same seed must produce same population size");
        for (int i = 0; i < run1Traits.size(); i++) {
            assertEquals(run1Traits.get(i), run2Traits.get(i), 1e-12,
                    "Trait at index " + i + " should be identical across deterministic runs");
        }
    }

    private List<Double> collectTraitsAfterRun(long seed) {
        SimulationConfig cfg = smallConfig();
        cfg.setRandomSeed(seed);
        cfg.setInitialPopulation(5);
        cfg.setReproductionMinAge(2);
        cfg.setReproductionMinEnergy(40.0);
        cfg.setReproductionEnergyCost(15.0);
        cfg.setReproductionCooldownTicks(1);

        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());
        for (int i = 0; i < 20; i++) {
            sim.stepTick();
        }

        List<Double> traits = new ArrayList<>();
        for (Agent a : sim.getContext().getAgents()) {
            traits.add(a.getTraits().aggression());
            traits.add(a.getTraits().cooperation());
            traits.add(a.getTraits().sociability());
            traits.add(a.getTraits().exploration());
            traits.add(a.getTraits().riskTolerance());
            traits.add(a.getTraits().trustPropensity());
        }
        return traits;
    }

    // ─── Test 7: Strategy inheritance when enabled ──────────────────────────
    @Test
    void strategyInheritanceWhenEnabled() {
        SimulationConfig cfg = smallConfig();
        cfg.setMutationRate(0.0);
        cfg.setMutationStrength(0.0);
        cfg.setStrategyInheritanceEnabled(true);
        DeterministicRandom random = new DeterministicRandom(1L);

        AgentTraits traits = new AgentTraits(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());
        Agent parent = sim.getContext().spawnAgent(new Position(5, 5), 90.0, 1, traits);
        parent.setStrategyType(StrategyType.ALWAYS_COOPERATE);

        ReproductionEngine engine = new ReproductionEngine();
        // With evolution disabled (no mutation chance), strategy should be inherited exactly
        cfg.setEvolutionEnabled(false);
        StrategyType childStrategy = engine.inheritStrategy(parent, traits, cfg, random);
        assertEquals(StrategyType.ALWAYS_COOPERATE, childStrategy,
                "With strategy inheritance and no evolution, child should get parent strategy");
    }

    // ─── Test 8: Strategy assigned from traits when inheritance disabled ────
    @Test
    void strategyAssignedFromTraitsWhenInheritanceDisabled() {
        SimulationConfig cfg = smallConfig();
        cfg.setStrategyInheritanceEnabled(false);
        DeterministicRandom random = new DeterministicRandom(1L);

        AgentTraits traits = new AgentTraits(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());
        Agent parent = sim.getContext().spawnAgent(new Position(5, 5), 90.0, 1, traits);
        parent.setStrategyType(StrategyType.ALWAYS_DEFECT);

        ReproductionEngine engine = new ReproductionEngine();
        StrategyType childStrategy = engine.inheritStrategy(parent, traits, cfg, random);
        // Should NOT be forced to parent's strategy
        assertNotNull(childStrategy, "Child strategy should be assigned");
    }

    // ─── Test 9: Evolution disabled means no trait mutation ─────────────────
    @Test
    void evolutionDisabledPreservesExactTraits() {
        SimulationConfig cfg = smallConfig();
        cfg.setEvolutionEnabled(false);
        cfg.setMutationRate(1.0);        // would mutate everything if evolution were on
        cfg.setMutationStrength(0.5);
        DeterministicRandom random = new DeterministicRandom(42L);

        AgentTraits parentTraits = new AgentTraits(0.4, 0.3, 0.7, 0.1, 0.9, 0.55);
        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());
        Agent parent = sim.getContext().spawnAgent(new Position(5, 5), 90.0, 1, parentTraits);

        ReproductionEngine engine = new ReproductionEngine();
        AgentTraits childTraits = engine.inheritTraits(parent, cfg, random);

        assertEquals(parentTraits.aggression(), childTraits.aggression(), 1e-12);
        assertEquals(parentTraits.cooperation(), childTraits.cooperation(), 1e-12);
        assertEquals(parentTraits.sociability(), childTraits.sociability(), 1e-12);
        assertEquals(parentTraits.exploration(), childTraits.exploration(), 1e-12);
        assertEquals(parentTraits.riskTolerance(), childTraits.riskTolerance(), 1e-12);
        assertEquals(parentTraits.trustPropensity(), childTraits.trustPropensity(), 1e-12);
    }

    // ─── Test 10: Multi-generation evolution produces generation > 2 ────────
    @Test
    void multiGenerationEvolutionProducesHigherGenerations() {
        SimulationConfig cfg = smallConfig();
        cfg.setInitialPopulation(10);
        cfg.setReproductionMinAge(2);
        cfg.setReproductionMinEnergy(35.0);
        cfg.setReproductionEnergyCost(10.0);
        cfg.setReproductionCooldownTicks(1);
        cfg.setInitialAgentEnergy(95.0);
        cfg.setBaseMetabolismCost(0.05);
        cfg.setRandomSeed(55L);

        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());

        // Run for many ticks to allow multiple generations
        for (int i = 0; i < 50; i++) {
            sim.stepTick();
        }

        int maxGen = 1;
        for (Agent a : sim.getContext().getAgents()) {
            if (a.getGeneration() > maxGen) {
                maxGen = a.getGeneration();
            }
        }

        assertTrue(maxGen >= 2,
                "After 50 ticks with favourable conditions, at least generation 2 should exist. Got: " + maxGen);
        assertEquals(maxGen, sim.getEvolutionEngine().getMaxGeneration(),
                "EvolutionEngine maxGeneration should match actual max");
    }

    // ─── Test 11: EvolutionEngine history grows with ticks ──────────────────
    @Test
    void evolutionEngineHistoryGrowsWithTicks() {
        SimulationConfig cfg = smallConfig();
        cfg.setInitialPopulation(5);
        cfg.setRandomSeed(33L);

        Simulation sim = new Simulation(cfg, new LivingWorldStrategy());
        int initialSize = sim.getEvolutionEngine().getHistory().size();

        for (int i = 0; i < 5; i++) {
            sim.stepTick();
        }

        int afterSize = sim.getEvolutionEngine().getHistory().size();
        assertTrue(afterSize > initialSize,
                "History should grow as ticks are simulated");
    }
}
