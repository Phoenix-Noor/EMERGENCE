package com.emergence.swarm;

import com.emergence.agent.Agent;
import com.emergence.core.SimulationContext;
import com.emergence.core.SimulationModule;
import com.emergence.world.Cell;
import com.emergence.world.Position;

import java.util.Comparator;
import java.util.List;

/**
 * Coordinates collective resource discovery and sharing of discovered resource locations among nearby/grouped agents.
 */
public final class NavigationEngine implements SimulationModule {

    private long resourcesSharedCount = 0L;
    private long collectiveDiscoveries = 0L;

    @Override
    public void initialize(SimulationContext context) {
        resourcesSharedCount = 0L;
        collectiveDiscoveries = 0L;
    }

    @Override
    public void update(SimulationContext context) {
        if (!context.getConfig().isSwarmEnabled() || !context.getConfig().isSwarmResourceSharingEnabled()) {
            return;
        }

        int shareRadius = context.getConfig().getGroupFormationRadius();
        for (Agent agent : context.getAgents()) {
            if (!agent.isAlive()) {
                continue;
            }
            Cell current = context.getWorld().getCell(agent.getPosition());
            if (current.getResourceAmount() > 1.0) {
                agent.getMemory().rememberResource(current.getPosition());
                List<Agent> nearby = context.findNearbyAgents(agent, shareRadius);
                for (Agent partner : nearby) {
                    boolean sameGroup = agent.getGroupId() != 0L && agent.getGroupId() == partner.getGroupId();
                    if (sameGroup || partner.getTraits().cooperation() >= 0.4) {
                        if (!partner.getMemory().getKnownResourceLocations().contains(current.getPosition())) {
                            partner.getMemory().rememberResource(current.getPosition());
                            resourcesSharedCount++;
                            if (sameGroup) {
                                collectiveDiscoveries++;
                            }
                        }
                    }
                }
            }
        }
    }

    public Position getBestKnownSharedResource(Agent agent, SimulationContext context) {
        List<Position> known = agent.getMemory().getKnownResourceLocations();
        if (known.isEmpty()) {
            return null;
        }
        return known.stream()
                .filter(pos -> context.getWorld().isInBounds(pos))
                .filter(pos -> context.getWorld().getCell(pos).getResourceAmount() > 0.5)
                .min(Comparator.comparingInt(pos -> agent.getPosition().manhattanDistanceTo(pos)))
                .orElse(null);
    }

    public long getResourcesSharedCount() { return resourcesSharedCount; }
    public long getCollectiveDiscoveries() { return collectiveDiscoveries; }
}
