package by.charity.client.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;

public class StyleManager {

    public static final String PINK_LIGHT    = "#FFF0F5";
    public static final String PINK_SOFT     = "#FFD6E7";
    public static final String PINK_MAIN     = "#F4A7C3";
    public static final String PINK_DARK     = "#E07FA0";
    public static final String PINK_DEEP     = "#C95980";
    public static final String WHITE         = "#FFFFFF";
    public static final String TEXT_DARK     = "#3D1A2E";
    public static final String TEXT_GRAY     = "#9E6E82";
    public static final String SUCCESS       = "#4A9E72";
    public static final String WARNING       = "#C49A2A";
    public static final String DANGER        = "#C05050";

    public static String buttonPrimary() {
        return String.format("""
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-padding: 8 20 8 20;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-cursor: hand;
                """, PINK_DEEP);
    }

    public static String buttonSecondary() {
        return String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-border-color: %s;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-padding: 8 20 8 20;
                -fx-font-size: 13px;
                -fx-cursor: hand;
                """, WHITE, PINK_DEEP, PINK_MAIN);
    }

    public static String buttonDanger() {
        return String.format("""
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-padding: 8 20 8 20;
                -fx-font-size: 13px;
                -fx-cursor: hand;
                """, DANGER);
    }

    public static String card() {
        return String.format("""
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-radius: 12;
                -fx-background-radius: 12;
                -fx-border-width: 1.5;
                -fx-padding: 16;
                -fx-effect: dropshadow(gaussian, rgba(233,109,163,0.13), 8, 0, 0, 3);
                """, WHITE, PINK_MAIN);
    }

    public static String tableStyle() {
        return String.format("""
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                """, WHITE, PINK_SOFT);
    }

    public static String textFieldStyle() {
        return String.format("""
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-padding: 7 10 7 10;
                -fx-font-size: 13px;
                -fx-text-fill: %s;
                """, WHITE, PINK_MAIN, TEXT_DARK);
    }

    public static String textFieldError() {
        return String.format("""
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-padding: 7 10 7 10;
                -fx-font-size: 13px;
                -fx-text-fill: %s;
                -fx-border-width: 2;
                """, WHITE, DANGER, TEXT_DARK);
    }

    public static String headerLabel() {
        return String.format("""
                -fx-font-size: 20px;
                -fx-font-weight: bold;
                -fx-text-fill: %s;
                """, TEXT_DARK);
    }

    public static String sidebarStyle() {
        return String.format("""
                -fx-background-color: linear-gradient(to bottom, %s, %s);
                -fx-padding: 0;
                """, PINK_MAIN, PINK_DARK);
    }

    public static String sidebarButton() {
        return String.format("""
                -fx-background-color: transparent;
                -fx-text-fill: %s;
                -fx-font-size: 14px;
                -fx-alignment: center-left;
                -fx-padding: 12 20 12 20;
                -fx-cursor: hand;
                -fx-border-radius: 0;
                -fx-background-radius: 0;
                -fx-min-width: 220;
                """, WHITE);
    }

    public static String sidebarButtonActive() {
        return """
                -fx-background-color: rgba(255,255,255,0.25);
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-alignment: center-left;
                -fx-padding: 12 20 12 20;
                -fx-cursor: hand;
                -fx-border-radius: 0;
                -fx-background-radius: 0;
                -fx-min-width: 220;
                -fx-font-weight: bold;
                """;
    }
}