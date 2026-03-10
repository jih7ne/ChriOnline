package com.chrionline.chrionline.core.theme;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

public class ThemeContext {

    private final Scene scene;

    public ThemeContext(Scene scene) {
        this.scene = scene;
    }

    // Theme helpers
    public boolean isDarkMode() {
        return AppTheme.isDarkMode();
    }

    public void toggleTheme() {
        AppTheme.toggleTheme();
        applyThemeToScene();
    }

    private void applyThemeToScene() {

    }


}