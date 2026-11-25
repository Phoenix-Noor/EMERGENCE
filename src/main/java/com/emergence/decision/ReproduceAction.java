package com.emergence.decision;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.core.SimulationConfig;
import com.emergence.core.SimulationContext;
import com.emergence.core.SimulationEvent;
import com.emergence.world.Position;
import com.emergence.ecosystem.ReproductionEngine;

import java.util.Objects;

/**
 * Produces an offspring agent with inherited (and optionally mutated) traits when thresholds are met.
 */
public final class ReproduceAction implements Action {

    private final Agent parent;
    private final ReproductionEngine reproductionEngine = new ReproductionEngine();

    public ReproduceAction(Agent parent) {
        this.parent = Objects.requireNonNull(parent, "parent must not be null");
    }

    @Override
    public String getName() {
        return "REPRODUCE";
    }

    @Override
    public boolean isValid(SimulationContext context) {
        if (!parent.isAlive()) {
            return false;
        }
        SimulationConfig cfg = context.getConfig();
        return parent.getAge() >= cfg.getReproductionMinAge()
                && parent.getEnergy() >= cfg.getReproductionMinEnergy()
                && parent.getHealth() >= cfg.getReproductionMinHealth()
                && parent.getReproductionCooldown() == 0
                && context.getLivingPopulationCount() < cfg.getMaxPopulation();
    }

    @Override
    public void execute(SimulationContext context) {
        SimulationConfig cfg = context.getConfig();
        // Apply energy cost and cooldown
        parent.adjustEnergy(-cfg.getReproductionEnergyCost());
        parent.setReproductionCooldown(cfg.getReproductionCooldownTicks());

        Position spawnPos = findSpawnPosition(context);

        // Inherit traits using config‑controlled mutation
        AgentTraits childTraits = reproductionEngine.inheritTraits(parent, cfg, context.getRandom());

        // Queue offspring (generation = parent+1)
        Agent child = context.queueOffspring(
                spawnPos,
                cfg.getReproductionEnergyCost() * 0.9,
                parent.getGeneration() + 1,
                childTraits
        );
        if (child != null) {
            // Record lineage
            child.setParentId(parent.getId());
            parent.incrementOffspringCount();
            // Inherit or mutate strategy according to config
            child.setStrategyType(
                    reproductionEngine.inheritStrategy(parent, childTraits, cfg, context.getRandom())
            );
            context.getEventBus().publish(new SimulationEvent(
                    context.getClock().getCurrentTick(),
                    "BIRTH",
                    "Agent #" + parent.getId() + " reproduced -> Agent #" + child.getId()
            ));
        }
    }

    private Position findSpawnPosition(SimulationContext context) {
        int[][] offsets = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int[] offset : offsets) {
            Position candidate = parent.getPosition().translate(offset[0], offset[1]);
            if (context.getWorld().isPassable(candidate)) {
                return context.getWorld().normalizePosition(candidate);
            }
        }
        return parent.getPosition();
    }
}
