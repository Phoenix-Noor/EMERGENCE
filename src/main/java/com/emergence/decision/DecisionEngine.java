package com.emergence.decision;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentState;
import com.emergence.agent.Perception;
import com.emergence.core.DeterministicRandom;
import com.emergence.core.SimulationContext;
import com.emergence.world.Cell;
import com.emergence.world.Position;
import com.emergence.world.Terrain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Hybrid rule-based + utility-based decision engine with full decision explainability.
 */
public final class DecisionEngine implements AgentStrategy {

    private static final int[][] DIRECTIONS = {
            {0, -1}, {1, 0}, {0, 1}, {-1, 0},
            {1, -1}, {1, 1}, {-1, 1}, {-1, -1}
    };

    private final UtilityEvaluator evaluator;

    public DecisionEngine() {
        this(new UtilityEvaluator());
    }

    public DecisionEngine(UtilityEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    }

    @Override
    public String getStrategyName() {
        return "UtilityDecisionEngine";
    }

    @Override
    public Action chooseAction(Agent agent, Perception perception, SimulationContext context) {
        agent.setState(AgentState.ACTIVE);

        List<Action> candidates = generateValidCandidates(agent, perception, context);
        Map<String, Double> utilityMap = new LinkedHashMap<>();

        Action bestAction = null;
        UtilityEvaluator.Evaluation bestEval = null;

        for (Action candidate : candidates) {
            UtilityEvaluator.Evaluation eval = evaluator.evaluate(candidate, agent, perception, context);
            String label = candidate.getName();
            if (candidate instanceof MoveAction move) {
                label = "MOVE(" + move.getTargetPosition().x() + "," + move.getTargetPosition().y() + ")";
            }
            utilityMap.put(label, eval.totalUtility());

            if (bestEval == null || eval.totalUtility() > bestEval.totalUtility()) {
                bestEval = eval;
                bestAction = candidate;
            }
        }

        if (bestAction == null) {
            bestAction = new RestAction(agent);
            bestEval = evaluator.evaluate(bestAction, agent, perception, context);
            utilityMap.put("REST", bestEval.totalUtility());
        }

        String obsSummary = String.format("foodOnCell=%.1f, visibleCells=%d, nearbyAgents=%d",
                perception.getCurrentCell().getResourceAmount(),
                perception.getVisibleCells().size(),
                perception.getNearbyAgents().size());

        agent.setLastDecision(new DecisionRecord(
                context.getClock().getCurrentTick(),
                agent.getId(),
                bestAction.getName(),
                bestEval.totalUtility(),
                utilityMap,
                obsSummary,
                bestEval.reason()
        ));

        return bestAction;
    }

    private List<Action> generateValidCandidates(Agent agent, Perception perception, SimulationContext context) {
        List<Action> valid = new ArrayList<>();

        ReproduceAction reproduce = new ReproduceAction(agent);
        if (reproduce.isValid(context)) {
            valid.add(reproduce);
        }

        ConsumeResourceAction consume = new ConsumeResourceAction(agent);
        if (consume.isValid(context)) {
            valid.add(consume);
        }

        // Best visible food step
        Cell bestFood = perception.getVisibleCells().stream()
                .filter(c -> c.getResourceAmount() > 0.5 && c.getTerrain() != Terrain.HAZARD)
                .max((a, b) -> Double.compare(
                        a.getResourceAmount() / Math.max(1.0, agent.getPosition().manhattanDistanceTo(a.getPosition())),
                        b.getResourceAmount() / Math.max(1.0, agent.getPosition().manhattanDistanceTo(b.getPosition()))
                ))
                .orElse(null);

        if (bestFood != null && !bestFood.getPosition().equals(agent.getPosition())) {
            int dx = Integer.compare(bestFood.getPosition().x(), agent.getPosition().x());
            int dy = Integer.compare(bestFood.getPosition().y(), agent.getPosition().y());
            MoveAction toFood = new MoveAction(agent, agent.getPosition().translate(dx, dy));
            if (toFood.isValid(context)) {
                valid.add(toFood);
            }
        }

        // Exploratory step
        DeterministicRandom random = context.getRandom();
        List<Position> passableDirs = new ArrayList<>();
        for (int[] dir : DIRECTIONS) {
            Position p = agent.getPosition().translate(dir[0], dir[1]);
            if (context.getWorld().isPassable(p)) {
                passableDirs.add(p);
            }
        }
        if (!passableDirs.isEmpty()) {
            Position pick = passableDirs.get(random.nextInt(passableDirs.size()));
            MoveAction explore = new MoveAction(agent, pick);
            if (explore.isValid(context)) {
                valid.add(explore);
            }
        }

        RestAction rest = new RestAction(agent);
        if (rest.isValid(context)) {
            valid.add(rest);
        }

        return valid;
    }
}
