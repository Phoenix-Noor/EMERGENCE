package com.emergence.social;

import com.emergence.agent.Agent;
import com.emergence.core.SimulationContext;
import com.emergence.core.SimulationModule;
import com.emergence.game.GameMove;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the directed social relationship graph, trust evolution, reputation, and network metrics.
 */
public final class SocialNetwork implements SimulationModule {

    private static final double STRENGTH_DECAY_PER_TICK = 0.002;
    private static final double PRUNE_STRENGTH_THRESHOLD = 0.02;

    private final Map<Long, Map<Long, Relationship>> adjacency = new LinkedHashMap<>();

    @Override
    public void initialize(SimulationContext context) {
        adjacency.clear();
    }

    @Override
    public void update(SimulationContext context) {
        Set<Long> aliveIds = new HashSet<>();
        for (Agent a : context.getAgents()) {
            if (a.isAlive()) {
                aliveIds.add(a.getId());
            }
        }

        // Remove dead agents and decay inactive relationships
        long currentTick = context.getClock().getCurrentTick();
        Iterator<Map.Entry<Long, Map<Long, Relationship>>> outerIt = adjacency.entrySet().iterator();
        while (outerIt.hasNext()) {
            Map.Entry<Long, Map<Long, Relationship>> entry = outerIt.next();
            if (!aliveIds.contains(entry.getKey())) {
                outerIt.remove();
                continue;
            }
            Iterator<Map.Entry<Long, Relationship>> innerIt = entry.getValue().entrySet().iterator();
            while (innerIt.hasNext()) {
                Relationship rel = innerIt.next().getValue();
                if (!aliveIds.contains(rel.getTargetId())) {
                    innerIt.remove();
                    continue;
                }
                if (currentTick - rel.getLastInteractionTick() > 15) {
                    rel.decay(STRENGTH_DECAY_PER_TICK);
                    if (rel.getStrength() < PRUNE_STRENGTH_THRESHOLD) {
                        innerIt.remove();
                    }
                }
            }
        }
    }

    public void recordGameInteraction(Agent source, Agent target, GameMove targetMove, long tick) {
        double trustDelta = (targetMove == GameMove.COOPERATE) ? 0.08 : -0.15;
        double strengthDelta = 0.10;
        Map<Long, Relationship> edges = adjacency.computeIfAbsent(source.getId(), k -> new LinkedHashMap<>());
        Relationship existing = edges.get(target.getId());
        if (existing == null) {
            double initialTrust = Math.max(0.0, Math.min(1.0, source.getTraits().trustPropensity() + trustDelta));
            edges.put(target.getId(), new Relationship(source.getId(), target.getId(), initialTrust, 0.25, tick));
        } else {
            existing.update(trustDelta, strengthDelta, tick);
        }
    }

    public Relationship getRelationship(long sourceId, long targetId) {
        Map<Long, Relationship> edges = adjacency.get(sourceId);
        return edges == null ? null : edges.get(targetId);
    }

    public Collection<Relationship> getOutgoingRelationships(long agentId) {
        Map<Long, Relationship> edges = adjacency.get(agentId);
        return edges == null ? List.of() : Collections.unmodifiableCollection(edges.values());
    }

    public double getTrust(long sourceId, long targetId, double defaultTrust) {
        Relationship rel = getRelationship(sourceId, targetId);
        return rel != null ? rel.getTrust() : defaultTrust;
    }

    /**
     * Computes an agent's reputation as the mean incoming trust from all agents that have interacted with it.
     */
    public double getReputation(long targetId) {
        double sum = 0.0;
        int count = 0;
        for (Map<Long, Relationship> edges : adjacency.values()) {
            Relationship rel = edges.get(targetId);
            if (rel != null) {
                sum += rel.getTrust();
                count++;
            }
        }
        return count == 0 ? 0.5 : sum / count;
    }

    public int getTotalEdgeCount() {
        int count = 0;
        for (Map<Long, Relationship> edges : adjacency.values()) {
            count += edges.size();
        }
        return count;
    }

    public double getNetworkDensity(int livingPopulation) {
        if (livingPopulation < 2) {
            return 0.0;
        }
        double maxDirectedEdges = (double) livingPopulation * (livingPopulation - 1);
        return getTotalEdgeCount() / maxDirectedEdges;
    }

    public double getAverageDegree(int livingPopulation) {
        if (livingPopulation == 0) {
            return 0.0;
        }
        return (double) getTotalEdgeCount() / livingPopulation;
    }

    public double getAverageTrust() {
        double sum = 0.0;
        int count = 0;
        for (Map<Long, Relationship> edges : adjacency.values()) {
            for (Relationship rel : edges.values()) {
                sum += rel.getTrust();
                count++;
            }
        }
        return count == 0 ? 0.5 : sum / count;
    }

    public double getClusteringCoefficient(List<Agent> livingAgents) {
        if (livingAgents.size() < 3) {
            return 0.0;
        }
        double totalCoeff = 0.0;
        int counted = 0;
        Map<Long, Set<Long>> undirected = new HashMap<>();
        for (Agent a : livingAgents) {
            undirected.put(a.getId(), new HashSet<>());
        }
        for (Map.Entry<Long, Map<Long, Relationship>> entry : adjacency.entrySet()) {
            long u = entry.getKey();
            if (!undirected.containsKey(u)) continue;
            for (Long v : entry.getValue().keySet()) {
                if (undirected.containsKey(v)) {
                    undirected.get(u).add(v);
                    undirected.get(v).add(u);
                }
            }
        }
        for (Set<Long> neighbors : undirected.values()) {
            int k = neighbors.size();
            if (k < 2) {
                continue;
            }
            List<Long> list = new ArrayList<>(neighbors);
            int links = 0;
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    if (undirected.get(list.get(i)).contains(list.get(j))) {
                        links++;
                    }
                }
            }
            totalCoeff += (2.0 * links) / (k * (k - 1));
            counted++;
        }
        return counted == 0 ? 0.0 : totalCoeff / counted;
    }
}
