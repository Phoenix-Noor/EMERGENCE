package com.emergence;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.core.DeterministicRandom;
import com.emergence.core.Simulation;
import com.emergence.core.SimulationConfig;
import com.emergence.core.SimulationContext;
import com.emergence.decision.ConsumeResourceAction;
import com.emergence.decision.LivingWorldStrategy;
import com.emergence.world.Cell;
import com.emergence.world.Position;
import com.emergence.world.Terrain;
import com.emergence.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LivingWorldTest {

    @Test
    void singleAgentInEmptyWorldDiesFromEnergyDepletion() {
        SimulationConfig config = new SimulationConfig();
        config.setWorldWidth(10);
        config.setWorldHeight(10);
        config.setInitialPopulation(0);
        config.setInitialFoodCellDensity(0.0);
        config.setWaterFraction(0.0);
        config.setForestFraction(0.0);
        config.setHazardFraction(0.0);
        config.setResourceRegenerationRate(0.0);
        config.setInitialAgentEnergy(5.0);
        config.setBaseMetabolismCost(1.0);
        config.setMovementEnergyCost(1.0);

        Simulation sim = new Simulation(config, new LivingWorldStrategy());
        Agent agent = sim.getContext().spawnAgent(
                new Position(5, 5), 5.0, 1, AgentTraits.random(sim.getContext().getRandom())
        );
        assertNotNull(agent);

        for (int i = 0; i < 10; i++) {
            sim.stepTick();
        }

        assertFalse(agent.isAlive());
        assertEquals(0, sim.getContext().getLivingPopulationCount());
        assertEquals(1, sim.getContext().getTotalDeaths());
    }

    @Test
    void agentConsumesAvailableFoodResourceAndNeverMakesCellNegative() {
        SimulationConfig config = new SimulationConfig();
        config.setWorldWidth(5);
        config.setWorldHeight(5);
        config.setInitialPopulation(0);
        config.setInitialFoodCellDensity(0.0);
        config.setWaterFraction(0.0);
        config.setForestFraction(0.0);
        config.setHazardFraction(0.0);

        Simulation sim = new Simulation(config, new LivingWorldStrategy());
        SimulationContext ctx = sim.getContext();
        Position pos = new Position(2, 2);
        Cell cell = ctx.getWorld().getCell(pos);
        cell.setTerrain(Terrain.GRASSLAND);
        cell.setResourceAmount(8.0);

        Agent agent = ctx.spawnAgent(pos, 50.0, 1, AgentTraits.random(ctx.getRandom()));
        assertNotNull(agent);

        ConsumeResourceAction consume = new ConsumeResourceAction(agent);
        assertTrue(consume.isValid(ctx));
        consume.execute(ctx);

        assertTrue(agent.getEnergy() > 50.0);
        assertEquals(0.0, cell.getResourceAmount(), 1e-6);
    }

    @Test
    void resourceRegenerationIsCappedAtMaxResource() {
        Cell cell = new Cell(new Position(0, 0), Terrain.GRASSLAND, 24.0, 25.0, 2.0);
        cell.regenerate();
        assertEquals(25.0, cell.getResourceAmount(), 1e-6);
    }

    @Test
    void fixedSeedProducesIdenticalSimulationResults() {
        SimulationConfig c1 = new SimulationConfig();
        c1.setWorldWidth(40);
        c1.setWorldHeight(40);
        c1.setInitialPopulation(30);
        c1.setRandomSeed(123456L);

        SimulationConfig c2 = new SimulationConfig();
        c2.setWorldWidth(40);
        c2.setWorldHeight(40);
        c2.setInitialPopulation(30);
        c2.setRandomSeed(123456L);

        Simulation sim1 = new Simulation(c1, new LivingWorldStrategy());
        Simulation sim2 = new Simulation(c2, new LivingWorldStrategy());

        for (int i = 0; i < 200; i++) {
            sim1.stepTick();
            sim2.stepTick();
        }

        assertEquals(sim1.getContext().getLivingPopulationCount(), sim2.getContext().getLivingPopulationCount());
        assertEquals(sim1.getContext().getTotalBirths(), sim2.getContext().getTotalBirths());
        assertEquals(sim1.getContext().getTotalDeaths(), sim2.getContext().getTotalDeaths());
        assertEquals(sim1.getContext().getWorld().getTotalResources(), sim2.getContext().getWorld().getTotalResources(), 1e-6);
    }

    @Test
    void thousandTickSimulationPreservesAllInvariants() {
        SimulationConfig config = new SimulationConfig();
        config.setRandomSeed(99L);
        Simulation sim = new Simulation(config, new LivingWorldStrategy());

        for (int tick = 0; tick < 1000; tick++) {
            sim.stepTick();
        }

        SimulationContext ctx = sim.getContext();
        World world = ctx.getWorld();

        assertEquals(1000L, ctx.getClock().getCurrentTick());
        assertTrue(ctx.getLivingPopulationCount() <= config.getMaxPopulation());

        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                Cell cell = world.getCell(x, y);
                assertTrue(cell.getResourceAmount() >= 0.0);
                assertTrue(cell.getResourceAmount() <= cell.getMaxResource());
            }
        }

        for (Agent agent : ctx.getAgents()) {
            assertTrue(agent.isAlive());
            assertTrue(world.isInBounds(agent.getPosition()));
            assertTrue(agent.getEnergy() > 0.0 && agent.getEnergy() <= agent.getMaxEnergy());
            assertTrue(agent.getHealth() > 0.0 && agent.getHealth() <= agent.getMaxHealth());
        }
    }
}
