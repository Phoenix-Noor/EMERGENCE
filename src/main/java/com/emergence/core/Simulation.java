package com.emergence.core;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.agent.Perception;
import com.emergence.decision.Action;
import com.emergence.decision.AgentStrategy;
import com.emergence.world.Cell;
import com.emergence.world.Position;
import com.emergence.world.Terrain;
import com.emergence.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Core discrete-tick simulation coordinator.
 * Implements the 10-stage update order documented in PRD Section 9 & 25.
 */
public final class Simulation {

    private static final Logger LOG = LoggerFactory.getLogger(Simulation.class);

    private final SimulationConfig config;
    private final AgentStrategy defaultStrategy;
    private final List<SimulationModule> modules = new ArrayList<>();

    private SimulationContext context;
    private SimulationState state = SimulationState.IDLE;

    public Simulation(SimulationConfig config, AgentStrategy defaultStrategy) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.defaultStrategy = Objects.requireNonNull(defaultStrategy, "defaultStrategy must not be null");
        this.config.validate();
        initializeWorld();
    }

    public void registerModule(SimulationModule module) {
        Objects.requireNonNull(module, "module must not be null");
        modules.add(module);
        module.initialize(context);
    }

    public void initializeWorld() {
        DeterministicRandom random = new DeterministicRandom(config.getRandomSeed());
        World world = new World(config, random);
        SimulationClock clock = new SimulationClock();
        EventBus eventBus = new EventBus();
        this.context = new SimulationContext(config, world, clock, random, eventBus);

        spawnInitialPopulation();

        for (SimulationModule module : modules) {
            module.initialize(context);
        }

        state = SimulationState.IDLE;
        eventBus.publish(new SimulationEvent(0L, "LIFECYCLE", "World initialized with "
                + context.getLivingPopulationCount() + " agents (seed=" + config.getRandomSeed() + ")"));
        LOG.info("Initialized simulation: {}x{}, population={}, seed={}",
                config.getWorldWidth(), config.getWorldHeight(),
                context.getLivingPopulationCount(), config.getRandomSeed());
    }

    private void spawnInitialPopulation() {
        World world = context.getWorld();
        DeterministicRandom random = context.getRandom();
        List<Position> passablePositions = new ArrayList<>();
        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                Position pos = new Position(x, y);
                if (world.isPassable(pos) && world.getCell(pos).getTerrain() != Terrain.HAZARD) {
                    passablePositions.add(pos);
                }
            }
        }
        if (passablePositions.isEmpty()) {
            throw new IllegalStateException("No passable non-hazard cells available to spawn agents");
        }

        for (int i = 0; i < config.getInitialPopulation(); i++) {
            Position pos = passablePositions.get(random.nextInt(passablePositions.size()));
            AgentTraits traits = AgentTraits.random(random);
            context.spawnAgent(pos, config.getInitialAgentEnergy(), 1, traits);
        }
    }

    /**
     * Advances the simulation by exactly one discrete tick.
     * Stage order:
     * 1. Environment update (resource regeneration)
     * 2. Perception generation (consistent snapshot)
     * 3. Decision generation (simultaneous, no order advantage)
     * 4. Strategic interaction / module updates
     * 5. Movement & action resolution (deterministic order by agent ID)
     * 6. Health, energy, and hazard updates
     * 7. Reproduction and death
     * 8. Metrics and event recording
     */
    public void stepTick() {
        if (state == SimulationState.STOPPED) {
            return;
        }

        context.getClock().advanceTick();
        long tick = context.getClock().getCurrentTick();

        // 1. Update environmental state
        context.getWorld().regenerateResources();

        // Snapshot of living agents sorted deterministically by ID
        List<Agent> livingAgents = new ArrayList<>();
        for (Agent agent : context.getAgents()) {
            if (agent.isAlive()) {
                livingAgents.add(agent);
            }
        }
        livingAgents.sort(Comparator.comparingLong(Agent::getId));

        // 2 & 3. Calculate perceptions and generate decisions against consistent snapshot
        Map<Agent, Action> plannedActions = new LinkedHashMap<>();
        for (Agent agent : livingAgents) {
            List<Cell> visibleCells = context.getWorld().getCellsInRadius(agent.getPosition(), agent.getPerceptionRadius());
            List<Agent> nearbyAgents = context.findNearbyAgents(agent, agent.getPerceptionRadius());
            Perception perception = new Perception(
                    context.getWorld().getCell(agent.getPosition()),
                    visibleCells,
                    nearbyAgents
            );
            Action action = defaultStrategy.chooseAction(agent, perception, context);
            if (action != null) {
                plannedActions.put(agent, action);
            }
        }

        // 4 & 5. Resolve actions deterministically
        for (Map.Entry<Agent, Action> entry : plannedActions.entrySet()) {
            Agent agent = entry.getKey();
            Action action = entry.getValue();
            if (agent.isAlive() && action.isValid(context)) {
                action.execute(context);
                agent.setLastActionName(action.getName());
            } else if (agent.isAlive()) {
                agent.setLastActionName("IDLE");
            }
        }

        // 6. Update metabolism, age, and environmental hazards
        for (Agent agent : livingAgents) {
            if (!agent.isAlive()) {
                continue;
            }
            agent.incrementAge();
            agent.adjustEnergy(-config.getBaseMetabolismCost());

            Cell currentCell = context.getWorld().getCell(agent.getPosition());
            if (currentCell.getTerrain() == Terrain.HAZARD && agent.isAlive()) {
                agent.adjustHealth(-config.getHazardDamage());
                agent.getMemory().rememberHazard(agent.getPosition());
            }
        }

        // 7. Commit reproduction offspring & remove dead agents
        context.commitPendingOffspring();
        int deaths = context.removeDeadAgents();
        if (deaths > 0) {
            context.getEventBus().publish(new SimulationEvent(tick, "DEATH", deaths + " agent(s) died"));
        }

        // 8. Update registered modules
        for (SimulationModule module : modules) {
            module.update(context);
        }
    }

    public void start() {
        if (state == SimulationState.STOPPED) {
            initializeWorld();
        }
        state = SimulationState.RUNNING;
    }

    public void pause() {
        if (state == SimulationState.RUNNING) {
            state = SimulationState.PAUSED;
        }
    }

    public void resume() {
        if (state == SimulationState.PAUSED) {
            state = SimulationState.RUNNING;
        }
    }

    public void stop() {
        state = SimulationState.STOPPED;
        for (SimulationModule module : modules) {
            module.shutdown();
        }
    }

    public void reset() {
        initializeWorld();
    }

    public SimulationState getState() {
        return state;
    }

    public SimulationConfig getConfig() {
        return config;
    }

    public SimulationContext getContext() {
        return context;
    }
}
