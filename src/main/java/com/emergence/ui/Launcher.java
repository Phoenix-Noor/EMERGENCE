package com.emergence.ui;

import javafx.application.Application;

/**
 * Non-Application launcher class for running Emergence in unnamed module environments.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(EmergenceApp.class, args);
    }
}
