package com.emergence.agent;

import com.emergence.world.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Bounded memory storing recent discovered food locations, known hazards, and interaction notes.
 */
public final class AgentMemory {

    private static final int DEFAULT_CAPACITY = 20;

    private final int capacity;
    private final Deque<Position> knownResourceLocations = new ArrayDeque<>();
    private final Deque<Position> knownHazards = new ArrayDeque<>();
    private final Deque<String> recentEvents = new ArrayDeque<>();

    public AgentMemory() {
        this(DEFAULT_CAPACITY);
    }

    public AgentMemory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public void rememberResource(Position position) {
        addBounded(knownResourceLocations, position);
    }

    public void forgetResource(Position position) {
        knownResourceLocations.remove(position);
    }

    public void rememberHazard(Position position) {
        addBounded(knownHazards, position);
    }

    public void recordEvent(String summary) {
        addBounded(recentEvents, summary);
    }

    public List<Position> getKnownResourceLocations() {
        return Collections.unmodifiableList(new ArrayList<>(knownResourceLocations));
    }

    public List<Position> getKnownHazards() {
        return Collections.unmodifiableList(new ArrayList<>(knownHazards));
    }

    public List<String> getRecentEvents() {
        return Collections.unmodifiableList(new ArrayList<>(recentEvents));
    }

    private <T> void addBounded(Deque<T> deque, T item) {
        if (item == null) {
            return;
        }
        deque.remove(item);
        deque.addLast(item);
        while (deque.size() > capacity) {
            deque.removeFirst();
        }
    }
}
