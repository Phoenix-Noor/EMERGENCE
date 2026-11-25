package com.emergence.ui;

import com.emergence.agent.Agent;
import com.emergence.agent.AgentTraits;
import com.emergence.core.Simulation;
import com.emergence.core.SimulationConfig;
import com.emergence.core.SimulationEvent;
import com.emergence.core.SimulationState;
import com.emergence.decision.DecisionEngine;
import com.emergence.decision.DecisionRecord;
import com.emergence.world.Position;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Main JavaFX desktop application entry point for EMERGENCE.
 */
public class EmergenceApp extends Application {

    private Simulation simulation;
    private WorldRenderer renderer;
    private Canvas worldCanvas;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> tickTask;
    private final Object simLock = new Object();

    private Long selectedAgentId;
    private double lastDragX;
    private double lastDragY;

    // Status labels
    private Label tickLabel;
    private Label popLabel;
    private Label resourceLabel;
    private Label birthsDeathsLabel;
    private Label gameMetricsLabel;
    private Label swarmMetricsLabel;
    private Label stateLabel;
    private Label inspectorLabel;
    private ListView<String> eventListView;

    @Override
    public void start(Stage stage) {
        SimulationConfig config = new SimulationConfig();
        simulation = new Simulation(config, new DecisionEngine());
        renderer = new WorldRenderer();
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "emergence-sim-thread");
            t.setDaemon(true);
            return t;
        });

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f141b; -fx-font-family: 'Segoe UI', sans-serif;");

        root.setTop(buildToolbar());
        root.setCenter(buildWorldPane());
        root.setRight(buildInspectorPane());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1280, 820);
        stage.setTitle("EMERGENCE — Simple Rules. Complex Worlds.");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> shutdown());
        stage.show();

        refreshUI();
    }

    private HBox buildToolbar() {
        Button startBtn = styledButton("Start", "#1f6feb");
        Button pauseBtn = styledButton("Pause", "#9e6a03");
        Button stepBtn = styledButton("Step", "#2ea043");
        Button resetBtn = styledButton("Reset", "#da3633");

        startBtn.setOnAction(e -> startLoop());
        pauseBtn.setOnAction(e -> pauseLoop());
        stepBtn.setOnAction(e -> {
            pauseLoop();
            synchronized (simLock) {
                simulation.stepTick();
            }
            refreshUI();
        });
        resetBtn.setOnAction(e -> {
            pauseLoop();
            synchronized (simLock) {
                simulation.reset();
                selectedAgentId = null;
            }
            renderer.resetView();
            refreshUI();
        });

        Label speedTitle = styledLabel("Speed (TPS):");
        Slider speedSlider = new Slider(1, 30, simulation.getConfig().getTicksPerSecond());
        speedSlider.setPrefWidth(130);
        Label speedValue = styledLabel(String.valueOf((int) speedSlider.getValue()));
        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int tps = Math.max(1, newV.intValue());
            speedValue.setText(String.valueOf(tps));
            synchronized (simLock) {
                simulation.getConfig().setTicksPerSecond(tps);
            }
            if (tickTask != null && !tickTask.isCancelled()) {
                startLoop();
            }
        });

        Label seedTitle = styledLabel("Seed:");
        TextField seedField = new TextField(String.valueOf(simulation.getConfig().getRandomSeed()));
        seedField.setPrefWidth(80);
        seedField.setStyle("-fx-background-color: #161b22; -fx-text-fill: #e6edf3; -fx-border-color: #30363d;");
        Button applySeedBtn = styledButton("Apply", "#30363d");
        applySeedBtn.setOnAction(e -> {
            try {
                long newSeed = Long.parseLong(seedField.getText().trim());
                pauseLoop();
                synchronized (simLock) {
                    simulation.getConfig().setRandomSeed(newSeed);
                    simulation.reset();
                    selectedAgentId = null;
                }
                refreshUI();
            } catch (NumberFormatException ignored) {
                seedField.setText(String.valueOf(simulation.getConfig().getRandomSeed()));
            }
        });

        CheckBox terrainLayer = layerCheck("Terrain", true, v -> { renderer.setShowTerrain(v); refreshUI(); });
        CheckBox resourceLayer = layerCheck("Resources", true, v -> { renderer.setShowResources(v); refreshUI(); });
        CheckBox groupLayer = layerCheck("Groups", true, v -> { renderer.setShowGroups(v); refreshUI(); });
        CheckBox agentLayer = layerCheck("Agents", true, v -> { renderer.setShowAgents(v); refreshUI(); });

        HBox bar = new HBox(10,
                startBtn, pauseBtn, stepBtn, resetBtn,
                new Separator(),
                speedTitle, speedSlider, speedValue,
                new Separator(),
                seedTitle, seedField, applySeedBtn,
                new Separator(),
                terrainLayer, resourceLayer, groupLayer, agentLayer
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");
        return bar;
    }

    private StackPane buildWorldPane() {
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: #0d1117;");
        worldCanvas = new Canvas(800, 700);

        container.widthProperty().addListener((obs, o, n) -> {
            worldCanvas.setWidth(Math.max(100, n.doubleValue()));
            renderCanvas();
        });
        container.heightProperty().addListener((obs, o, n) -> {
            worldCanvas.setHeight(Math.max(100, n.doubleValue()));
            renderCanvas();
        });

        worldCanvas.setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.15 : 0.87;
            renderer.adjustZoom(factor, e.getX(), e.getY());
            renderCanvas();
        });

        worldCanvas.setOnMousePressed(e -> {
            lastDragX = e.getX();
            lastDragY = e.getY();
        });

        worldCanvas.setOnMouseDragged(e -> {
            renderer.pan(e.getX() - lastDragX, e.getY() - lastDragY);
            lastDragX = e.getX();
            lastDragY = e.getY();
            renderCanvas();
        });

        worldCanvas.setOnMouseClicked(e -> {
            if (e.isStillSincePress()) {
                synchronized (simLock) {
                    Position gridPos = renderer.screenToGrid(e.getX(), e.getY(), worldCanvas, simulation.getContext().getWorld());
                    if (gridPos != null) {
                        selectedAgentId = simulation.getContext().getAgents().stream()
                                .filter(Agent::isAlive)
                                .filter(a -> a.getPosition().equals(gridPos))
                                .min(Comparator.comparingLong(Agent::getId))
                                .map(Agent::getId)
                                .orElse(null);
                    }
                }
                refreshUI();
            }
        });

        container.getChildren().add(worldCanvas);
        return container;
    }

    private VBox buildInspectorPane() {
        Label title = new Label("AGENT INSPECTOR");
        title.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold; -fx-font-size: 12px;");

        inspectorLabel = new Label("Click an agent on the grid to inspect its state and traits.");
        inspectorLabel.setWrapText(true);
        inspectorLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 12px; -fx-font-family: 'Consolas', monospace;");

        Label eventsTitle = new Label("RECENT EVENTS");
        eventsTitle.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold; -fx-font-size: 12px;");

        eventListView = new ListView<>();
        eventListView.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #c9d1d9; -fx-font-size: 11px;");
        VBox.setVgrow(eventListView, Priority.ALWAYS);

        VBox panel = new VBox(10, title, inspectorLabel, new Separator(), eventsTitle, eventListView);
        panel.setPrefWidth(310);
        panel.setPadding(new Insets(12));
        panel.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 0 1;");
        return panel;
    }

    private HBox buildStatusBar() {
        tickLabel = styledLabel("Tick: 0");
        popLabel = styledLabel("Population: 0");
        resourceLabel = styledLabel("Resources: 0.0");
        birthsDeathsLabel = styledLabel("Births: 0 | Deaths: 0");
        gameMetricsLabel = styledLabel("Coop: 0.0% | Avg Payoff: 0.00");
        swarmMetricsLabel = styledLabel("Groups: 0 | Avg Size: 0.0");
        stateLabel = styledLabel("State: IDLE");

        HBox status = new HBox(20, stateLabel, tickLabel, popLabel, resourceLabel, birthsDeathsLabel, gameMetricsLabel, swarmMetricsLabel);
        status.setPadding(new Insets(8, 14, 8, 14));
        status.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 1 0 0 0;");
        return status;
    }

    private void startLoop() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel(false);
        }
        synchronized (simLock) {
            simulation.start();
        }
        long periodMs = Math.max(10L, 1000L / simulation.getConfig().getTicksPerSecond());
        tickTask = executor.scheduleAtFixedRate(() -> {
            synchronized (simLock) {
                if (simulation.getState() == SimulationState.RUNNING) {
                    simulation.stepTick();
                }
            }
            Platform.runLater(this::refreshUI);
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    private void pauseLoop() {
        if (tickTask != null) {
            tickTask.cancel(false);
        }
        synchronized (simLock) {
            simulation.pause();
        }
        refreshUI();
    }

    private void refreshUI() {
        renderCanvas();
        synchronized (simLock) {
            var ctx = simulation.getContext();
            stateLabel.setText("State: " + simulation.getState());
            tickLabel.setText("Tick: " + ctx.getClock().getCurrentTick());
            popLabel.setText("Population: " + ctx.getLivingPopulationCount());
            resourceLabel.setText(String.format(Locale.US, "Resources: %.1f", ctx.getWorld().getTotalResources()));
            birthsDeathsLabel.setText("Births: " + ctx.getTotalBirths() + " | Deaths: " + ctx.getTotalDeaths());
            var gameEngine = simulation.getGameTheoryEngine();
            gameMetricsLabel.setText(String.format(Locale.US, "Coop: %.1f%% | Avg Payoff: %.2f",
                    gameEngine.getCooperationFrequency() * 100.0,
                    gameEngine.getAveragePayoff()));
            var gm = simulation.getGroupManager();
            swarmMetricsLabel.setText(String.format(Locale.US, "Groups: %d | Avg Size: %.1f",
                    gm.getGroupCount(), gm.getAverageGroupSize()));

            if (selectedAgentId != null) {
                Agent selected = ctx.getAgents().stream()
                        .filter(a -> a.getId() == selectedAgentId)
                        .findFirst()
                        .orElse(null);
                if (selected != null) {
                    AgentTraits t = selected.getTraits();
                    DecisionRecord dr = selected.getLastDecision();
                    String decisionInfo = dr == null ? "None" : String.format(Locale.US,
                            "%s (U=%.2f)\nReason: %s",
                            dr.selectedAction(), dr.selectedUtility(), dr.reason());
                    inspectorLabel.setText(String.format(Locale.US,
                            "ID: #%d (Gen %d) | Group: %s\nPos: (%d, %d)\nState: %s\nStrategy: %s\nAction: %s\nAge: %d\nEnergy: %.1f / %.1f\nHealth: %.1f / %.1f\n"
                                    + "--- Traits ---\nAggression:  %.2f\nCooperation: %.2f\nSociability: %.2f\nExploration: %.2f\nRisk Tol.:   %.2f\nTrust Prop.: %.2f\n"
                                    + "--- Decision ---\n%s",
                            selected.getId(), selected.getGeneration(),
                            selected.getGroupId() > 0 ? "#" + selected.getGroupId() : "None",
                            selected.getPosition().x(), selected.getPosition().y(),
                            selected.getState(), selected.getStrategyType().getDisplayName(),
                            selected.getLastActionName(),
                            selected.getAge(), selected.getEnergy(), selected.getMaxEnergy(),
                            selected.getHealth(), selected.getMaxHealth(),
                            t.aggression(), t.cooperation(), t.sociability(),
                            t.exploration(), t.riskTolerance(), t.trustPropensity(),
                            decisionInfo
                    ));
                } else {
                    inspectorLabel.setText("Selected agent #" + selectedAgentId + " is no longer active.");
                }
            } else {
                inspectorLabel.setText("Click an agent on the grid to inspect its state and traits.");
            }

            List<SimulationEvent> events = ctx.getEventBus().getRecentEvents();
            int startIdx = Math.max(0, events.size() - 25);
            eventListView.getItems().setAll(
                    events.subList(startIdx, events.size()).stream()
                            .map(ev -> String.format("[T%d] %s: %s", ev.tick(), ev.category(), ev.message()))
                            .toList()
            );
        }
    }

    private void renderCanvas() {
        synchronized (simLock) {
            renderer.render(worldCanvas, simulation.getContext(), simulation.getGroupManager(), selectedAgentId);
        }
    }

    private void shutdown() {
        if (tickTask != null) {
            tickTask.cancel(true);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        synchronized (simLock) {
            simulation.stop();
        }
    }

    private static Button styledButton(String text, String bgHex) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bgHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        return btn;
    }

    private static Label styledLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
        return lbl;
    }

    private static CheckBox layerCheck(String text, boolean initial, java.util.function.Consumer<Boolean> onToggle) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(initial);
        cb.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
        cb.selectedProperty().addListener((obs, o, n) -> onToggle.accept(n));
        return cb;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
