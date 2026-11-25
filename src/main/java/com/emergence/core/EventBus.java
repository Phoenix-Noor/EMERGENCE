package com.emergence.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Synchronous, deterministic event bus for simulation events.
 * Retains a bounded recent history for UI inspection.
 */
public final class EventBus {

    private static final int DEFAULT_HISTORY_LIMIT = 500;

    private final int historyLimit;
    private final List<Consumer<SimulationEvent>> listeners = new ArrayList<>();
    private final List<SimulationEvent> recentEvents = new ArrayList<>();

    public EventBus() {
        this(DEFAULT_HISTORY_LIMIT);
    }

    public EventBus(int historyLimit) {
        if (historyLimit <= 0) {
            throw new IllegalArgumentException("historyLimit must be positive");
        }
        this.historyLimit = historyLimit;
    }

    public void subscribe(Consumer<SimulationEvent> listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
    }

    public void publish(SimulationEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        recentEvents.add(event);
        if (recentEvents.size() > historyLimit) {
            recentEvents.remove(0);
        }
        for (Consumer<SimulationEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    public List<SimulationEvent> getRecentEvents() {
        return Collections.unmodifiableList(new ArrayList<>(recentEvents));
    }

    public void clear() {
        recentEvents.clear();
    }
}
