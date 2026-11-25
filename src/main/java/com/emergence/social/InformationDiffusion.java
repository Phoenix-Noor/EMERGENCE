package com.emergence.social;

import com.emergence.agent.Agent;
import com.emergence.core.SimulationContext;
import com.emergence.core.SimulationModule;
import com.emergence.world.Cell;
import com.emergence.world.Terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Diffuses resource, hazard, and behavioural information hop-by-hop through trusted social relationships.
 */
public final class InformationDiffusion implements SimulationModule {

    private static final double MIN_TRUST_TO_SHARE = 0.35;
    private static final int DEFAULT_TTL_TICKS = 15;

    private final SocialNetwork socialNetwork;
    private final Map<Long, List<InformationMessage>> inboxes = new HashMap<>();
    private final Map<Long, Set<Long>> seenMessagesByAgent = new HashMap<>();
    private final Set<Long> reachedAgents = new HashSet<>();

    private long nextMessageId = 1L;
    private long messagesSent = 0L;
    private long messagesDelivered = 0L;

    public InformationDiffusion(SocialNetwork socialNetwork) {
        this.socialNetwork = Objects.requireNonNull(socialNetwork, "socialNetwork must not be null");
    }

    @Override
    public void initialize(SimulationContext context) {
        inboxes.clear();
        seenMessagesByAgent.clear();
        reachedAgents.clear();
        nextMessageId = 1L;
        messagesSent = 0L;
        messagesDelivered = 0L;
    }

    @Override
    public void update(SimulationContext context) {
        long tick = context.getClock().getCurrentTick();
        Map<Long, Agent> aliveById = new HashMap<>();
        for (Agent a : context.getAgents()) {
            if (a.isAlive()) {
                aliveById.put(a.getId(), a);
            }
        }

        // 1. Agents originate messages when observing rich food or hazards
        for (Agent agent : aliveById.values()) {
            Cell cell = context.getWorld().getCell(agent.getPosition());
            if (cell.getResourceAmount() >= context.getConfig().getFoodConsumptionAmount() * 0.8) {
                broadcastToTrustedConnections(
                        agent,
                        InformationMessage.InformationType.RESOURCE_LOCATION,
                        cell.getPosition(),
                        "Rich food",
                        0.9,
                        tick,
                        aliveById
                );
            } else if (cell.getTerrain() == Terrain.HAZARD) {
                broadcastToTrustedConnections(
                        agent,
                        InformationMessage.InformationType.HAZARD_LOCATION,
                        cell.getPosition(),
                        "Hazard",
                        0.95,
                        tick,
                        aliveById
                );
            }
        }

        // 2. Process inboxes and forward non-expired messages one hop
        Map<Long, List<InformationMessage>> snapshotInboxes = new HashMap<>(inboxes);
        inboxes.clear();

        for (Map.Entry<Long, List<InformationMessage>> entry : snapshotInboxes.entrySet()) {
            Agent recipient = aliveById.get(entry.getKey());
            if (recipient == null) {
                continue;
            }
            for (InformationMessage msg : entry.getValue()) {
                if (msg.isExpired(tick)) {
                    continue;
                }
                applyMessageToMemory(recipient, msg);
                if (msg.confidence() >= 0.55) {
                    forwardMessage(recipient, msg, tick, aliveById);
                }
            }
        }
    }

    public InformationMessage publishMessage(
            Agent sender,
            InformationMessage.InformationType type,
            com.emergence.world.Position position,
            String payload,
            double confidence,
            long tick,
            Map<Long, Agent> aliveById
    ) {
        return broadcastToTrustedConnections(sender, type, position, payload, confidence, tick, aliveById);
    }

    private InformationMessage broadcastToTrustedConnections(
            Agent sender,
            InformationMessage.InformationType type,
            com.emergence.world.Position position,
            String payload,
            double confidence,
            long tick,
            Map<Long, Agent> aliveById
    ) {
        InformationMessage msg = new InformationMessage(
                nextMessageId++,
                sender.getId(),
                tick,
                tick + DEFAULT_TTL_TICKS,
                type,
                position,
                payload,
                confidence
        );
        seenMessagesByAgent.computeIfAbsent(sender.getId(), k -> new HashSet<>()).add(msg.id());
        deliverToNeighbours(sender, msg, aliveById);
        return msg;
    }

    private void forwardMessage(Agent relay, InformationMessage msg, long tick, Map<Long, Agent> aliveById) {
        InformationMessage decayed = new InformationMessage(
                msg.id(),
                msg.sourceAgentId(),
                msg.creationTick(),
                msg.expirationTick(),
                msg.type(),
                msg.position(),
                msg.payload(),
                msg.confidence() * 0.75
        );
        if (!decayed.isExpired(tick)) {
            deliverToNeighbours(relay, decayed, aliveById);
        }
    }

    private void deliverToNeighbours(Agent sender, InformationMessage msg, Map<Long, Agent> aliveById) {
        for (Relationship rel : socialNetwork.getOutgoingRelationships(sender.getId())) {
            if (rel.getTrust() < MIN_TRUST_TO_SHARE) {
                continue;
            }
            Agent target = aliveById.get(rel.getTargetId());
            if (target == null) {
                continue;
            }
            Set<Long> seen = seenMessagesByAgent.computeIfAbsent(target.getId(), k -> new HashSet<>());
            if (seen.add(msg.id())) {
                inboxes.computeIfAbsent(target.getId(), k -> new ArrayList<>()).add(msg);
                messagesSent++;
                messagesDelivered++;
                reachedAgents.add(target.getId());
            }
        }
    }

    private void applyMessageToMemory(Agent recipient, InformationMessage msg) {
        recipient.getMemory().incrementMessagesReceived();
        if (msg.position() == null) {
            return;
        }
        switch (msg.type()) {
            case RESOURCE_LOCATION -> recipient.getMemory().rememberResource(msg.position());
            case HAZARD_LOCATION -> recipient.getMemory().rememberHazard(msg.position());
            case BEHAVIOURAL_RECOMMENDATION -> recipient.getMemory().recordEvent("Tip: " + msg.payload());
        }
    }

    public long getMessagesSent() { return messagesSent; }
    public long getMessagesDelivered() { return messagesDelivered; }
    public int getInformationReach() { return reachedAgents.size(); }
}
