package com.emergence.world;

import com.emergence.core.DeterministicRandom;
import com.emergence.core.SimulationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 2D grid world managing terrain cells and resource regeneration.
 */
public final class World {

    private final int width;
    private final int height;
    private final boolean wrapEdges;
    private final Cell[][] grid;

    public World(SimulationConfig config, DeterministicRandom random) {
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(random, "random must not be null");
        config.validate();

        this.width = config.getWorldWidth();
        this.height = config.getWorldHeight();
        this.wrapEdges = config.isWrapEdges();
        this.grid = new Cell[width][height];

        initializeGrid(config, random);
    }

    private void initializeGrid(SimulationConfig config, DeterministicRandom random) {
        double waterThreshold = config.getWaterFraction();
        double hazardThreshold = waterThreshold + config.getHazardFraction();
        double forestThreshold = hazardThreshold + config.getForestFraction();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double roll = random.nextDouble();
                Terrain terrain;
                if (roll < waterThreshold) {
                    terrain = Terrain.WATER;
                } else if (roll < hazardThreshold) {
                    terrain = Terrain.HAZARD;
                } else if (roll < forestThreshold) {
                    terrain = Terrain.FOREST;
                } else {
                    terrain = Terrain.GRASSLAND;
                }

                double initialResource = 0.0;
                double regenRate = 0.0;
                if (terrain == Terrain.GRASSLAND || terrain == Terrain.FOREST) {
                    if (random.nextDouble() < config.getInitialFoodCellDensity()) {
                        initialResource = config.getMaxCellResource() * (0.4 + 0.6 * random.nextDouble());
                        regenRate = config.getResourceRegenerationRate();
                    } else if (random.nextDouble() < 0.25) {
                        regenRate = config.getResourceRegenerationRate() * 0.5;
                    }
                }

                grid[x][y] = new Cell(
                        new Position(x, y),
                        terrain,
                        initialResource,
                        config.getMaxCellResource(),
                        regenRate
                );
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isWrapEdges() {
        return wrapEdges;
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isInBounds(Position position) {
        return position != null && isInBounds(position.x(), position.y());
    }

    public Position normalizePosition(Position position) {
        if (wrapEdges) {
            int nx = ((position.x() % width) + width) % width;
            int ny = ((position.y() % height) + height) % height;
            return new Position(nx, ny);
        }
        int cx = Math.max(0, Math.min(width - 1, position.x()));
        int cy = Math.max(0, Math.min(height - 1, position.y()));
        return new Position(cx, cy);
    }

    public Cell getCell(int x, int y) {
        if (!isInBounds(x, y)) {
            throw new IndexOutOfBoundsException("Cell out of bounds: (" + x + ", " + y + ")");
        }
        return grid[x][y];
    }

    public Cell getCell(Position position) {
        Objects.requireNonNull(position, "position must not be null");
        return getCell(position.x(), position.y());
    }

    public boolean isPassable(Position position) {
        if (!wrapEdges && !isInBounds(position)) {
            return false;
        }
        Position norm = normalizePosition(position);
        return getCell(norm).getTerrain().isPassableByDefault();
    }

    public List<Cell> getCellsInRadius(Position center, int radius) {
        Objects.requireNonNull(center, "center must not be null");
        List<Cell> result = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int tx = center.x() + dx;
                int ty = center.y() + dy;
                if (wrapEdges) {
                    Position norm = normalizePosition(new Position(tx, ty));
                    result.add(getCell(norm));
                } else if (isInBounds(tx, ty)) {
                    result.add(grid[tx][ty]);
                }
            }
        }
        return result;
    }

    public void regenerateResources() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y].regenerate();
            }
        }
    }

    public double getTotalResources() {
        double total = 0.0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                total += grid[x][y].getResourceAmount();
            }
        }
        return total;
    }
}
