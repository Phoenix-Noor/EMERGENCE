# EMERGENCE

**Simple Rules. Complex Worlds.**

Emergence is a configurable, game-theoretic, multi-agent artificial-life simulation platform built in Java 21 and JavaFX.

## Technology Stack
- **Java:** 21 LTS
- **Desktop UI:** JavaFX 21
- **Build:** Maven
- **Database:** SQLite
- **Serialization:** Jackson JSON
- **Testing:** JUnit 5
- **Logging:** SLF4J + Logback

## Quick Start

```powershell
# Compile and run tests
mvn test

# Launch the JavaFX application
mvn javafx:run
```

## Project Structure
- `com.emergence.core` — Simulation engine, clock, config, deterministic RNG, event bus
- `com.emergence.world` — 2D grid world, terrain, cells, spatial queries
- `com.emergence.agent` — Autonomous agents, traits, memory, perception
- `com.emergence.decision` — Actions, strategies, decision engine
- `com.emergence.game` — Game theory engine (Prisoner's Dilemma, repeated games)
- `com.emergence.swarm` — Flocking and group management
- `com.emergence.social` — Social network, trust, information diffusion
- `com.emergence.ecosystem` — Resource dynamics, reproduction, evolution
- `com.emergence.analytics` — Metrics, experiments, statistics
- `com.emergence.persistence` — SQLite & JSON repositories
- `com.emergence.ui` — JavaFX world renderer and laboratory dashboard
