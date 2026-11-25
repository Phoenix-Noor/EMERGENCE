package com.emergence.ui;

import com.emergence.agent.Agent;
import com.emergence.core.SimulationContext;
import com.emergence.swarm.AgentGroup;
import com.emergence.swarm.GroupManager;
import com.emergence.world.Cell;
import com.emergence.world.Position;
import com.emergence.world.Terrain;
import com.emergence.world.World;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * High-performance JavaFX Canvas renderer for the 2D simulation world, resources, and agents.
 */
public final class WorldRenderer {

    private static final Color BG_COLOR = Color.web("#0d1117");
    private static final Color GRASS_COLOR = Color.web("#1b2e22");
    private static final Color FOREST_COLOR = Color.web("#13261b");
    private static final Color WATER_COLOR = Color.web("#142b47");
    private static final Color HAZARD_COLOR = Color.web("#4a1b1f");
    private static final Color SELECTED_RING = Color.web("#ffd166");

    private double zoom = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;

    private boolean showTerrain = true;
    private boolean showResources = true;
    private boolean showGroups = true;
    private boolean showAgents = true;

    public void adjustZoom(double factor, double pivotX, double pivotY) {
        double oldZoom = zoom;
        zoom = Math.max(0.4, Math.min(8.0, zoom * factor));
        double scaleChange = zoom / oldZoom;
        offsetX = pivotX - (pivotX - offsetX) * scaleChange;
        offsetY = pivotY - (pivotY - offsetY) * scaleChange;
    }

    public void pan(double dx, double dy) {
        offsetX += dx;
        offsetY += dy;
    }

    public void resetView() {
        zoom = 1.0;
        offsetX = 0.0;
        offsetY = 0.0;
    }

    public void setShowTerrain(boolean showTerrain) { this.showTerrain = showTerrain; }
    public void setShowResources(boolean showResources) { this.showResources = showResources; }
    public void setShowGroups(boolean showGroups) { this.showGroups = showGroups; }
    public void setShowAgents(boolean showAgents) { this.showAgents = showAgents; }

    public Position screenToGrid(double screenX, double screenY, Canvas canvas, World world) {
        double cellSize = computeBaseCellSize(canvas, world) * zoom;
        if (cellSize <= 0) {
            return null;
        }
        int gx = (int) Math.floor((screenX - offsetX) / cellSize);
        int gy = (int) Math.floor((screenY - offsetY) / cellSize);
        if (world.isInBounds(gx, gy)) {
            return new Position(gx, gy);
        }
        return null;
    }

    public void render(Canvas canvas, SimulationContext context, GroupManager groupManager, Long selectedAgentId) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, w, h);

        if (context == null) {
            return;
        }

        World world = context.getWorld();
        double cellSize = computeBaseCellSize(canvas, world) * zoom;

        gc.save();
        gc.translate(offsetX, offsetY);

        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                Cell cell = world.getCell(x, y);
                double px = x * cellSize;
                double py = y * cellSize;

                if (showTerrain) {
                    gc.setFill(terrainColor(cell.getTerrain()));
                    gc.fillRect(px, py, cellSize + 0.4, cellSize + 0.4);
                }

                if (showResources && cell.getResourceAmount() > 0.01) {
                    double ratio = Math.min(1.0, cell.getResourceAmount() / cell.getMaxResource());
                    gc.setFill(Color.color(0.25, 0.85, 0.42, 0.25 + 0.65 * ratio));
                    double pad = cellSize * 0.18;
                    gc.fillOval(px + pad, py + pad, Math.max(1.5, cellSize - 2 * pad), Math.max(1.5, cellSize - 2 * pad));
                }
            }
        }

        if (showGroups && groupManager != null) {
            for (AgentGroup group : groupManager.getGroups()) {
                Color groupColor = Color.hsb((group.getId() * 67.0) % 360.0, 0.75, 0.92, 0.45);
                double cx = (group.getCentroid().x() + 0.5) * cellSize;
                double cy = (group.getCentroid().y() + 0.5) * cellSize;
                gc.setStroke(groupColor);
                gc.setLineWidth(Math.max(0.8, cellSize * 0.12));
                for (Agent agent : context.getAgents()) {
                    if (agent.isAlive() && agent.getGroupId() == group.getId()) {
                        double ax = (agent.getPosition().x() + 0.5) * cellSize;
                        double ay = (agent.getPosition().y() + 0.5) * cellSize;
                        gc.strokeLine(ax, ay, cx, cy);
                    }
                }
                gc.setFill(groupColor);
                double r = Math.max(2.0, cellSize * 0.35);
                gc.fillOval(cx - r, cy - r, r * 2, r * 2);
            }
        }

        if (showAgents) {
            List<Agent> agents = context.getAgents();
            for (Agent agent : agents) {
                if (!agent.isAlive()) {
                    continue;
                }
                double px = agent.getPosition().x() * cellSize;
                double py = agent.getPosition().y() * cellSize;
                double energyRatio = Math.max(0.2, agent.getEnergy() / agent.getMaxEnergy());

                gc.setFill(Color.color(0.22 + 0.55 * (1.0 - energyRatio), 0.72 * energyRatio + 0.2, 0.96));
                double pad = cellSize * 0.14;
                double size = Math.max(2.5, cellSize - 2 * pad);
                gc.fillOval(px + pad, py + pad, size, size);

                if (selectedAgentId != null && selectedAgentId == agent.getId()) {
                    gc.setStroke(SELECTED_RING);
                    gc.setLineWidth(Math.max(1.5, cellSize * 0.2));
                    gc.strokeOval(px + pad * 0.4, py + pad * 0.4, cellSize - pad * 0.8, cellSize - pad * 0.8);
                }
            }
        }

        gc.restore();
    }

    private double computeBaseCellSize(Canvas canvas, World world) {
        double cw = canvas.getWidth() / world.getWidth();
        double ch = canvas.getHeight() / world.getHeight();
        return Math.max(1.0, Math.min(cw, ch));
    }

    private Color terrainColor(Terrain terrain) {
        return switch (terrain) {
            case GRASSLAND -> GRASS_COLOR;
            case FOREST -> FOREST_COLOR;
            case WATER -> WATER_COLOR;
            case HAZARD -> HAZARD_COLOR;
        };
    }
}
