package com.emergence.swarm;

import com.emergence.agent.Agent;
import com.emergence.core.SimulationContext;
import com.emergence.core.SimulationModule;
import com.emergence.world.Position;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages leaderless dynamic agent groups: formation, joining, leaving, splitting, merging, and cleanup.
 */
public final class GroupManager implements SimulationModule {

    private final Map<Long, AgentGroup> groups = new LinkedHashMap<>();
    private long nextGroupId = 1L;

    @Override
    public void initialize(SimulationContext context) {
        groups.clear();
        nextGroupId = 1L;
    }

    @Override
    public void update(SimulationContext context) {
        if (!context.getConfig().isSwarmEnabled()) {
            return;
        }
        int groupRadius = context.getConfig().getGroupFormationRadius();

        Map<Long, Agent> aliveById = new HashMap<>();
        List<Agent> living = new ArrayList<>();
        for (Agent a : context.getAgents()) {
            if (a.isAlive()) {
                aliveById.put(a.getId(), a);
                living.add(a);
            }
        }
        living.sort(Comparator.comparingLong(Agent::getId));

        // 1. Remove dead or dispersed members and dissolve undersized groups
        Iterator<Map.Entry<Long, AgentGroup>> groupIt = groups.entrySet().iterator();
        while (groupIt.hasNext()) {
            AgentGroup group = groupIt.next().getValue();
            List<Long> toRemove = new ArrayList<>();
            for (Long memberId : group.getMemberIds()) {
                Agent member = aliveById.get(memberId);
                if (member == null) {
                    toRemove.add(memberId);
                } else if (member.getPosition().manhattanDistanceTo(group.getCentroid()) > groupRadius * 2) {
                    toRemove.add(memberId);
                    member.setGroupId(0L);
                }
            }
            for (Long id : toRemove) {
                group.removeMember(id);
            }
            if (group.size() < 2) {
                for (Long remaining : new ArrayList<>(group.getMemberIds())) {
                    Agent a = aliveById.get(remaining);
                    if (a != null) {
                        a.setGroupId(0L);
                    }
                    group.removeMember(remaining);
                }
                groupIt.remove();
            } else {
                recomputeCentroidAndCohesion(group, aliveById);
            }
        }

        // 2. Assign ungrouped agents to nearby groups or form new groups
        for (Agent agent : living) {
            if (agent.getGroupId() != 0L && groups.containsKey(agent.getGroupId())) {
                continue;
            }
            agent.setGroupId(0L);

            AgentGroup nearestGroup = findNearestGroup(agent, groupRadius);
            if (nearestGroup != null) {
                nearestGroup.addMember(agent.getId());
                agent.setGroupId(nearestGroup.getId());
                recomputeCentroidAndCohesion(nearestGroup, aliveById);
            } else {
                Agent partner = findNearestUngroupedAgent(agent, living, groupRadius);
                if (partner != null) {
                    Position centroid = new Position(
                            (agent.getPosition().x() + partner.getPosition().x()) / 2,
                            (agent.getPosition().y() + partner.getPosition().y()) / 2
                    );
                    AgentGroup created = new AgentGroup(nextGroupId++, centroid);
                    created.addMember(agent.getId());
                    created.addMember(partner.getId());
                    agent.setGroupId(created.getId());
                    partner.setGroupId(created.getId());
                    recomputeCentroidAndCohesion(created, aliveById);
                    groups.put(created.getId(), created);
                }
            }
        }

        // 3. Merge overlapping groups whose centroids are within groupRadius
        mergeCloseGroups(groupRadius, aliveById);
    }

    private AgentGroup findNearestGroup(Agent agent, int radius) {
        AgentGroup best = null;
        int bestDist = Integer.MAX_VALUE;
        for (AgentGroup group : groups.values()) {
            int dist = agent.getPosition().manhattanDistanceTo(group.getCentroid());
            if (dist <= radius && dist < bestDist) {
                bestDist = dist;
                best = group;
            }
        }
        return best;
    }

    private Agent findNearestUngroupedAgent(Agent self, List<Agent> living, int radius) {
        Agent best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Agent other : living) {
            if (other.getId() == self.getId() || other.getGroupId() != 0L) {
                continue;
            }
            int dist = self.getPosition().manhattanDistanceTo(other.getPosition());
            if (dist <= radius && dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }

    private void mergeCloseGroups(int radius, Map<Long, Agent> aliveById) {
        List<Long> ids = new ArrayList<>(groups.keySet());
        for (int i = 0; i < ids.size(); i++) {
            AgentGroup g1 = groups.get(ids.get(i));
            if (g1 == null) continue;
            for (int j = i + 1; j < ids.size(); j++) {
                AgentGroup g2 = groups.get(ids.get(j));
                if (g2 == null) continue;
                if (g1.getCentroid().manhattanDistanceTo(g2.getCentroid()) <= Math.max(1, radius / 2)) {
                    for (Long memberId : new ArrayList<>(g2.getMemberIds())) {
                        g1.addMember(memberId);
                        Agent m = aliveById.get(memberId);
                        if (m != null) {
                            m.setGroupId(g1.getId());
                        }
                    }
                    groups.remove(g2.getId());
                    recomputeCentroidAndCohesion(g1, aliveById);
                }
            }
        }
    }

    private void recomputeCentroidAndCohesion(AgentGroup group, Map<Long, Agent> aliveById) {
        if (group.size() == 0) {
            return;
        }
        double sx = 0.0;
        double sy = 0.0;
        int count = 0;
        for (Long id : group.getMemberIds()) {
            Agent a = aliveById.get(id);
            if (a != null) {
                sx += a.getPosition().x();
                sy += a.getPosition().y();
                count++;
            }
        }
        if (count == 0) {
            return;
        }
        Position centroid = new Position((int) Math.round(sx / count), (int) Math.round(sy / count));
        group.setCentroid(centroid);

        double totalDist = 0.0;
        for (Long id : group.getMemberIds()) {
            Agent a = aliveById.get(id);
            if (a != null) {
                totalDist += a.getPosition().euclideanDistanceTo(centroid);
            }
        }
        double avgDist = totalDist / count;
        group.setCohesion(1.0 / (1.0 + avgDist));
    }

    public Collection<AgentGroup> getGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    public AgentGroup getGroup(long groupId) {
        return groups.get(groupId);
    }

    public int getGroupCount() {
        return groups.size();
    }

    public double getAverageGroupSize() {
        if (groups.isEmpty()) {
            return 0.0;
        }
        int total = 0;
        for (AgentGroup g : groups.values()) {
            total += g.size();
        }
        return (double) total / groups.size();
    }

    public double getAverageCohesion() {
        if (groups.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (AgentGroup g : groups.values()) {
            sum += g.getCohesion();
        }
        return sum / groups.size();
    }
}
