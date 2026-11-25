package com.emergence.core;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.game.GameStrategies;
import com.emergence.world.Position;
import com.emergence.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Shared simulation runtime context passed to modules, strategies, and actions.
 * Owns agent ID generation and active population list.
 */
public final class SimulationContext {

    private final SimulationConfig config;
    private final World world;
    private final SimulationClock clock;
    private final DeterministicRandom random;
    private final EventBus eventBus;
    private final List<Agent> agents = new ArrayList<>();
    private final List<Agent> pendingOffspring = new ArrayList<>();

    private long nextAgentId = 1L;
    private long totalBirths = 0L;
    private long totalDeaths = 0L;

    public SimulationContext(
            SimulationConfig config,
            World world,
            SimulationClock clock,
            DeterministicRandom random,
            EventBus eventBus
    ) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.world = Objects.requireNonNull(world, "world must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus must not be null");
    }

    public SimulationConfig getConfig() { return config; }
    public World getWorld() { return world; }
    public SimulationClock getClock() { return clock; }
    public DeterministicRandom getRandom() { return random; }
    public EventBus getEventBus() { return eventBus; }

    public List<Agent> getAgents() {
        return Collections.unmodifiableList(agents);
    }

    public int getLivingPopulationCount() {
        int count = 0;
        for (Agent agent : agents) {
            if (agent.isAlive()) {
                count++;
            }
        }
        return count;
    }

    public long getTotalBirths() { return totalBirths; }
    public long getTotalDeaths() { return totalDeaths; }

    public Agent spawnAgent(Position position, double initialEnergy, int generation, AgentTraits traits) {
        if (agents.size() + pendingOffspring.size() >= config.getMaxPopulation()) {
            return null;
        }
        Agent agent = new Agent(
                nextAgentId++,
                position,
                initialEnergy,
                config.getMaxAgentEnergy(),
                config.getInitialAgentHealth(),
                config.getMaxAgentHealth(),
                config.getDefaultPerceptionRadius(),
                generation,
                traits
        );
        agent.setStrategyType(GameStrategies.assignFromTraits(traits, random));
        agents.add(agent);
        return agent;
    }

    public Agent queueOffspring(Position position, double initialEnergy, int generation, AgentTraits traits) {
        if (agents.size() + pendingOffspring.size() >= config.getMaxPopulation()) {
            return null;
        }
        Agent offspring = new Agent(
                nextAgentId++,
                position,
                initialEnergy,
                config.getMaxAgentEnergy(),
                config.getInitialAgentHealth(),
                config.getMaxAgentHealth(),
                config.getDefaultPerceptionRadius(),
                generation,
                traits
        );
        // parentId set by ReproduceAction
        pendingOffspring.add(offspring);
        totalBirths++;
        return offspring;
    }

    public void commitPendingOffspring() {
        if (!pendingOffspring.isEmpty()) {
            agents.addAll(pendingOffspring);
            pendingOffspring.clear();
        }
    }

    public int removeDeadAgents() {
        int removed = 0;
        var iterator = agents.iterator();
        while (iterator.hasNext()) {
            Agent agent = iterator.next();
            if (!agent.isAlive()) {
                iterator.remove();
                removed++;
                totalDeaths++;
            }
        }
        return removed;
    }

    public List<Agent> findNearbyAgents(Agent self, int radius) {
        List<Agent> nearby = new ArrayList<>();
        Position origin = self.getPosition();
        for (Agent other : agents) {
            if (other.getId() != self.getId() && other.isAlive()) {
                if (origin.manhattanDistanceTo(other.getPosition()) <= radius) {
                    nearby.add(other);
                }
            }
        }
        nearby.sort(Comparator.comparingLong(Agent::getId));
        return nearby;
    }
}
