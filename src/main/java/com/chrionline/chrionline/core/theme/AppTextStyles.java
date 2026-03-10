package com.chrionline.chrionline.core.theme;


import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class AppTextStyles {


    public static final String FONT_FAMILY = "System";
    public static final String FONT_FAMILY_BRAND = "System";

    // ===== DISPLAY / HEADER STYLES =====
    public static Text createDisplayLarge(String text) {
        Text display = new Text(text);
        display.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 48));
        display.setStyle("-fx-letter-spacing: -0.5px;");
        return display;
    }

    public static Text createDisplayMedium(String text) {
        Text display = new Text(text);
        display.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 40));
        display.setStyle("-fx-letter-spacing: -0.5px;");
        return display;
    }

    public static Text createDisplaySmall(String text) {
        Text display = new Text(text);
        display.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 32));
        display.setStyle("-fx-letter-spacing: -0.5px;");
        return display;
    }

    // ===== HEADLINE STYLES =====
    public static Text createHeadlineLarge(String text) {
        Text headline = new Text(text);
        headline.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 32));
        headline.setStyle("-fx-letter-spacing: -0.4px;");
        return headline;
    }

    public static Text createHeadlineMedium(String text) {
        Text headline = new Text(text);
        headline.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 28));
        headline.setStyle("-fx-letter-spacing: -0.3px;");
        return headline;
    }

    public static Text createHeadlineSmall(String text) {
        Text headline = createPriceLarge(text);
        headline.setStyle("-fx-letter-spacing: -0.2px;");
        return headline;
    }

    // ===== TITLE STYLES =====
    public static Text createTitleLarge(String text) {
        Text title = new Text(text);
        title.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 20));
        title.setStyle("-fx-letter-spacing: -0.2px;");
        return title;
    }

    public static Text createTitleMedium(String text) {
        Text title = new Text(text);
        title.setFont(Font.font(FONT_FAMILY, FontWeight.MEDIUM, 18));
        title.setStyle("-fx-letter-spacing: -0.1px;");
        return title;
    }

    public static Text createTitleSmall(String text) {
        Text title = new Text(text);
        title.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 16));
        return title;
    }

    // ===== BODY STYLES =====
    public static Text createBodyLarge(String text) {
        Text body = new Text(text);
        body.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, 16));
        body.setStyle("-fx-letter-spacing: 0.2px;");
        return body;
    }

    public static Text createBodyMedium(String text) {
        Text body = new Text(text);
        body.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, 14));
        body.setStyle("-fx-letter-spacing: 0.2px;");
        return body;
    }

    public static Text createBodySmall(String text) {
        Text body = new Text(text);
        body.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, 12));
        body.setStyle("-fx-letter-spacing: 0.3px;");
        return body;
    }

    // ===== LABEL STYLES =====
    public static Text createLabelLarge(String text) {
        Text label = createPriceSmall(text);
        label.setStyle("-fx-letter-spacing: 0.3px;");
        return label;
    }

    public static Text createLabelMedium(String text) {
        Text label = new Text(text);
        label.setFont(Font.font(FONT_FAMILY, FontWeight.MEDIUM, 12));
        label.setStyle("-fx-letter-spacing: 0.3px;");
        return label;
    }

    public static Text createLabelSmall(String text) {
        Text label = new Text(text);
        label.setFont(Font.font(FONT_FAMILY, FontWeight.MEDIUM, 10));
        label.setStyle("-fx-letter-spacing: 0.4px;");
        return label;
    }

    // ===== AIRBNB SPECIFIC STYLES =====
    public static Text createPriceLarge(String text) {
        Text price = new Text(text);
        price.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 24));
        return price;
    }

    public static Text createPriceMedium(String text) {
        Text price = new Text(text);
        price.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 18));
        return price;
    }

    public static Text createPriceSmall(String text) {
        Text price = new Text(text);
        price.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 14));
        return price;
    }

    public static Text createRating(String text) {
        Text rating = new Text(text);
        rating.setFont(Font.font(FONT_FAMILY, FontWeight.MEDIUM, 14));
        return rating;
    }

    public static Text createSuperhost(String text) {
        Text superhost = new Text(text);
        superhost.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 12));
        superhost.setStyle("-fx-letter-spacing: 0.5px;");
        return superhost;
    }

    // ===== BUTTON STYLES =====
    public static Text createButtonLarge(String text) {
        Text button = createTitleSmall(text);
        button.setStyle("-fx-letter-spacing: 0.5px;");
        return button;
    }

    public static Text createButtonMedium(String text) {
        Text button = createPriceSmall(text);
        button.setStyle("-fx-letter-spacing: 0.5px;");
        return button;
    }

    public static Text createButtonSmall(String text) {
        return createSuperhost(text);
    }
}
