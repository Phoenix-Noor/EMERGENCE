package com.emergence;

import com.emergence.ui.EmergenceApp;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class Phase1UiVerificationTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX toolkit failed to start");
    }

    @AfterAll
    static void exitJavaFx() {
        Platform.setImplicitExit(true);
    }

    @Test
    void verifyAllPhase1UiControlsAndOutputs() throws Exception {
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        CountDownLatch startLatch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Stage stage = new Stage();
            EmergenceApp app = new EmergenceApp();
            app.start(stage);
            stageRef.set(stage);
            startLatch.countDown();
        });
        assertTrue(startLatch.await(10, TimeUnit.SECONDS), "EmergenceApp failed to start");

        Stage stage = stageRef.get();
        Scene scene = stage.getScene();
        assertNotNull(scene);

        // 1 & 2. Verify window and world Canvas are visible
        Canvas canvas = findFirst(scene.getRoot(), Canvas.class);
        assertNotNull(canvas, "World Canvas should exist");
        assertTrue(canvas.getWidth() > 0 && canvas.getHeight() > 0, "World Canvas must have positive size");

        Button startBtn = findButton(scene.getRoot(), "Start");
        Button pauseBtn = findButton(scene.getRoot(), "Pause");
        Button stepBtn = findButton(scene.getRoot(), "Step");
        Button resetBtn = findButton(scene.getRoot(), "Reset");
        assertNotNull(startBtn);
        assertNotNull(pauseBtn);
        assertNotNull(stepBtn);
        assertNotNull(resetBtn);

        Label tickLabel = findLabelStartingWith(scene.getRoot(), "Tick:");
        Label resourceLabel = findLabelStartingWith(scene.getRoot(), "Resources:");
        @SuppressWarnings("unchecked")
        ListView<String> eventLog = findFirst(scene.getRoot(), ListView.class);
        assertNotNull(tickLabel);
        assertNotNull(resourceLabel);
        assertNotNull(eventLog);
        assertFalse(eventLog.getItems().isEmpty(), "Event log should have initial lifecycle event");

        String initialResources = resourceLabel.getText();

        // 3. Test Step
        runOnFxAndWait(() -> stepBtn.fire());
        assertEquals("Tick: 1", tickLabel.getText());

        // 4. Test Start and Pause (agents move, consume resources, events update)
        runOnFxAndWait(() -> startBtn.fire());
        Thread.sleep(1500);
        runOnFxAndWait(() -> pauseBtn.fire());

        int tickAfterRun = Integer.parseInt(tickLabel.getText().replace("Tick:", "").trim());
        assertTrue(tickAfterRun > 1, "Start should advance ticks automatically, got " + tickAfterRun);
        assertNotEquals(initialResources, resourceLabel.getText(), "Resources should change as agents consume/regenerate");

        // 5. Verify Agent Inspector displays actual agent information when clicked
        // Scan canvas grid until we hit an agent (or verify inspector updates)
        AtomicReference<Label> inspectorRef = new AtomicReference<>();
        runOnFxAndWait(() -> {
            Label insp = findLabelContaining(scene.getRoot(), "Click an agent");
            if (insp == null) {
                insp = findLabelContaining(scene.getRoot(), "ID: #");
            }
            inspectorRef.set(insp);
            double w = canvas.getWidth();
            double h = canvas.getHeight();
            double cellSize = Math.min(w / 100.0, h / 100.0);
            for (int gx = 0; gx < 100 && !insp.getText().contains("ID: #"); gx++) {
                for (int gy = 0; gy < 100 && !insp.getText().contains("ID: #"); gy++) {
                    double sx = (gx + 0.5) * cellSize;
                    double sy = (gy + 0.5) * cellSize;
                    MouseEvent click = new MouseEvent(
                            MouseEvent.MOUSE_CLICKED, sx, sy, sx, sy,
                            MouseButton.PRIMARY, 1, false, false, false, false,
                            true, false, false, false, false, true, null
                    );
                    canvas.fireEvent(click);
                }
            }
        });
        assertNotNull(inspectorRef.get());
        String inspectorText = inspectorRef.get().getText();
        assertTrue(inspectorText.contains("ID: #"), "Inspector should display selected agent ID, got: " + inspectorText);
        assertTrue(inspectorText.contains("Energy:"), "Inspector should display agent energy");
        assertTrue(inspectorText.contains("--- Traits ---"), "Inspector should display agent traits");

        // 6. Test Reset
        runOnFxAndWait(() -> resetBtn.fire());
        assertEquals("Tick: 0", tickLabel.getText());
        assertTrue(stage.isShowing());

        runOnFxAndWait(stage::close);
    }

    private static void runOnFxAndWait(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX action timed out");
    }

    private static <T extends Node> T findFirst(Parent root, Class<T> type) {
        List<Node> all = collectAll(root);
        for (Node n : all) {
            if (type.isInstance(n)) {
                return type.cast(n);
            }
        }
        return null;
    }

    private static Button findButton(Parent root, String text) {
        for (Node n : collectAll(root)) {
            if (n instanceof Button b && text.equals(b.getText())) {
                return b;
            }
        }
        return null;
    }

    private static Label findLabelStartingWith(Parent root, String prefix) {
        for (Node n : collectAll(root)) {
            if (n instanceof Label l && l.getText() != null && l.getText().startsWith(prefix)) {
                return l;
            }
        }
        return null;
    }

    private static Label findLabelContaining(Parent root, String sub) {
        for (Node n : collectAll(root)) {
            if (n instanceof Label l && l.getText() != null && l.getText().contains(sub)) {
                return l;
            }
        }
        return null;
    }

    private static List<Node> collectAll(Parent root) {
        List<Node> list = new ArrayList<>();
        for (Node child : root.getChildrenUnmodifiable()) {
            list.add(child);
            if (child instanceof Parent p) {
                list.addAll(collectAll(p));
            }
        }
        return list;
    }
}
